package cs244b.dstore.api;

public class DStoreSetting {
    public static int PORT = 7345;

    public static String getServer(int sid) {
        // TODO: return from the list of server
        return "http://localhost:" + PORT + "/";
    }
}
