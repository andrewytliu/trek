package cs244b.dstore.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SimFailureServer extends JsonRpcServer {
    private boolean isPartitioned;

    public SimFailureServer(Object handler, Class<?> remoteInterface) {
        super(handler, remoteInterface);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (isPartitioned) {
            throw new IOException("Dropped request due to network partition");
        } else {
            super.handle(request, response);
        }
    }

    public synchronized void setPartitioned(boolean v) {
        isPartitioned = v;
    }
}
