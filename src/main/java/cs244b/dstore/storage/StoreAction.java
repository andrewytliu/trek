package cs244b.dstore.storage;

import java.io.*;

public class StoreAction extends JsonSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    public static enum Type {
        CREATE,
        DELETE,
        // TODO: fill this in
    }

    private Type type;
    private String path;
    private String arg1;
    private String arg2;

    public StoreAction(Type t, String p, String o1, String o2) {
        type = t;
        path = p;
        arg1 = o1;
        arg2 = o2;
    }

    public StoreAction() {}

    public void setPresentation(String p)  throws IOException, ClassNotFoundException {
        StoreAction action = (StoreAction) parsePresentation(p);
        this.type = action.type;
        this.path = action.path;
        this.arg1 = action.arg1;
        this.arg2 = action.arg2;
    }

    public static StoreAction create(String path, String data, Boolean isSequential) {
        return new StoreAction(Type.CREATE, path, data, isSequential.toString());
    }

    // TODO: other actions
}
