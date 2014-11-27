package cs244b.dstore.testing;

import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import jline.console.ConsoleReader;

import java.io.IOException;

public class Monitor {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("[USAGE] server1|server2|...");
        }
        DStoreSetting.setServer(args[0]);

        ConsoleReader reader = new ConsoleReader();
        reader.setPrompt("> ");
        String line;

        while ((line = reader.readLine()) != null) {
            //TODO: Modify to fit new implementation of partition testing

            String[] input = line.split(" ");
            if (input.length < 2) {
                System.err.println("[USAGE] command server_id");
                continue;
            }
            String command = input[0];
            int sid;

            try {
                sid = Integer.parseInt(input[1]);
            } catch (NumberFormatException e) {
                System.err.println("[USAGE] command server_id");
                continue;
            }

            if (sid < 0 || sid >= DStoreSetting.SERVER.size()) {
                System.err.println("The specified server is invalid");
            }

            if (command.equalsIgnoreCase("partition")) {
                //RpcClient.testingStub(sid).setIsPartitioned(true);
            } else if (command.equalsIgnoreCase("revive")) {
                //RpcClient.testingStub(sid).setIsPartitioned(false);
            } else {
                System.err.println("Unrecognized commands");
            }
        }
    }
}
