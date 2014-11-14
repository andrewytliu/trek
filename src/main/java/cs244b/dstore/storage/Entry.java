package cs244b.dstore.storage;

import java.io.Serializable;

public class Entry implements Serializable {
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
