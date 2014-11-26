package cs244b.dstore.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.HttpException;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;

public class SimFailureClient extends JsonRpcHttpClient {
    private boolean isPartitioned;

    public SimFailureClient(URL serviceUrl) {
        super(serviceUrl);
        isPartitioned = false;
    }

    @Override
    public Object invoke(
            String methodName, Object argument, Type returnType,
            Map<String, String> extraHeaders)
            throws Throwable {
        if (isPartitioned) {
            throw new HttpException("Timeout due to network partition", new IOException());
        } else {
            return super.invoke(methodName, argument, returnType, extraHeaders);
        }
    }

    public synchronized void setPartitioned(boolean v) {
        isPartitioned = v;
    }
}
