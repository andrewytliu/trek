package cs244b.dstore.server;

import cs244b.dstore.api.DStoreTesting;
import cs244b.dstore.rpc.RpcClient;

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
    public void kill() {
        server.kill();
    }

    @Override
    public void recovery() {
        server.recovery();
    }
}
