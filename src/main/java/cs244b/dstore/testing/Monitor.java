package cs244b.dstore.testing;

import cs244b.dstore.api.DStoreMonitor;
import cs244b.dstore.rpc.RpcServer;

public class Monitor {
    public class DStoreMonitorImpl implements DStoreMonitor {
        @Override
        public void log(int replicaNumber, String rpcLog) {
            System.out.println("[" + replicaNumber + "] " + rpcLog);
        }
    }

    private class DStoreMonitorServer extends RpcServer {
        public void setup() {
            addServlet(new DStoreMonitorImpl(), "/monitor.json");
        }
    }

    private DStoreMonitorServer server;

    public Monitor() {
        server = new DStoreMonitorServer();
        server.setup();
        server.start();
    }

    public static void main(String[] args) {
        new Monitor();
    }
}
