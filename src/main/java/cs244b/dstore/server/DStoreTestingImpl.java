package cs244b.dstore.server;

import cs244b.dstore.api.DStoreTesting;
import cs244b.dstore.rpc.RpcClient;

import java.util.Collections;
import java.util.List;

public class DStoreTestingImpl implements DStoreTesting {
    public DStoreTestingImpl(int numServers) {
        RpcClient.setPartitioned(Collections.nCopies(numServers, Boolean.FALSE));
    }

    public void setPartitioned(List<Boolean> values) {
        RpcClient.setPartitioned(values);
    }
}
