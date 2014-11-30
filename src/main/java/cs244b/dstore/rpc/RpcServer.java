package cs244b.dstore.rpc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import cs244b.dstore.api.DStoreSetting;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    protected <T> void addServlet(T serviceStub, String path) {
        context.addServlet(new ServletHolder(new RpcServlet<T>(serviceStub)), path);
    }

    protected void removeServlet(Class klass) {
        ServletHandler handler = context.getServletHandler();
        Set<String> names = new HashSet<>();
        List<ServletHolder> servlets = new ArrayList<>();
        List<ServletMapping> mappings = new ArrayList<>();

        for (ServletHolder holder : handler.getServlets()) {
            try {
                if (klass.isInstance(holder.getServlet())) {
                    names.add(holder.getName());
                } else {
                    servlets.add(holder);
                }
            } catch (ServletException e) {
                e.printStackTrace();
            }
        }

        for (ServletMapping mapping : handler.getServletMappings()) {
            if (!names.contains(mapping.getServletName())) {
                mappings.add(mapping);
            }
        }

        handler.setServletMappings(mappings.toArray(new ServletMapping[0]));
        handler.setServlets(servlets.toArray(new ServletHolder[0]));
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
