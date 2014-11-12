package cs244b.dstore.server;

import cs244b.dstore.rpc.RpcServer;

public class DStoreServer extends RpcServer {
    public void start(int number) {
        DStoreInternalImpl internal = new DStoreInternalImpl(number);
        DStoreServiceImpl service = new DStoreServiceImpl(internal);

        addServlet(internal, "/internal.json");
        addServlet(service, "/service.json");
        super.start();
    }
}
