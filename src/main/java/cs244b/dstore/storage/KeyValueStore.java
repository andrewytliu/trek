package cs244b.dstore.storage;

import java.lang.Exception;
import java.util.HashMap;

public class KeyValueStore {
    private HashMap<String, String> store;

    public KeyValueStore() {
        store = new HashMap<String, String>();
        store.put("/", "");
    }

    public String create(String path, String data, boolean isSequential)
            throws AlreadyExistsException, NoParentException {
        return "";
    }

    public void delete(String path, int version) throws DoesNotExistException {

    }

    public boolean exists(String path) {
        return false;
    }

    public Entry getData(String path) throws DoesNotExistException {
        return null;
    }

    public Entry setData(String path, String data, int version) throws DoesNotExistException, StaleVersionException {
        return null;
    }

    public String[] getChildren(String path) throws DoesNotExistException {
        return new String[5];
    }

    public void takeSnapshot() {

    }

    public void restoreSnapshot() throws NoSnapshotException {

    }

    public class AlreadyExistsException extends Exception {
    }

    public class NoParentException extends Exception {
    }

    public class DoesNotExistException extends Exception {
    }

    public class StaleVersionException extends Exception {
    }

    public class NoSnapshotException extends Exception {
    }
}