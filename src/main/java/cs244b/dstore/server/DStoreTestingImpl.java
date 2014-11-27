package cs244b.dstore.server;

import cs244b.dstore.api.DStoreTesting;

public class DStoreTestingImpl implements DStoreTesting {
    private boolean isPartitioned = false;

    public synchronized void setIsPartitioned(boolean value) {
        isPartitioned = value;
    }

    public synchronized boolean getIsPartitioned() {
        return isPartitioned;
    }
}
