package cs244b.dstore.server;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcServer;

public class DStoreServer extends RpcServer {
    public void start(int number) {
        DStoreInternalImpl internal = new DStoreInternalImpl(number);
        DStoreServiceImpl service = new DStoreServiceImpl(internal);

        addServlet(internal, "/internal.json");
        addServlet(service, "/service.json");
        super.start();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("[USAGE] server1|server2|... this_server");
        }
        DStoreSetting.setServer(args[0]);
        DStoreServer server = new DStoreServer();
        server.start(DStoreSetting.SERVER.indexOf(args[1]));
    }
}
