package cs244b.dstore.rpc;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import cs244b.dstore.api.DStoreSetting;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;

public class RpcServer extends HttpServlet {

    private class RpcServlet<T> extends HttpServlet {
        private T serviceStub;
        private JsonRpcServer jsonRpcServer;

        public RpcServlet(T service) {
            this.serviceStub = service;
        }

        protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
            try {
                jsonRpcServer.handle(req, resp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void init(ServletConfig config) {
            this.jsonRpcServer = new JsonRpcServer(this.serviceStub, serviceStub.getClass());
            this.jsonRpcServer.setRethrowExceptions(true);
        }
    }

    private Server server;
    private ServletContextHandler context;

    public RpcServer() {
        server = new Server(DStoreSetting.PORT);
        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
   }

    protected <T> void addServlet (T serviceStub, String path) {
        context.addServlet(new ServletHolder(new RpcServlet<T>(serviceStub)), path);
    }

    public void start() {
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
