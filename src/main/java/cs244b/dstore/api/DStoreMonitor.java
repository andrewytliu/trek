package cs244b.dstore.api;

public interface DStoreMonitor {
    public void log(int replicaNumber, String rpcLog);
}
