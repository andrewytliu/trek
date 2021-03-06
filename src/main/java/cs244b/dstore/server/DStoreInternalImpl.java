package cs244b.dstore.server;

import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.KeyValueStore;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DStoreInternalImpl implements DStoreInternal {
    private enum Status {
        NORMAL, VIEWCHANGE, RECOVERING
    }

    // Normal property
    private int replicaNumber;
    private int view;
    private int latestNormalView;
    private Status status;
    private int op;
    private List<StoreAction> log;
    private int commit;
    private ReentrantLock normalLock;
    private Condition normalCondition;
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
    // Recovery
    private int nonce;
    private Semaphore recoveryLock;
    private Set<Integer> recoverySet;
    private int recoveryCommit;
    // Storage
    private KeyValueStore storage;
    // Timer
    private Timer timer;
    private TimerTask task;
    private AtomicInteger timerId;
    // Log
    private static final Logger logger = Logger.getLogger("cs244b.VR");

    public DStoreInternalImpl(int number) {
        replicaNumber = number;
        view = 0;
        latestNormalView = 0;
        status = Status.NORMAL;
        normalLock = new ReentrantLock();
        normalCondition = normalLock.newCondition();
        op = -1;
        log = new ArrayList<>();
        commit = -1;
        voteLock = new HashMap<>();
        voteSet = new HashMap<>();
        viewSet = new HashMap<>();
        doViewSet = new HashMap<>();
        storage = new KeyValueStore();
        timer = new Timer();
        timerId = new AtomicInteger();
        recoveryLock = new Semaphore(-DStoreSetting.getF());
        recoverySet = new HashSet<>();

        initTimer();
    }

    private void log(String l) {
        String str = "R: " + replicaNumber + ", V: " + view + ", Op: " + op + ", Ci: " + commit + " => " + l;
        if (DStoreSetting.MONITOR == null) {
            logger.log(Level.INFO, str);
        } else {
            try {
                RpcClient.monitorStub().log(replicaNumber, str);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not log to monitor");
                logger.log(Level.INFO, str);
            }
        }
    }

    public void kill() {
        timerId.incrementAndGet();
        task.cancel();
        timer.cancel();
        timer = null;
        task = null;
    }

    public void recover() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                startRecovery();
                try {
                    Thread.sleep(DStoreSetting.RECOVERY_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    doRecovery();
                }
            }
        }).start();
    }

    public List<StoreAction> getCommitLog() {
        if (commit >= 0) {
            return log.subList(0, commit + 1);
        } else {
            return new ArrayList<>();
        }
    }

    public int getReplicaNumber() {
        return replicaNumber;
    }

    public int getPrimary() {
        return view % DStoreSetting.SERVER.size();
    }

    public boolean isPrimary() {
        return getPrimary() == replicaNumber;
    }

    public void initTimer() {
        if (isPrimary()) {
            setPrimaryTimer();
        } else {
            setTimer();
        }
    }

    private void setPrimaryTimer() {
        final int id = timerId.incrementAndGet();
        task = new TimerTask() {
            @Override
            public void run() {
                if (id != timerId.get()) return;
                log("Primary Timer Ticked");
                for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
                    if (i == replicaNumber) continue;
                    RpcClient.internalStub(i).commit(view, commit);
                }
                if (id != timerId.get()) return;
                setPrimaryTimer();
            }
        };
        if (timer != null) {
            timer.schedule(task, DStoreSetting.HEARTBEAT_SOFT);
        }
    }

    private void clearPrimaryTimer() {
        task.cancel();
        setPrimaryTimer();
    }

    private void setTimer() {
        final int id = timerId.incrementAndGet();
        task = new TimerTask() {
            @Override
            public void run() {
                if (id != timerId.get()) return;
                log("Normal Timer Ticked");
                status = Status.VIEWCHANGE;
                view++;
                for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
                    RpcClient.internalStub(i).startViewChange(view, replicaNumber);
                }
                if (id != timerId.get()) return;
                setTimer();
            }
        };
        if (timer != null) {
            timer.schedule(task, DStoreSetting.HEARTBEAT_HARD);
        }
    }

    private void clearTimer() {
        task.cancel();
        setTimer();
    }

    // TODO: set the timer for commit
    public int startTransactionPrimary(StoreAction action) {
        log("StartTransactionPrimary()");
        // TODO: need to redirect the request to primary
        normalLock.lock();
        while (status != Status.NORMAL) {
            normalCondition.awaitUninterruptibly();
        }
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
        normalLock.unlock();
        return op;
    }

    // TODO: timeout when not succeed
    public StoreResponse doCommitPrimary(int op) {
        log("doCommitPrimary(" + op + ")");
        // TODO: something must be wrong here
        if (commit + 1 != op) return null;
        voteLock.get(op).acquireUninterruptibly();
        voteLock.remove(op);
        commit = op;
        return storage.apply(log.get(op));
    }

    private void doCommit(int commit) {
        log("doCommit(" + commit + ")");
        for (int i = this.commit + 1; i <= commit; ++i) {
            storage.apply(log.get(i));
        }
        this.commit = commit;
    }

    public void startRecovery() {
        log("startRecovery()");
        // Killing timer
        if (task != null) task.cancel();
        timerId.incrementAndGet();
        // Setup
        nonce = (int) (Math.random() * 1e5);
        status = Status.RECOVERING;
        for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
            if (i == replicaNumber) continue;
            RpcClient.internalStub(i).recovery(replicaNumber, nonce);
        }
    }

    public void doRecovery() {
        log("doRecovery()");
        recoveryLock.acquireUninterruptibly();
        doCommit(recoveryCommit);
        status = Status.NORMAL;
        latestNormalView = view;
        normalLock.lock();
        normalCondition.signalAll();
        normalLock.unlock();

        initTimer();
    }

    @Override
    public void prepare(int view, StoreAction action, int op, int commit) {
        log("prepare(v: " + view + ", m, op: " + op + ", ci: " + commit + ")");
        // State need to be NORMAL
        if (status != Status.NORMAL) return;
        // Drop the message if the sender is behind
        if (this.view > view) return;
        // Recovering
        if (this.view < view || this.op + 1 < op) {
            recover();
            return;
        }
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
        // Recovering
        if (this.view < view || this.op + 1 < op) {
            recover();
            return;
        }
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
        // Recovering
        if (this.view < view || commit > op) {
            recover();
            return;
        }
        clearTimer();
        doCommit(commit);
    }

    @Override
    public void startViewChange(int view, int replica) {
        log("startViewChange(v: " + view + ", r: " + replica + ")");
        if (this.view > view ||
                (this.status != Status.VIEWCHANGE && this.view == view)) return;
        // Change state
        status = Status.VIEWCHANGE;
        if (!viewSet.containsKey(view)) {
            viewSet.put(view, new HashSet<Integer>());
        }
        viewSet.get(view).add(replica);
        // Clear timer
        clearTimer();
        // Send startViewChange if received higher number
        if (view > this.view) {
            this.view = view;
            viewSet.get(view).add(replicaNumber);
            for (int i = 0; i < DStoreSetting.SERVER.size(); ++i) {
                if (i == replicaNumber) continue;
                RpcClient.internalStub(i).startViewChange(view, replicaNumber);
            }
        }
        // Receiving f vote: reply
        if (viewSet.get(view).size() == DStoreSetting.getF() + 1) {
            RpcClient.internalStub(view % DStoreSetting.SERVER.size()).
                    doViewChange(view, log, latestNormalView, op, commit, replicaNumber);
        }
    }

    @Override
    public void doViewChange(int view, List<StoreAction> log,
                             int oldView, int op, int commit, int replica) {
        log("doViewChange(v: " + view + ", log, ov: " + oldView + ", op: " + op + ", ci: " + commit + ", r: " + replica + ")");
        if (this.view > view ||
                (this.status != Status.VIEWCHANGE && this.view == view)) return;
        // Change state
        status = Status.VIEWCHANGE;
        // Check view: something must be wrong here
        if (view % DStoreSetting.SERVER.size() != replicaNumber) return;
        if (!doViewSet.containsKey(view)) {
            doViewSet.put(view, new HashSet<Integer>());
            // TODO: should we group it with group number?
            vcView = this.latestNormalView;
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
            latestNormalView = this.view;
            normalLock.lock();
            normalCondition.signalAll();
            normalLock.unlock();

            // Semaphore for uncommitted log
            final int logStart = this.commit;
            final int logEnd = this.op;
            for (int i = logStart; i <= logEnd; ++i) {
                Semaphore semaphore = new Semaphore(-DStoreSetting.getF() + 1);
                voteLock.put(i, semaphore);
                voteSet.put(i, new HashSet<Integer>());
            }
            // Thread for waiting
            new Runnable() {
                @Override
                public void run() {
                    for (int i = logStart; i <= logEnd; ++i) {
                        doCommit(i);
                    }
                }
            }.run();

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
        if (this.view > view ||
                (this.status != Status.VIEWCHANGE && this.view == view)) return;

        status = Status.NORMAL;

        normalLock.lock();
        normalCondition.signalAll();
        normalLock.unlock();

        this.log = log;
        this.op = op;
        this.commit = commit;
        this.view = view;
        this.latestNormalView = view;
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
        log("recovery(r: " + replica + ", n: " + nonce + ")");
        // Status must be normal
        if (status != Status.NORMAL) return;
        // Reply
        if (isPrimary()) {
            RpcClient.internalStub(replica).
                    recoveryResponse(view, nonce, log, op, commit, replicaNumber);
        } else {
            RpcClient.internalStub(replica).
                    recoveryResponse(view, nonce, null, -1, -1, replicaNumber);
        }
    }

    @Override
    public void recoveryResponse(int view, int nonce, List<StoreAction> log,
                                 int op, int commit, int replica) {
        log("recoveryResponse(v: " + view + " ,n: " + nonce + ", log, op: " + op + ", ci: " + commit + ", r: " + replica + ")");
        // Check nonce
        if (this.nonce != nonce) return;
        int primary = view % DStoreSetting.SERVER.size();
        // Cache primary
        // TODO: check latest view?
        if (primary == replica) {
            this.view = view;
            this.log = log;
            this.op = op;
            this.recoveryCommit = commit;
        }
        recoverySet.add(replica);
        // Must have f + 1 response and response from primary
        if (recoveryLock.availablePermits() < 0 || recoverySet.contains(primary)) {
            recoveryLock.release();
        }
    }
}
