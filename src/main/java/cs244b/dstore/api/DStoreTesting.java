package cs244b.dstore.api;

import cs244b.dstore.storage.StoreAction;

import java.util.List;

public interface DStoreTesting {
    // Creating network partition after n rpc outgoing call
    public void setPartitioned(List<Boolean> values, int rpcCount);
    // Get committed log
    public List<StoreAction> committedLog();
    // Kill the requested node after n rpc outgoing call
    public void kill(int rpcCount);
    // Recover the node
    public void recover();
    // Check if the server is alive
    public boolean isAlive();
}
