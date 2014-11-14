package cs244b.dstore.storage;

import java.io.*;
import java.lang.Exception;
import java.util.*;

public class KeyValueStore {
    private TreeMap<String, Entry> store;
    private String snapshotPath;

    public KeyValueStore() {
        store = new TreeMap<String, Entry>();
        snapshotPath = System.getProperty("user.home") + "/.dstore/snapshot.ser";
    }

    public StoreResponse apply(StoreAction action) {
        // TODO: actually apply action, replying dummy object for now
        return new StoreResponse(StoreResponse.Status.OK, "Done");
    }

    public String create(String path, String data, boolean isSequential)
            throws NodeExistsException, NoNodeException {
        path = normalizePath(path);
        if (path.equals("/") || path.lastIndexOf(':') > path.lastIndexOf('/')) {
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
        if(isSequential) {
            int seqValue = getNextSeqValue(path);
            path = path + ":" + seqValue;
        }
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
        if (version >= 0 && dataEntry.version != version) {
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
        if (version >= 0 && dataEntry.version != version) {
            throw new BadVersionException();
        }
        dataEntry.value = data;
        dataEntry.version++;
        return new Entry(dataEntry);
    }

    public List<String> getChildren(String path) throws NoNodeException {
        path = normalizePath(path);
        Set<String> range;
        int offset;
        if (path.equals("/")) {
            range = store.subMap("/", "/{").keySet();
            offset = 1;
        } else {
            range = store.subMap(path + "/", path + "/{").keySet();
            offset = path.length() + 1;
        }

        ArrayList<String> results = new ArrayList<>();
        for (String key : range) {
            String name = key.substring(offset);
            if (!name.contains("/")) {
                results.add(name);
            }
        }
        return results;
    }

    public void takeSnapshot() {
        try {
            File dir = new File(System.getProperty("user.home") + "/.dstore");
            dir.mkdir();
            FileOutputStream fileStream = new FileOutputStream(snapshotPath);
            ObjectOutputStream out = new ObjectOutputStream(fileStream);
            out.writeObject(store);
            out.close();
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean restoreSnapshot() {
        try {
            File f = new File(snapshotPath);
            if (!f.isFile() || !f.canRead()) {
                return false;
            }
            FileInputStream fileStream = new FileInputStream(f);
            ObjectInputStream in = new ObjectInputStream(fileStream);
            store = (TreeMap<String, Entry>)in.readObject();
            in.close();
            fileStream.close();
            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return false;
    }

    public static class NodeExistsException extends Exception {
    }

    public static class NoNodeException extends Exception {
    }

    public static class BadVersionException extends Exception {
    }

    private static String normalizePath(String path) {
        if (path == null || !path.matches("^/[a-zA-Z0-9:_/]*")) {
            throw new IllegalArgumentException();
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

    private int getNextSeqValue(String path) {
        Set<String> range = store.subMap(path + ":", path + ";").keySet();
        int offset = path.length() + 1;
        int max = -1;
        for (String key : range) {
            int seqValue = Integer.parseInt(key.substring(offset));
            max = (seqValue > max) ? seqValue : max;
        }
        return max+1;
    }

    public static void main (String[] args) throws NoNodeException, NodeExistsException, BadVersionException {
        KeyValueStore s = new KeyValueStore();
        try {
            s.create("/a:1", "foo", false);
        } catch (IllegalArgumentException e) {
            System.out.println("OK");
        }
        s.create("/a", "foo", true);
        s.create("/a", "bar", true);
        s.setData("/a:1", "baz", -1);
        s.takeSnapshot();
        KeyValueStore t = new KeyValueStore();
        t.restoreSnapshot();
        List<String> children = t.getChildren("/");
        for (String c : children) {
            System.out.print(c + " ");
        }
        System.out.println();
        System.out.println(t.getData("/a:1").value + " " + t.getData("/a:1").version);
    }
}
