package cs244b.dstore.testing;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Tester {
    private int numServers;
    private boolean[][] partitioned;

    public Tester(int ns) {
        numServers = ns;
        partitioned = new boolean[numServers][numServers];
    }

    //
    private void normalTesting(int failTimes) throws InterruptedException {
        System.out.println("Fail test ...");
        System.out.print("  ");
        for (int j = 0; j < numServers; ++j) {
            System.out.print(j + " ");
        }
        System.out.println();
        for (int i = 0; i < failTimes; ++i) {
            System.out.print(i + " ");
            for (int j = 0; j < numServers; ++i) {
                RpcClient.testingStub(j).kill(i);
                Thread.sleep(DStoreSetting.HEARTBEAT_HARD * 2);
                if (isLogConsistent()) {
                    System.out.print("O ");
                } else {
                    System.out.print("X ");
                }
            }
            System.out.println();
        }
    }

    // Make the servers in arg unreachable from the ones not in arg
    private void partition(String arg) throws InvalidInputException {
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
    private void group(String arg) throws InvalidInputException {
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
    private void reset(String arg) throws InvalidInputException {
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

    // Check if logs are consistent
    private boolean isLogConsistent() {
        List<List<StoreAction>> logs = new LinkedList<>();
        int minLength = Integer.MAX_VALUE;

        for (int i = 0; i < numServers; ++i) {
            List<StoreAction> log;
            try {
                log = RpcClient.testingStub(i).committedLog();
            } catch (Exception e) {
                continue;
            }
            logs.add(log);
            if (log.size() < minLength) minLength = log.size();
        }

        for (int i = 0; i < minLength; ++i) {
            StoreAction first = logs.get(0).get(i);
            for (int j = 1; j < numServers; ++j) {
                if (!first.equals(logs.get(j).get(i))) return false;
            }
        }
        return true;
    }

    // Kill the server
    // TODO: what if killed twice?
    private void kill(String server) throws InvalidInputException {
        RpcClient.testingStub(getServer(server)).kill(0);
    }

    // Perform recovery
    // TODO: what if recover twice?
    private void recover(String server) throws InvalidInputException {
        RpcClient.testingStub(getServer(server)).recover();
    }

    private void printHealth() {
        for (int i = 0; i < numServers; ++i) {
            System.out.print(i + " ");
        }
        System.out.println();
        for (int i = 0; i < numServers; ++i) {
            boolean alive;
            try {
                alive = RpcClient.testingStub(i).isAlive();
            } catch (Exception e) {
                alive = false;
            }
            if (alive) {
                System.out.print("O ");
            } else {
                System.out.print("X ");
            }
        }
        System.out.println();
    }

    private void printPartition() {
        System.out.print("  ");
        for (int j = 0; j < numServers; ++j) {
            System.out.print(j + " ");
        }
        System.out.println();
        for (int i = 0; i < numServers; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < numServers; j++) {
                if (partitioned[i][j]) {
                    System.out.print("X ");
                } else {
                    System.out.print("O ");
                }
            }
            System.out.println();
        }
        updateServers();
    }

    private ArrayList<Integer> getServers(String arg) throws InvalidInputException {
        String[] serverStrings = arg.split("\\|");
        ArrayList<Integer> servers = new ArrayList<Integer>();
        for (String s : serverStrings) {
            servers.add(getServer(s));
        }
        return servers;
    }

    private int getServer(String server) throws InvalidInputException {
        try {
            Integer val = Integer.valueOf(server);
            if (val < 0 || val >= numServers) {
                throw new InvalidInputException();
            }
            return val;
        } catch (NumberFormatException e) {
            throw new InvalidInputException();
        }
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
            RpcClient.testingStub(i).setPartitioned(getPartitioned(i), 0);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("[USAGE] server1|server2|...");
        }
        DStoreSetting.setServer(args[0]);
        int numServers = DStoreSetting.SERVER.size();

        Tester t = new Tester(numServers);
        RpcClient.setPartitioned(Collections.nCopies(numServers, Boolean.FALSE));

        ConsoleReader reader = new ConsoleReader();
        reader.setPrompt("> ");
        String line;

        while ((line = reader.readLine()) != null) {
            String[] input = line.split(" ");
            String command = input[0];
            String arg = (input.length > 1) ? input[1] : null;

            try {
                if (command.equalsIgnoreCase("help")) {
                    // TODO
                } else if (command.equalsIgnoreCase("partition")) {
                    if (arg == null) {
                        System.err.println("[USAGE] parition server1|...");
                    } else {
                        t.partition(arg);
                    }
                } else if (command.equalsIgnoreCase("group")) {
                    if (arg == null) {
                        System.err.println("[USAGE] group server1|server2|...");
                    } else {
                        t.group(arg);
                    }
                } else if (command.equalsIgnoreCase("reset")) {
                    t.reset(arg);
                } else if (command.equalsIgnoreCase("print")) {
                    t.printPartition();
                } else if (command.equalsIgnoreCase("kill")) {
                    if (arg == null) {
                        System.err.println("[USAGE] kill server1");
                    } else {
                        t.kill(arg);
                    }
                } else if (command.equalsIgnoreCase("recover")) {
                    if (arg == null) {
                        System.err.println("[USAGE] recover server1|server2|...");
                    } else {
                        t.recover(arg);
                    }
                } else if (command.equalsIgnoreCase("health")) {
                    t.printHealth();
                } else if (command.equalsIgnoreCase("consistent")) {
                    System.out.println(t.isLogConsistent());
                } else if (command.equalsIgnoreCase("normal")) {
                    t.normalTesting(10);
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
