package cs244b.dstore.server;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcServer;

public class DStoreServer extends RpcServer {
    private DStoreInternalImpl internal;
    private DStoreServiceImpl service;

    public void setup(int number) {
        internal = new DStoreInternalImpl(number);
        service = new DStoreServiceImpl(internal);

        addServlet(internal, "/internal.json");
        addServlet(service, "/service.json");
    }

    public void recovery() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(DStoreSetting.RECOVERY_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    internal.startRecovery();
                    internal.doRecovery();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        // TODO: using argument parse lib
        if (args.length < 2) {
            System.err.println("[USAGE] server1|server2|... server_index [recovery]");
        }
        DStoreSetting.setServer(args[0]);
        DStoreServer server = new DStoreServer();
        server.setup(Integer.valueOf(args[1]));
        if (args.length == 3) {
            server.recovery();
        }
        server.start();
    }
}
