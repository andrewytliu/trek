package cs244b.dstore.storage;

public class Entry {
    public String value;
    public int version;

    public Entry (String value) {
        this.value = value;
        this.version = 0;
    }

    public Entry (Entry e) {
        this.value = e.value;
        this.version = e.version;
    }
}