package cs244b.dstore.client;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.Entry;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class DStoreClient {
    private static Logger logger = Logger.getLogger("cs244b.client");

    private int primary;
    private int client;
    private int request;
    private HashMap<String, Integer> versions;

    public DStoreClient() {
        primary = 0;
        request = 0;
        versions = new HashMap<String, Integer>();
        switchPrimary();
    }

    private void switchPrimary() {
        while (true) {
            try {
                logger.info("Testing primary: " + primary);
                primary = RpcClient.serviceStub(primary).primary();
                logger.info("Getting client id: " + client);
                client = RpcClient.serviceStub(primary).id();
                break;
            } catch (Throwable t) {
                primary = ++primary % DStoreSetting.SERVER.size();
            }
        }
    }

    private StoreResponse request(StoreAction action) {
        StoreResponse resp;
        while (true) {
            try {
                resp = RpcClient.serviceStub(0).request(action, client, request);
                if (resp.getStatus() != StoreResponse.Status.NOT_PRIMARY) break;
            } catch (Throwable t) {

            }
            switchPrimary();
        }
        request++;
        return resp;
    }

    private int getVersion(String path) {
        Integer v = versions.get(path);
        return (v == null) ? -1 : v;
    }

    private void setVersion(String path, int version) {
        versions.put(path, version);
    }

    private static boolean parseBool(String arg) throws InvalidInputException {
        if (arg.equalsIgnoreCase("true")) {
            return true;
        }
        if (arg.equalsIgnoreCase("false")) {
            return false;
        }
        throw new InvalidInputException();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("[USAGE] server1|server2|...");
        }
        DStoreSetting.setServer(args[0]);

        DStoreClient client = new DStoreClient();

        ConsoleReader reader = new ConsoleReader();
        reader.setPrompt("> ");
        String line;

        while ((line = reader.readLine()) != null) {
            String[] input = line.split(" ");
            if (input.length < 2) {
                System.err.println("[USAGE] command path [additional arguments]");
                continue;
            }
            String command = input[0];
            String path = input[1];
            StoreAction act = null;

            if (command.equalsIgnoreCase("create")) {
                try {
                    if (input.length < 3 || input.length > 4) {
                        throw new InvalidInputException();
                    }
                    String data = input[2];
                    boolean isSequential = (input.length == 4) && parseBool(input[3]);
                    act = StoreAction.create(path, data, isSequential);
                } catch (InvalidInputException e) {
                    System.err.println("[USAGE] create path data [isSequential=false]");
                    continue;
                }
            } else if (command.equalsIgnoreCase("delete")) {
                try {
                    if (input.length > 3) {
                        throw new InvalidInputException();
                    }
                    boolean ignoreVersion = (input.length == 3) && parseBool(input[2]);
                    int version = (ignoreVersion) ? -1 : client.getVersion(path);
                    act = StoreAction.delete(path, version);
                } catch (InvalidInputException e) {
                    System.err.println("[USAGE] delete path [ignoreVersion=false]");
                    continue;
                }
            } else if (command.equalsIgnoreCase("exists")) {
                try {
                    if (input.length > 2) {
                        throw new InvalidInputException();
                    }
                    act = StoreAction.exists(path);
                } catch (InvalidInputException e) {
                    System.err.println("[USAGE] exists path");
                    continue;
                }
            } else if (command.equalsIgnoreCase("getData")) {
                try {
                    if (input.length > 2) {
                        throw new InvalidInputException();
                    }
                    act = StoreAction.getData(path);
                } catch (InvalidInputException e) {
                    System.err.println("[USAGE] getData path");
                    continue;
                }
            } else if (command.equalsIgnoreCase("setData")) {
                try {
                    if (input.length < 3 || input.length > 4) {
                        throw new InvalidInputException();
                    }
                    String data = input[2];
                    boolean ignoreVersion = (input.length == 4) && parseBool(input[3]);
                    int version = (ignoreVersion) ? -1 : client.getVersion(path);
                    act = StoreAction.setData(path, data, version);
                } catch (InvalidInputException e) {
                    System.err.println("[USAGE] setData path data [ignoreVersion=false]");
                    continue;
                }
            } else if (command.equalsIgnoreCase("getChildren")) {
                try {
                    if (input.length > 2) {
                        throw new InvalidInputException();
                    }
                    act = StoreAction.getChildren(path);
                } catch (InvalidInputException e) {
                    System.err.println("[USAGE] getChildren path");
                    continue;
                }
            } else {
                System.err.println("Unrecognized command");
                continue;
            }

            StoreResponse resp = client.request(act);

            if (resp.getStatus() != StoreResponse.Status.OK) {
                System.err.println("Request failed with status " + resp.getStatus().toString());
            } else {
                if (command.equalsIgnoreCase("create")) {
                    String createdPath = (String)resp.getValue();
                    System.out.println("Created node at " + createdPath);
                    client.setVersion(createdPath, 0);
                } else if (command.equalsIgnoreCase("delete")) {
                    System.out.println("OK");
                } else if (command.equalsIgnoreCase("exists")) {
                    System.out.println((String)resp.getValue());
                } else if (command.equalsIgnoreCase("getData")) {
                    Entry dataEntry = (Entry)resp.getValue();
                    System.out.println(dataEntry.value);
                    client.setVersion(path, dataEntry.version);
                } else if (command.equalsIgnoreCase("setData")) {
                    System.out.println("OK");
                    Entry dataEntry = (Entry)resp.getValue();
                    client.setVersion(path, dataEntry.version);
                } else { //getChildren
                    List<String> children = (List<String>)resp.getValue();
                    for (String c : children) {
                        System.out.println(c);
                    }
                }
            }
        }
    }

    private static class InvalidInputException extends Exception {
    }
}
