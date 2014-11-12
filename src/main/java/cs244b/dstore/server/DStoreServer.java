package cs244b.dstore.server;

import cs244b.dstore.rpc.RpcServer;

public class DStoreServer extends RpcServer {
    public void start() {
        addServlet(new DStoreServiceImpl(), "/client.json");
        addServlet(new DStoreInternalImpl(), "/internal.json");
        super.start();
    }
}
