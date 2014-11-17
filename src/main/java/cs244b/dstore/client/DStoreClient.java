package cs244b.dstore.client;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

public class DStoreClient {
    private static Logger logger = Logger.getLogger("cs244b.client");

    private int primary;
    private int client;
    private int request;

    public DStoreClient() {
        primary = 0;
        request = 0;
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

    public StoreResponse request(StoreAction action) {
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

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("[USAGE] server1|server2|...");
        }
        DStoreSetting.setServer(args[0]);

        DStoreClient client = new DStoreClient();
        HashMap<String, Integer> versions = new HashMap<String, Integer>();

        ConsoleReader reader = new ConsoleReader();
        reader.setPrompt("> ");
        String line;

        while ((line = reader.readLine()) != null) {
            String[] input = line.split(" ");
            if (input.length < 2) {
                System.out.println("Insufficient arguments");
                continue;
            }
            String command = input[0];
            String path = input[1];
            StoreAction act = null;

            if (command.equalsIgnoreCase("create")) {
                if (input.length < 3 || input.length > 4) {
                    System.out.println("[USAGE] create path data [isSequential=false]");
                    continue;
                }
                String data = input[2];
                boolean isSequential;
                if (input.length == 3 || input[3].equalsIgnoreCase("false")) {
                    isSequential = false;
                } else if (input.length == 4 && input[3].equalsIgnoreCase("true")) {
                    isSequential = true;
                } else {
                    System.out.println("[USAGE] create path data [isSequential=false]");
                    continue;
                }
                act = StoreAction.create(path, data, isSequential);
            } else if (command.equalsIgnoreCase("delete")) {

            } else if (command.equalsIgnoreCase("exists")) {

            } else if (command.equalsIgnoreCase("getData")) {

            } else if (command.equalsIgnoreCase("setData")) {

            } else if (command.equalsIgnoreCase("getChildren")) {

            } else {
                System.out.println("Unrecognized command");
                continue;
            }

            StoreResponse resp = client.request(act);

            if (resp.getStatus() != StoreResponse.Status.OK) {
                System.out.println("Request failed with status " + resp.getStatus().toString());
            } else {
                //TODO: Customize based on command
                System.out.println("Response: " + resp.getValue());
            }
        }
    }
}
