package cs244b.dstore.storage;

import java.io.Serializable;

public class StoreResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        OK,
        NOT_PRIMARY,
        INVALID_CLIENT_ID,
        INVALID_REQUEST_NUM,
        INVALID_PATH,
    }

    public Status getStatus() {
        return status;
    }

    private Status status;

    public Serializable getValue() {
        return value;
    }

    private Serializable value;

    public StoreResponse(Status s, Serializable v) {
        status = s;
        value = v;
    }
}
