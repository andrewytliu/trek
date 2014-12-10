package cs244b.dstore.server;

import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.api.DStoreTesting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DStoreTestingImpl implements DStoreTesting {
    private static final Logger logger = Logger.getLogger("cs244b.VR");
    private DStoreServer server;
    private int replicaNumber;

    public DStoreTestingImpl(int numServers, DStoreServer server, int replicaNumber) {
        RpcClient.setPartitioned(Collections.nCopies(numServers, Boolean.FALSE));
        this.server = server;
        this.replicaNumber = replicaNumber;
    }

    private void log(String l) {
        String str = "XX " + l;
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

    @Override
    public void setPartitioned(List<Boolean> values, int rpcCount) {
        log("setPartitioned(values, " + rpcCount + ")");
        RpcClient.setPartitioned(values, rpcCount);
    }

    @Override
    public List<StoreAction> getCommitLog() {
        log("getCommitLog()");
        return server.getCommitLog();
    }

    @Override
    public void kill(int rpcCount) {
        log("kill(" + rpcCount + ")");
        RpcClient.setKill(server, rpcCount);
    }

    @Override
    public void recover() {
        log("recover()");
        server.recover();
    }

    @Override
    public boolean isAlive() {
        log("isAlive() => " + server.isAlive());
        return server.isAlive();
    }

    @Override
    public void setHeartbeat(int ms) {
        server.kill();
        DStoreSetting.setHeartbeat(ms);
        server.restart();
    }

    @Override
    public void clear() {
        RpcClient.clear();
    }
}
