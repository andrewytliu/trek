package cs244b.dstore.storage;

import java.lang.Exception;
import java.util.TreeMap;

public class KeyValueStore {
    private TreeMap<String, String> store;

    public KeyValueStore() {
        store = new TreeMap<String, String>();
        store.put("/", "");
    }

    public StoreResponse apply(StoreAction action) {
        return null;
    }

    public String create(String path, String data, boolean isSequential)
            throws InvalidPathException, AlreadyExistsException, NoParentException {
        path = normalizePath(path);
        if (store.containsKey(path) && !isSequential) {
            throw new AlreadyExistsException();
        }
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

    public Entry setData(String path, String data, int version)
            throws DoesNotExistException, StaleVersionException {
        return null;
    }

    public String[] getChildren(String path) throws DoesNotExistException {
        return new String[5];
    }

    public void takeSnapshot() {

    }

    public void restoreSnapshot() throws NoSnapshotException {

    }

    public static class InvalidPathException extends Exception {
    }

    public static class AlreadyExistsException extends Exception {
    }

    public static class NoParentException extends Exception {
    }

    public static class DoesNotExistException extends Exception {
    }

    public static class StaleVersionException extends Exception {
    }

    public static class NoSnapshotException extends Exception {
    }

    private static String normalizePath(String path) throws InvalidPathException {
        if (!path.matches("^/[a-zA-Z0-9_/]*")) {
            throw new InvalidPathException();
        }
        StringBuilder sb = new StringBuilder("/");
        char last = '/';
        for (int i = 1; i < path.length(); i++) {
            char cur = path.charAt(i);
            if (last == '/' && cur == '/') {
                continue;
            }
            sb.append(cur);
            last = cur;
        }
        if (sb.length() > 1 && sb.charAt(sb.length()-1) == '/') { // not "/"
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }
}