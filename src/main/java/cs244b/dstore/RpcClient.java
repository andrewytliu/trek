package cs244b.dstore;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import java.net.URL;

public class RpcClient {
    public static void main(String[] args) {
        try {
            JsonRpcHttpClient client = new JsonRpcHttpClient(
                    new URL("http://localhost:7345/hello.json"));

            HelloService helloService = ProxyUtil.createClientProxy(
                    RpcClient.class.getClassLoader(),
                    HelloService.class,
                    client);

            String hello = helloService.helloworld();
            // String hello = client.invoke("helloWorld", new Object[]{}, String.class);
            System.out.println(hello);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
