package cs244b.dstore.rpc;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import cs244b.dstore.api.DStoreService;
import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RpcClient {
    private static JsonRpcHttpClient getClient(int sid, String path) {
        try {
            return new JsonRpcHttpClient(
                    new URL(DStoreSetting.SERVER.get(sid) + path));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T createClientProxy(
            ClassLoader classLoader,
            Class<T> proxyInterface,
            final JsonRpcHttpClient client) {

        // create and return the proxy
        return (T) Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{proxyInterface},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) {
//                        if (method.getDeclaringClass() == Object.class) {
//                            return proxyObjectMethods(method, proxy, args);
//                        }
                        try {
                            Object arguments = ReflectionUtil.parseArguments(method, args, false);

                            String methodName = method.getName();
                            JsonRpcMethod methodAnnotation = method.getAnnotation(JsonRpcMethod.class);
                            if (methodAnnotation != null && methodAnnotation.value() != null) {
                                methodName = methodAnnotation.value();
                            }

                            return client.invoke(
                                    methodName, arguments, method.getGenericReturnType(), new HashMap<String, String>());
                        } catch (Throwable t) {
                            return null;
                        }
                    }
                });
    }

    public static DStoreService serviceStub(int sid) {
        return ProxyUtil.createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreService.class,
                getClient(sid, "service.json"));
    }

    public static DStoreInternal internalStub(int sid) {
        return createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreInternal.class,
                getClient(sid, "internal.json"));
    }
}
