package cs244b.dstore.server;

import cs244b.dstore.api.DStoreInternal;

public class DStoreInternalImpl implements DStoreInternal {
    @Override
    public void prepare(int view, int op, int commit) {

    }

    @Override
    public void prepareOk(int view, int op, int commit) {

    }

    @Override
    public void commit(int view, int commit) {

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
