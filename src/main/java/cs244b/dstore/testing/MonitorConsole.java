package cs244b.dstore.testing;

import cs244b.dstore.api.DStoreMonitor;
import cs244b.dstore.rpc.RpcServer;

public class MonitorConsole {
    private class DStoreMonitorImpl implements DStoreMonitor {
        @Override
        public void log(int replicaNumber, String rpcLog) {
            System.out.println("[" + replicaNumber + "] " + rpcLog);
        }
    }

    private class DStoreMonitorServer extends RpcServer {
        public void setup() {
            addServlet(new DStoreMonitorImpl(), "/monitor.json");
        }

        @Override
        public void start() {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    DStoreMonitorServer.super.start();
                }
            });
            t.start();
        }
    }

    private DStoreMonitorServer server;

    public MonitorConsole() {
        server = new DStoreMonitorServer();
        server.setup();
        server.start();
    }

    public static void main(String[] args) {
        new MonitorConsole();
    }
}
