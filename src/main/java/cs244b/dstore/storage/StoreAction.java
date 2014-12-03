package cs244b.dstore.storage;

import java.io.*;
import cs244b.dstore.storage.StoreResponse.Status;

public class StoreAction extends JsonSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    public static enum Type {
        CREATE,
        DELETE,
        EXISTS,
        GET_DATA,
        SET_DATA,
        GET_CHILDREN,
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

    @Override
    public boolean equals(Object other) {
        if (other instanceof StoreAction) return false;
        StoreAction act = (StoreAction) other;
        return type.equals(act.type) && path.equals(act.path) && arg1.equals(act.arg1) && arg2.equals(act.arg2);
    }

    public void setPresentation(String p)  throws IOException, ClassNotFoundException {
        StoreAction action = (StoreAction) parsePresentation(p);
        this.type = action.type;
        this.path = action.path;
        this.arg1 = action.arg1;
        this.arg2 = action.arg2;
    }

    public StoreResponse apply(KeyValueStore store) {
        Status status = Status.OK;
        Serializable value = null;
        switch (type) {
            case CREATE:
                try {
                    value = store.create(path, arg1, Boolean.valueOf(arg2));
                } catch (IllegalArgumentException e) {
                    status = Status.INVALID_PATH;
                } catch (KeyValueStore.NodeExistsException e) {
                    status = Status.NODE_EXISTS;
                } catch (KeyValueStore.NoNodeException e) {
                    status = Status.NO_NODE;
                }
                break;
            case DELETE:
                try {
                    store.delete(path, Integer.valueOf(arg1));
                } catch (IllegalArgumentException e) {
                    status = Status.INVALID_PATH;
                } catch (KeyValueStore.NoNodeException e) {
                    status = Status.NO_NODE;
                } catch (KeyValueStore.BadVersionException e) {
                    status = Status.BAD_VERSION;
                }
                break;
            case EXISTS:
                try {
                    value = Boolean.toString(store.exists(path));
                } catch (IllegalArgumentException e) {
                    status = Status.INVALID_PATH;
                }
                break;
            case GET_DATA:
                try {
                    value = store.getData(path);
                } catch (IllegalArgumentException e) {
                    status = Status.INVALID_PATH;
                } catch (KeyValueStore.NoNodeException e) {
                    status = Status.NO_NODE;
                }
                break;
            case SET_DATA:
                try {
                    value = store.setData(path, arg1, Integer.valueOf(arg2));
                } catch (IllegalArgumentException e) {
                    status = Status.INVALID_PATH;
                } catch (KeyValueStore.NoNodeException e) {
                    status = Status.NO_NODE;
                } catch (KeyValueStore.BadVersionException e) {
                    status = Status.BAD_VERSION;
                }
                break;
            case GET_CHILDREN:
                try {
                    value = store.getChildren(path);
                } catch (IllegalArgumentException e) {
                    status = Status.INVALID_PATH;
                } catch (KeyValueStore.NoNodeException e) {
                    status = Status.NO_NODE;
                }
                break;
        }
        return new StoreResponse(status, value);
    }

    public static StoreAction create(String path, String data, Boolean isSequential) {
        return new StoreAction(Type.CREATE, path, data, isSequential.toString());
    }

    public static StoreAction delete(String path, Integer version) {
        return new StoreAction(Type.DELETE, path, version.toString(), null);
    }

    public static StoreAction exists(String path) {
        return new StoreAction(Type.EXISTS, path, null, null);
    }

    public static StoreAction getData(String path) {
        return new StoreAction(Type.GET_DATA, path, null, null);
    }

    public static StoreAction setData(String path, String data, Integer version) {
        return new StoreAction(Type.SET_DATA, path, data, version.toString());
    }

    public static StoreAction getChildren(String path) {
        return new StoreAction(Type.GET_CHILDREN, path, null, null);
    }

    public String toString() {
        String res = "Type: " + type + " Path: " + path;
        if (arg1 != null) {
            res += " arg1: " + arg1;
        }
        if (arg2 != null) {
            res += " arg2: " + arg2;
        }
        return res;
    }
}
