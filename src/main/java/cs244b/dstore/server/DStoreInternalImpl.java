package cs244b.dstore.server;

import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.KeyValueStore;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private KeyValueStore storage;

    public DStoreInternalImpl(int number) {
        replicaNumber = number;
        view = 0;
        status = Status.NORMAL;
        op = 0;
        log = new ArrayList<>();
        commit = -1;
        voteLock = new HashMap<>();
        storage = new KeyValueStore();
    }

    public int getPrimary() {
        return view % DStoreSetting.getServerNum();
    }

    public boolean isPrimary() {
        return getPrimary() == replicaNumber;
    }

    // TODO: set the timer for commit
    public int proceedClient(StoreAction action) {
        if (!isPrimary()) return -1;

        log.add(action);
        op++;
        Semaphore semaphore = new Semaphore(DStoreSetting.getF());
        semaphore.acquireUninterruptibly(DStoreSetting.getF());
        voteLock.put(op, semaphore);
        for (int i = 0; i < DStoreSetting.getServerNum(); ++i) {
            if (i == replicaNumber) continue;
            RpcClient.internalStub(i).prepare(view, action, op, commit);
        }
        return op;
    }

    // TODO: timeout when not succeed
    public StoreResponse doCommit(int op) {
        voteLock.get(op).acquireUninterruptibly();
        voteLock.remove(op);
        return storage.apply(log.get(op));
    }

    @Override
    public void prepare(int view, StoreAction action, int op, int commit) {
        // TODO: check log for all early entries
        // TODO: check view?
        // TODO: commit number?
        if (this.op + 1 != op) return;

        log.add(action);
        this.op++;
        RpcClient.internalStub(getPrimary()).prepareOk(view, op, replicaNumber);
    }

    @Override
    public void prepareOk(int view, int op, int commit) {
        // TODO: commit number?
        // TODO: check view?
        if (voteLock.containsKey(op)) {
            voteLock.get(op).release();
        }
    }

    @Override
    public void commit(int view, int commit) {
        // TODO: commit number?
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
