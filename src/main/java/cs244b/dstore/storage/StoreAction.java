package cs244b.dstore.storage;

import java.io.Serializable;

public class StoreAction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        CREATE,
        DELETE,
        // TODO: fill this in
    }

    private Type type;
    private String path;
    private Serializable arg1;
    private Serializable arg2;

    private StoreAction(Type t, String p, Serializable o1, Serializable o2) {
        type = t;
        path = p;
        arg1 = o1;
        arg2 = o2;
    }

    public static StoreAction create(String path, String data, boolean isSequential) {
        return new StoreAction(Type.CREATE, path, data, isSequential);
    }

    // TODO: other actions
}
