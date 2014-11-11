package cs244b.dstore;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;

public class RpcServer extends HttpServlet {

    public class HelloServiceImpl implements HelloService {
        public String helloworld() {
            System.out.println("Called!!!");
            return "Hi everyone!";
        }
    }

    private HelloService helloService;
    private JsonRpcServer jsonRpcServer;

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            jsonRpcServer.handle(req, resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(ServletConfig config) {
        this.helloService = new HelloServiceImpl();
        this.jsonRpcServer = new JsonRpcServer(this.helloService, HelloService.class);
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(7345);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new RpcServer()), "/hello.json");

        server.start();
        server.join();
    }
}
