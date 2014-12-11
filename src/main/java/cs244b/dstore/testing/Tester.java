package cs244b.dstore.testing;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.client.DStoreClient;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;
import jline.console.ConsoleReader;

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

    private interface FailureCreator {
        void fail(int server, int rpcCount);
    }

    private void testRun(int failTimes, FailureCreator failureCreator) throws Exception {
        System.out.print("  ");
        for (int j = 0; j < numServers; ++j) {
            System.out.print(j + " ");
        }
        System.out.println();
        for (int i = 0; i < failTimes; ++i) {
            System.out.print(i + " ");
            for (int j = 0; j < numServers; ++j) {
                // Fail
                failureCreator.fail(j, i);
                Thread.sleep(DStoreSetting.HEARTBEAT_HARD * 2);
                // Check consistency
                DStoreClient client = new DStoreClient();
                client.request(StoreAction.exists("/"));
                if (isLogConsistent()) {
                    System.out.print("O ");
                } else {
                    System.out.print("X ");
                }
                // Recovering
                RpcClient.testingStub(j).clear();
                if (!RpcClient.testingStub(j).isAlive()) {
                    RpcClient.testingStub(j).recover();
                }
                reset(null);
                Thread.sleep(DStoreSetting.HEARTBEAT_HARD * 2);
            }
            System.out.println();
        }
    }

    private void normalTesting(int failTimes) throws Exception {
        System.out.println("Testing crash fail ...");
        testRun(failTimes, new FailureCreator() {
            @Override
            public void fail(int server, int rpcCount) {
                RpcClient.testingStub(server).kill(rpcCount);
            }
        });
    }

    private void partitionTesting(int failTimes) throws Exception {
        System.out.println("Testing network configuration ...");
        for (final List<Boolean> conf : getPossiblePartitions()) {
            System.out.println("Partition: " + conf);
            testRun(failTimes, new FailureCreator() {
                @Override
                public void fail(int server, int rpcCount) {
                    setPartition(conf);
                    updateServers(rpcCount);
                }
            });
        }
    }

    private void setPartition(List<Boolean> conf) {
        for (int i = 0; i < numServers; ++i) {
            boolean self = conf.get(i);
            for (int j = 0; j < numServers; ++j) {
                partitioned[i][j] = (self != conf.get(j));
            }
        }
    }

    private ArrayList<ArrayList<Boolean>> getPossiblePartitions() {
        ArrayList<ArrayList<Boolean>> res = new ArrayList<>();
        int n = DStoreSetting.SERVER.size();
        int p = (int)Math.pow(2, n);
        for (int i = 1; i < p; i++) {
            String binString = Integer.toBinaryString(i);
            ArrayList<Boolean> permutation = new ArrayList<>();
            int falseCount = 0;
            for (int j = 0; j < n-binString.length(); j++) {
                permutation.add(false);
                falseCount++;
            }
            for (char c : binString.toCharArray()) {
                if (c == '0') {
                    permutation.add(false);
                    falseCount++;
                } else {
                    permutation.add(true);
                }
            }
            if (falseCount >= n - DStoreSetting.getF()) {
                res.add(permutation);
            }
        }
        return res;
    }

    // Make the servers in arg unreachable from the ones not in arg
    private void partition(String arg, int rpcCount) throws InvalidInputException {
        ArrayList<Integer> servers = getServers(arg);
        for (int i = 0; i < numServers; i++) {
            for (int j = 0; j < numServers; j++) {
                if (servers.contains(i) && !servers.contains(j)
                        || !servers.contains(i) && servers.contains(j)) {
                    partitioned[i][j] = true;
                }
            }
        }
        updateServers(rpcCount);
    }

    // Make the servers in arg reachable from each other
    private void group(String arg, int rpcCount) throws InvalidInputException {
        ArrayList<Integer> servers = getServers(arg);
        for (int i = 0; i < numServers; i++) {
            for (int j = 0; j < numServers; j++) {
                if (servers.contains(i) && servers.contains(j)) {
                    partitioned[i][j] = false;
                }
            }
        }
        updateServers(rpcCount);
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
        updateServers(0);
    }

    private void updateServers(int rpcCount) {
        for (int i = 0; i < numServers; i++) {
            RpcClient.testingStub(i).setPartitioned(getPartitioned(i), rpcCount);
        }
    }

    // Check if logs are consistent
    private boolean isLogConsistent() {
        List<List<StoreAction>> logs = new LinkedList<>();
        int minLength = Integer.MAX_VALUE;

        for (int i = 0; i < numServers; ++i) {
            List<StoreAction> log;
            try {
                log = RpcClient.testingStub(i).getCommitLog();
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
    private void kill(String server, int rpcCount) throws InvalidInputException {
        RpcClient.testingStub(getServer(server)).kill(rpcCount);
    }

    // Perform recovery
    // TODO: what if recover twice?
    private void recover(String server) throws InvalidInputException {
        RpcClient.testingStub(getServer(server)).recover();
    }

    private void setHeartbeat(int ms) {
        for (int i = 0; i < numServers; ++i) {
            RpcClient.testingStub(i).setHeartbeat(ms);
        }
        DStoreSetting.setHeartbeat(ms);
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
            String arg1 = (input.length > 1) ? input[1] : null;
            String arg2 = (input.length > 2) ? input[2] : null;

            try {
                if (command.equalsIgnoreCase("help")) {
                    // TODO
                } else if (command.equalsIgnoreCase("partition")) {
                    if (arg1 == null) {
                        System.err.println("[USAGE] parition server1|... [rpcCount]");
                    } else {
                        int rpcCount = 0;
                        if (arg2 != null) rpcCount = Integer.valueOf(arg2);
                        t.partition(arg1, rpcCount);
                    }
                } else if (command.equalsIgnoreCase("group")) {
                    if (arg1 == null) {
                        System.err.println("[USAGE] group server1|server2|... [rpcCount]");
                    } else {
                        int rpcCount = 0;
                        if (arg2 != null) rpcCount = Integer.valueOf(arg2);
                        t.group(arg1, rpcCount);
                    }
                } else if (command.equalsIgnoreCase("reset")) {
                    t.reset(arg1);
                } else if (command.equalsIgnoreCase("print")) {
                    t.printPartition();
                } else if (command.equalsIgnoreCase("kill")) {
                    if (arg1 == null) {
                        System.err.println("[USAGE] kill server1 [rpcCount]");
                    } else {
                        int rpcCount = 0;
                        if (arg2 != null) rpcCount = Integer.valueOf(arg2);
                        t.kill(arg1, rpcCount);
                    }
                } else if (command.equalsIgnoreCase("recover")) {
                    if (arg1 == null) {
                        System.err.println("[USAGE] recover server1");
                    } else {
                        t.recover(arg1);
                    }
                } else if (command.equalsIgnoreCase("health")) {
                    t.printHealth();
                } else if (command.equalsIgnoreCase("consistent")) {
                    System.out.println(t.isLogConsistent());
                } else if (command.equalsIgnoreCase("heartbeat")) {
                    if (arg1 == null) {
                        System.err.println("[USAGE] heartbeat ms");
                    } else {
                        t.setHeartbeat(Integer.valueOf(arg1));
                    }
                } else if (command.equalsIgnoreCase("normaltest")) {
                    t.normalTesting(5);
                } else if (command.equalsIgnoreCase("partitiontest")) {
                    t.partitionTesting(5);
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
