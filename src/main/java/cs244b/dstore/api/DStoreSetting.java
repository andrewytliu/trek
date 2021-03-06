package cs244b.dstore.api;

import java.util.ArrayList;
import java.util.List;

public class DStoreSetting {
    public static int HEARTBEAT_SOFT = 5 * 1000;
    public static int HEARTBEAT_HARD = 6 * 1000;

    public static int RECOVERY_DELAY = 3 * 1000;
    public static int CLIENT_DELAY = HEARTBEAT_HARD;

    public static int PORT = 3234;
    public static List<String> SERVER = new ArrayList<>();
    public static String MONITOR = null;

    public static void setMonitor(String monitor) {
        MONITOR = "http://" + monitor + ":" + PORT + "/";
    }

    public static void setServer(String list) {
        for (String host : list.split("\\|")) {
            SERVER.add("http://" + host + ":" + PORT + "/");
        }
    }

    public static void setHeartbeat(int base) {
        HEARTBEAT_SOFT = base;
        HEARTBEAT_HARD = (int)(base * 1.2);
    }

    public static int getF() {
        return (SERVER.size() - 1) / 2;
    }
}
