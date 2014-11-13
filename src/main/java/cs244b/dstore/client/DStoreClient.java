package cs244b.dstore.client;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;
import jline.console.ConsoleReader;

import java.io.IOException;

public class DStoreClient {
    private int primary;
    private int client;
    private int request;

    public DStoreClient() {
        primary = 0;
        request = 0;
        switchPrimary();
    }

    private void switchPrimary() {
        primary = RpcClient.serviceStub(primary).primary();
        client = RpcClient.serviceStub(primary).id();
    }

    public StoreResponse request(StoreAction action) {
        StoreResponse resp;
        while (true) {
            resp = RpcClient.serviceStub(0).request(action, client, request);
            if (resp.getStatus() != StoreResponse.Status.NOT_PRIMARY) break;
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
