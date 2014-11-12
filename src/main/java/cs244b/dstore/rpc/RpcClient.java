package cs244b.dstore.rpc;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import cs244b.dstore.api.DStoreClientAPI;
import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;

import java.net.MalformedURLException;
import java.net.URL;

public class RpcClient {
    private static JsonRpcHttpClient getClient(int sid, String path) {
        try {
            JsonRpcHttpClient client = new JsonRpcHttpClient(
                    new URL(DStoreSetting.getServer(sid) + path));
            return client;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static DStoreClientAPI clientApiStub(int sid) {
        return ProxyUtil.createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreClientAPI.class,
                getClient(sid, "client.json"));
    }

    public static DStoreInternal internalApi(int sid) {
        return ProxyUtil.createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreInternal.class,
                getClient(sid, "internal.json"));
    }
}
