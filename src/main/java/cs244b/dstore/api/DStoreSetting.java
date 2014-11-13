package cs244b.dstore.api;

public class DStoreSetting {
    public static int HEARTBEAT_SOFT = 5;
    public static int HEARTBEAT_HARD = 6;
    public static int PORT = 7345;

    public static String getServer(int sid) {
        // TODO: return from the list of server
        return "http://localhost:" + PORT + "/";
    }

    public static int getServerNum() {
        return 1;
    }

    public static int getF() {
        return (getServerNum() - 1) / 2;
    }
}
