package cs244b.dstore.server;

import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.KeyValueStore;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.util.*;
import java.util.concurrent.Semaphore;

public class DStoreInternalImpl implements DStoreInternal {
    private enum Status {
        NORMAL, VIEWCHANGE, RECOVERING
    }

    private int replicaNumber;
    private int view;
    private Status status;
    private int op;
    private List<StoreAction> log;
    private int commit;
    private Map<Integer, Semaphore> voteLock;
    private Map<Integer, Set<Integer>> voteSet;
    private KeyValueStore storage;

    public DStoreInternalImpl(int number) {
        replicaNumber = number;
        view = 0;
        status = Status.NORMAL;
        op = 0;
        log = new ArrayList<>();
        commit = -1;
        voteLock = new HashMap<>();
        voteSet = new HashMap<>();
        storage = new KeyValueStore();
    }

    public int getPrimary() {
        return view % DStoreSetting.getServerNum();
    }

    public boolean isPrimary() {
        return getPrimary() == replicaNumber;
    }

    // TODO: set the timer for commit
    public int startTransactionPrimary(StoreAction action) {
        // TODO: need to redirect the request to primary
        if (!isPrimary()) return -1;

        // Putting the action in log and increase the op number
        log.add(action);
        op++;
        // Acquire all f semaphore
        Semaphore semaphore = new Semaphore(DStoreSetting.getF());
        semaphore.acquireUninterruptibly(DStoreSetting.getF());
        voteLock.put(op, semaphore);
        voteSet.put(op, new HashSet<Integer>());
        // Send prepare to all cohorts
        for (int i = 0; i < DStoreSetting.getServerNum(); ++i) {
            if (i == replicaNumber) continue;
            RpcClient.internalStub(i).prepare(view, action, op, commit);
        }
        return op;
    }

    // TODO: timeout when not succeed
    public StoreResponse doCommitPrimary(int op) {
        voteLock.get(op).acquireUninterruptibly();
        voteLock.remove(op);
        return storage.apply(log.get(op));
    }

    private void doCommit(int commit) {
        for (int i = this.commit + 1; i < commit; ++i) {
            storage.apply(log.get(i));
        }
        this.commit = commit;
    }

    @Override
    public void prepare(int view, StoreAction action, int op, int commit) {
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
        RpcClient.internalStub(getPrimary()).prepareOk(view, op, replicaNumber);
    }

    @Override
    public void prepareOk(int view, int op, int replica) {
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
        // State need to be NORMAL
        if (status != Status.NORMAL) return;
        // Drop the message if the sender is behind
        if (this.view > view) return;
        // TODO: perform state transfer
        if (this.view < view) return;
        doCommit(commit);
    }

    @Override
    public void startViewChange(int view, int replica) {

    }

    @Override
    public void doViewChange(int view, int[] log, int oldView, int op, int commit, int replica) {

    }

    @Override
    public void recovery(int replica, int nonce) {

    }

    @Override
    public void recoveryResponse(int view, int nonce, int[] log, int op, int commit, int replica) {

    }
}
