package cs244b.dstore.storage;

import org.apache.commons.codec.binary.Base64;

import java.io.*;

public class JsonSerializable implements Serializable {
    public String getPresentation() throws IOException {
        ByteArrayOutputStream barray = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(barray);
        stream.writeObject(this);
        stream.flush();
        stream.close();
        return Base64.encodeBase64String(barray.toByteArray());
    }

    protected Object parsePresentation(String p) throws IOException, ClassNotFoundException {
        ByteArrayInputStream barray =
                new ByteArrayInputStream(Base64.decodeBase64(p));
        ObjectInputStream stream = new ObjectInputStream(barray);
        return stream.readObject();
    }
}
