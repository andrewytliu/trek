package cs244b.dstore.client;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;
import jline.console.ConsoleReader;

import java.io.IOException;
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

        ConsoleReader reader = new ConsoleReader();
        reader.setPrompt("> ");
        String line;

        while ((line = reader.readLine()) != null) {
            String[] input = line.split(" ");
            if (input[0].equals("create")) {
                StoreResponse resp = client.request(StoreAction.create(input[1],input[2],false));
                System.out.println("Response: " + resp.getValue());
            }
        }
    }
}
