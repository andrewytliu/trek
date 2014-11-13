package cs244b.dstore.storage;

import java.lang.Exception;
import java.util.*;

public class KeyValueStore {
    private TreeMap<String, Entry> store;

    public KeyValueStore() {
        store = new TreeMap<String, Entry>();
    }

    public StoreResponse apply(StoreAction action) {
        return null;
    }

    public String create(String path, String data, boolean isSequential)
            throws NodeExistsException, NoNodeException {
        path = normalizePath(path);
        if (path.equals("/")) {
            throw new IllegalArgumentException();
        }
        if (!isSequential && store.containsKey(path)) {
            throw new NodeExistsException();
        }
        String parent = path.substring(0, path.lastIndexOf('/'));
        if (!parent.equals("") && !store.containsKey(parent)) {
            throw new NoNodeException();
        }
        Entry dataEntry = new Entry(data);
        //TODO: Handle sequential
        store.put(path, new Entry(dataEntry));
        return path;
    }

    public void delete(String path, int version)
            throws NoNodeException, BadVersionException {
        path = normalizePath(path);
        Entry dataEntry = store.get(path);
        if (dataEntry == null) {
            throw new NoNodeException();
        }
        if (dataEntry.version != version) {
            throw new BadVersionException();
        }
        store.remove(path);
    }

    public boolean exists(String path) {
        path = normalizePath(path);
        return store.containsKey(path);
    }

    public Entry getData(String path) throws NoNodeException {
        path = normalizePath(path);
        Entry dataEntry = store.get(path);
        if (dataEntry == null) {
            throw new NoNodeException();
        }
        return new Entry(dataEntry);
    }

    public Entry setData(String path, String data, int version)
            throws NoNodeException, BadVersionException {
        path = normalizePath(path);
        Entry dataEntry = store.get(path);
        if (dataEntry == null) {
            throw new NoNodeException();
        }
        if (dataEntry.version != version) {
            throw new BadVersionException();
        }
        dataEntry.value = data;
        dataEntry.version++;
        return new Entry(dataEntry);
    }

    public List<String> getChildren(String path) throws NoNodeException {
        path = normalizePath(path);
        Set<String> childPaths;
        if (path.equals("/")) {
            childPaths = store.subMap("/A", "/{").keySet();
        } else {
            childPaths = store.subMap(path + "/A", path + "/{").keySet();
        }
        ArrayList<String> results = new ArrayList<>();
        for (String childPath : childPaths) {
            results.add(childPath.substring(childPath.lastIndexOf('/')+1));
        }
        return results;
    }

    public void takeSnapshot() {

    }

    public void restoreSnapshot() throws NoSnapshotException {

    }

    public static class NodeExistsException extends Exception {
    }

    public static class NoNodeException extends Exception {
    }

    public static class BadVersionException extends Exception {
    }

    public static class NoSnapshotException extends Exception {
    }

    private static String normalizePath(String path) {
        if (path == null || !path.matches("^/[a-zA-Z0-9_/]*")) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder("/");
        char last = '/';
        for (int i = 1; i < path.length(); i++) {
            char cur = path.charAt(i);
            if (last == '/' && cur == '/') {
                continue;
            }
            if (last == '/' && cur >= '0' && cur <= '9') {
                throw new IllegalArgumentException();
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
