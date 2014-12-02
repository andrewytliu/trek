package cs244b.dstore.server;

import cs244b.dstore.api.DStoreTesting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;

import java.util.Collections;
import java.util.List;

public class DStoreTestingImpl implements DStoreTesting {
    private DStoreServer server;

    public DStoreTestingImpl(int numServers, DStoreServer server) {
        RpcClient.setPartitioned(Collections.nCopies(numServers, Boolean.FALSE));
        this.server = server;
    }

    @Override
    public void setPartitioned(List<Boolean> values) {
        RpcClient.setPartitioned(values);
    }

    @Override
    public List<StoreAction> committedLog() {
        return server.comittedLog();
    }

    @Override
    public void kill() {
        server.kill();
    }

    @Override
    public void recover() {
        server.recover();
    }

    @Override
    public boolean isAlive() {
        return server.isAlive();
    }
}
