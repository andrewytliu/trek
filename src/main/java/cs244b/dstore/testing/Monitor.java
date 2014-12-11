package cs244b.dstore.testing;

import cs244b.dstore.api.DStoreMonitor;
import cs244b.dstore.rpc.RpcServer;

public class Monitor {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public class DStoreMonitorImpl implements DStoreMonitor {
        @Override
        public void log(int replicaNumber, String rpcLog) {
            if (rpcLog.startsWith("C")) {
                System.out.println(ANSI_GREEN + "[" + replicaNumber + "] " + rpcLog + ANSI_RESET);
            } else if (rpcLog.startsWith("T")) {
                System.out.println(ANSI_YELLOW + "[" + replicaNumber + "] " + rpcLog + ANSI_RESET);
            } else {
                System.out.println("[" + replicaNumber + "] " + rpcLog);
            }
        }
    }

    private class DStoreMonitorServer extends RpcServer {
        public void setup() {
            addServlet(new DStoreMonitorImpl(), "/monitor");
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
