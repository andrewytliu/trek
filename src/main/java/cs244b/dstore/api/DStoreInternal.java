package cs244b.dstore.api;

import cs244b.dstore.storage.StoreAction;

public interface DStoreInternal {
    public void prepare(int view, StoreAction action, int op, int commit);
    public void prepareOk(int view, int op, int commit);
    public void commit(int view, int commit);

    public void startViewChange(int view, int replica);
    public void doViewChange
            (int view, int[] log, int oldView, int op, int commit, int replica);

    public void recovery(int replica, int nonce);
    public void recoveryResponse
            (int view, int nonce, int[] log, int op, int commit, int replica);
}
