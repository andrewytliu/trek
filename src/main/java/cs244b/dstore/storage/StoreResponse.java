package cs244b.dstore.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;
import java.io.Serializable;

public class StoreResponse extends JsonSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        OK,
        NOT_PRIMARY,
        INVALID_CLIENT_ID,
        INVALID_REQUEST_NUM,
        INVALID_PATH,
    }

    private Status status;
    private Serializable value;

    @JsonIgnore
    public Status getStatus() {
        return status;
    }

    @JsonIgnore
    public Serializable getValue() {
        return value;
    }

    public StoreResponse(Status s, Serializable v) {
        status = s;
        value = v;
    }

    public StoreResonse() {}

    public void setPresentation(String p) throws IOException, ClassNotFoundException {
        StoreResponse resp = (StoreResponse) parsePresentation(p);
        this.status = resp.status;
        this.value = resp.value;
    }
}
