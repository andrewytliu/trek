package cs244b.dstore.api;

import cs244b.dstore.storage.StoreAction;

import java.util.List;

public interface DStoreTesting {
    // Set the network state at this node to values after rpcCount outgoing RPC requests
    public void setPartitioned(List<Boolean> values, int rpcCount);
    // Get commit log
    public List<StoreAction> getCommitLog();
    // Kill the requested node after n rpc outgoing call
    public void kill(int rpcCount);
    // Recover the node
    public void recover();
    // Check if the server is alive
    public boolean isAlive();
}
