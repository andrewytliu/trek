package cs244b.dstore.server;

import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.KeyValueStore;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DStoreInternalImpl implements DStoreInternal {
    private enum Status {
        NORMAL, VIEWCHANGE, RECOVERING
    }

    // Normal property
    private int replicaNumber;
    private int view;
    private Status status;
    private int op;
    private List<StoreAction> log;
    private int commit;
    // Prepare vote
    private Map<Integer, Semaphore> voteLock;
    private Map<Integer, Set<Integer>> voteSet;
    // View change vote
    private Map<Integer, Set<Integer>> viewSet;
    private Map<Integer, Set<Integer>> doViewSet;
    private int vcView;
    private int vcOp;
    private List<StoreAction> vcLog;
    private int vcCommit;
    // Storage
    private KeyValueStore storage;
    // Timer
    private Timer timer;
    private TimerTask task;
    // Log
    private static final Logger logger = Logger.getLogger("cs244b.VR");

    public DStoreInternalImpl(int number) {
        replicaNumber = number;
        view = 0;
        status = Status.NORMAL;
        op = -1;
        log = new ArrayList<>();
        commit = -1;
        voteLock = new HashMap<>();
        voteSet = new HashMap<>();
        viewSet = new HashMap<>();
        doViewSet = new HashMap<>();
        storage = new KeyValueStore();
        timer = new Timer();

        if (isPrimary()) {
            setPrimaryTimer();
        } else {
            setTimer();
        }
    }

    private void log(String l) {
        logger.log(Level.INFO, "R: " + replicaNumber + ", V: " + view + ", Op: " + op + ", Ci: " + commit + " => " + l);
    }

    public int getPrimary() {
        return view % DStoreSetting.SERVER.size();
    }

    public boolean isPrimary() {
        return getPrimary() == replicaNumber;
    }

    private void setPrimaryTimer() {
        task = new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
                    if (i == replicaNumber) continue;
                    RpcClient.internalStub(i).commit(view, commit);
                }
            }
        };
        timer.schedule(task, DStoreSetting.HEARTBEAT_SOFT);
    }

    private void clearPrimaryTimer() {
        task.cancel();
        setPrimaryTimer();
    }

    private void setTimer() {
        task = new TimerTask() {
            @Override
            public void run() {
                status = Status.VIEWCHANGE;
                view++;
                for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
                    RpcClient.internalStub(i).startViewChange(view, replicaNumber);
                }
            }
        };
        timer.schedule(task, DStoreSetting.HEARTBEAT_HARD);
    }

    private void clearTimer() {
        task.cancel();
        setTimer();
    }

    // TODO: set the timer for commit
    public int startTransactionPrimary(StoreAction action) {
        log("StartTransactionPrimary()");
        // TODO: need to redirect the request to primary
        if (!isPrimary()) return -1;

        // Putting the action in log and increase the op number
        log.add(action);
        op++;
        // Acquire all f semaphore
        Semaphore semaphore = new Semaphore(-DStoreSetting.getF() + 1);
        voteLock.put(op, semaphore);
        voteSet.put(op, new HashSet<Integer>());
        // Send prepare to all cohorts
        clearPrimaryTimer();
        for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
            if (i == replicaNumber) continue;
            RpcClient.internalStub(i).prepare(view, action, op, commit);
        }
        return op;
    }

    // TODO: timeout when not succeed
    public StoreResponse doCommitPrimary(int op) {
        log("doCommitPrimary(" + op + ")");
        voteLock.get(op).acquireUninterruptibly();
        voteLock.remove(op);
        return storage.apply(log.get(op));
    }

    private void doCommit(int commit) {
        log("doCommit(" + commit + ")");
        for (int i = this.commit + 1; i < commit; ++i) {
            storage.apply(log.get(i));
        }
        this.commit = commit;
    }

    @Override
    public void prepare(int view, StoreAction action, int op, int commit) {
        log("prepare(v: " + view + ", m, op: " + op + ", ci: " + commit + ")");
        // State need to be NORMAL
        if (status != Status.NORMAL) return;
        // Drop the message if the sender is behind
        if (this.view > view) return;
        // TODO: perform state transfer
        if (this.view < view) return;
        // Make sure we have all of the previous log
        if (this.op + 1 != op) return;
        // Commit previous log
        doCommit(commit);
        // Insert log
        log.add(action);
        // Increase op number
        this.op++;
        clearTimer();
        RpcClient.internalStub(getPrimary()).prepareOk(view, op, replicaNumber);
    }

    @Override
    public void prepareOk(int view, int op, int replica) {
        log("prepareOk(v: " + view + ", op: " + op + ", r: " + replica + ")");
        // State need to be NORMAL
        if (status != Status.NORMAL) return;
        // Drop the message if the sender is behind
        if (this.view > view) return;
        // TODO: perform state transfer
        if (this.view < view) return;
        // Check if there is duplicated prepareOk and then release semaphore
        if (voteLock.containsKey(op) && !voteSet.get(op).contains(replica)) {
            voteLock.get(op).release();
            voteSet.get(op).add(replica);
        }
    }

    @Override
    public void commit(int view, int commit) {
        log("commit(v: " + view + ", ci: " + commit + ")");
        // State need to be NORMAL
        if (status != Status.NORMAL) return;
        // Drop the message if the sender is behind
        if (this.view > view) return;
        // TODO: perform state transfer
        if (this.view < view) return;
        clearTimer();
        doCommit(commit);
    }

    @Override
    public void startViewChange(int view, int replica) {
        log("startViewChange(v: " + view + ", r: " + replica + ")");
        if (this.view >= view) return;
        // Change state
        status = Status.VIEWCHANGE;
        if (!viewSet.containsKey(view)) {
            viewSet.put(view, new HashSet<Integer>());
        }
        viewSet.get(view).add(replica);
        // Clear timer
        clearTimer();
        // Receiving f vote: reply
        if (viewSet.get(view).size() == DStoreSetting.getF()) {
            RpcClient.internalStub(getPrimary()).
                    doViewChange(view, log, this.view, op, commit, replicaNumber);
        }
    }

    @Override
    public void doViewChange(int view, List<StoreAction> log,
                             int oldView, int op, int commit, int replica) {
        log("doViewChange(v: " + view + ", log, ov: " + oldView + ", op: " + op + ", ci: " + commit + ", r: " + replica + ")");
        if (this.view >= view) return;
        // Change state
        status = Status.VIEWCHANGE;
        // Check view: something must be wrong here
        if (view % DStoreSetting.SERVER.size() != replicaNumber) return;
        if (!doViewSet.containsKey(view)) {
            doViewSet.put(view, new HashSet<Integer>());
            // TODO: should we group it with group number?
            vcView = this.view;
            vcOp = this.op;
            vcLog = this.log;
            vcCommit = this.commit;
        }
        doViewSet.get(view).add(replica);
        // Comparing largest log
        if (oldView > vcView || (oldView == vcView && op > vcOp)) {
            vcView = oldView;
            vcOp = op;
            vcLog = log;
            vcCommit = commit;
        }
        clearTimer();
        // Receiving f + 1 vote: view change
        if (doViewSet.get(view).size() == DStoreSetting.getF() + 1) {
            this.view = view;
            this.op = vcOp;
            this.commit = vcCommit;
            this.log = vcLog;
            status = Status.NORMAL;
            // Sending startView
            clearPrimaryTimer();
            for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
                if (i == replicaNumber) continue;
                RpcClient.internalStub(i).
                        startView(view, this.log, this.op, this.commit);
            }
        }
    }

    @Override
    public void startView(int view, List<StoreAction> log, int op, int commit) {
        log("startView(v: " + view + ", log, op: " + op + ", ci: " + commit + ")");
        if (this.view >= view) return;

        status = Status.NORMAL;
        this.log = log;
        this.op = op;
        this.commit = commit;
        // Handling uncommitted operation
        if (commit < log.size() - 1) {
            for (int i = commit + 1; i < log.size(); ++i) {
                RpcClient.internalStub(getPrimary()).prepareOk(view, i, replicaNumber);
            }
        }
        clearTimer();
    }

    @Override
    public void recovery(int replica, int nonce) {

    }

    @Override
    public void recoveryResponse(int view, int nonce, int[] log, int op, int commit, int replica) {

    }
}
