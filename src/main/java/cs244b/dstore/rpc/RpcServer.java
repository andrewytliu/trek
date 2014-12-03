package cs244b.dstore.rpc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import cs244b.dstore.api.DStoreSetting;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;

import java.io.IOException;
import java.util.*;

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
    private ContextHandlerCollection collection;
    private Map<String, ServletContextHandler> context;

    public RpcServer() {
        server = new Server(DStoreSetting.PORT);
        context = new HashMap<>();
        collection = new ContextHandlerCollection();
        server.setHandler(collection);
    }

    protected <T> void addServlet(T serviceStub, String path) {
        ServletContextHandler handler =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath(path);
        handler.addServlet(new ServletHolder(new RpcServlet<T>(serviceStub)), "*.json");
        collection.addHandler(handler);
        context.put(path, handler);
    }

    protected void removeServlet(String path) {
        ServletContextHandler handler = context.get(path);
        if (handler == null) return;
        try {
            handler.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.destroy();
        context.put(path, null);
        collection.removeHandler(handler);
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
