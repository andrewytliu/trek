package cs244b.dstore.testing;

import cs244b.dstore.api.DStoreMonitor;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.rpc.RpcServer;
import cs244b.dstore.server.DStoreInternalImpl;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.util.ArrayList;

public class Monitor {
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
    }

    private int numServers;
    private boolean[][] partitioned;
    private DStoreMonitorServer server;

    public Monitor(int ns) {
        numServers = ns;
        partitioned = new boolean[numServers][numServers];
        server = new DStoreMonitorServer();
        server.setup();
        server.start();
    }

    // Make the servers in arg unreachable from the ones not in arg
    public void partition(String arg) throws InvalidInputException {
        ArrayList<Integer> servers = getServers(arg);
        for (int i = 0; i < numServers; i++) {
            for (int j = 0; j < numServers; j++) {
                if (servers.contains(i) && !servers.contains(j)
                        || !servers.contains(i) && servers.contains(j)) {
                    partitioned[i][j] = true;
                }
            }
        }
        updateServers();
    }

    // Make the servers in arg reachable from each other
    public void group(String arg) throws InvalidInputException {
        ArrayList<Integer> servers = getServers(arg);
        for (int i = 0; i < numServers; i++) {
            for (int j = 0; j < numServers; j++) {
                if (servers.contains(i) && servers.contains(j)) {
                    partitioned[i][j] = false;
                }
            }
        }
        updateServers();
    }

    // Make the servers in arg reachable from everyone
    // If arg == null, then all partitions are cleared
    public void reset(String arg) throws InvalidInputException {
        if (arg == null) {
            partitioned = new boolean[numServers][numServers];
        } else {
            ArrayList<Integer> servers = getServers(arg);
            for (int i = 0; i < numServers; i++) {
                for (int j = 0; j < numServers; j++) {
                    if (servers.contains(i) || servers.contains(j)) {
                        partitioned[i][j] = false;
                    }
                }
            }
        }
        updateServers();
    }

    private ArrayList<Integer> getServers(String arg) throws InvalidInputException {
        String[] serverStrings = arg.split("\\|");
        ArrayList<Integer> servers = new ArrayList<Integer>();
        for (String s : serverStrings) {
            try {
                Integer val = Integer.valueOf(s);
                if (val < 0 || val >= numServers) {
                    throw new InvalidInputException();
                }
                servers.add(val);
            } catch (NumberFormatException e) {
                throw new InvalidInputException();
            }
        }
        return servers;
    }

    private ArrayList<Boolean> getPartitioned(int sid) {
        ArrayList<Boolean> res = new ArrayList<Boolean>();
        for (int i = 0; i < numServers; i++) {
            res.add(partitioned[sid][i]);
        }
        return res;
    }

    private void updateServers() {
        for (int i = 0; i < numServers; i++) {
            RpcClient.testingStub(i).setPartitioned(getPartitioned(i));
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("[USAGE] server1|server2|...");
        }
        DStoreSetting.setServer(args[0]);

        Monitor m = new Monitor(DStoreSetting.SERVER.size());

        ConsoleReader reader = new ConsoleReader();
        reader.setPrompt("> ");
        String line;

        while ((line = reader.readLine()) != null) {
            String[] input = line.split(" ");
            String command = input[0];
            String arg = (input.length > 1) ? input[1] : null;

            try {
                if (command.equalsIgnoreCase("partition")) {
                    if (arg == null) {
                        System.err.println("[USAGE] parition server1|...");
                    } else {
                        m.partition(arg);
                    }
                } else if (command.equalsIgnoreCase("group")) {
                    if (arg == null) {
                        System.err.println("[USAGE] group server1|server2|...");
                    } else {
                        m.group(arg);
                    }
                } else if (command.equalsIgnoreCase("reset")) {
                    m.reset(arg);
                } else {
                    System.err.println("Unrecognized command");
                }
            } catch (InvalidInputException e) {
                System.err.println("The server list is invalid");
            }
        }
    }

    private static class InvalidInputException extends Exception {
    }
}
