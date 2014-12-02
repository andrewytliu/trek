package cs244b.dstore.api;

import cs244b.dstore.storage.StoreAction;

import java.util.List;

public interface DStoreTesting {
    // Creating network partition
    public void setPartitioned(List<Boolean> values);
    // Get committed log
    public List<StoreAction> committedLog();
    // Kill the requested node
    public void kill();
    // Recover the node
    public void recover();
    // Check if the server is alive
    public boolean isAlive();
}
