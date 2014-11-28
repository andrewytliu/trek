package cs244b.dstore.rpc;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import cs244b.dstore.api.DStoreService;
import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.api.DStoreTesting;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class RpcClient {
    private static List<Boolean> partitioned;

    public synchronized static void setPartitioned(List<Boolean> values) {
        partitioned = values;
    }

    private synchronized static boolean isPartitioned(int sid) {
        return partitioned.get(sid);
    }

    private static JsonRpcHttpClient getClient(int sid, String path) {
        try {
            return new JsonRpcHttpClient(new URL(DStoreSetting.SERVER.get(sid) + path));
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
        if (isPartitioned(sid)) {
            return new PartitionedServiceStub();
        }
        return ProxyUtil.createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreService.class,
                getClient(sid, "service.json"));
    }

    public static DStoreInternal internalStub(int sid) {
        if (isPartitioned(sid)) {
            return new NoopInternalStub();
        }
        DStoreInternal internal = createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreInternal.class,
                getClient(sid, "internal.json"));
        if (internal == null) {
            return new NoopInternalStub(); //TODO: Why is this needed?
        } else {
            return internal;
        }
    }

    public static DStoreTesting testingStub(int sid) {
        return ProxyUtil.createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreTesting.class,
                getClient(sid, "testing.json"));
    }

    private static class PartitionedServiceStub implements DStoreService {
        @Override
        public int id() throws ServiceTimeoutException {
            throw new ServiceTimeoutException();
        }

        @Override
        public int primary() throws ServiceTimeoutException {
            throw new ServiceTimeoutException();
        }

        @Override
        public StoreResponse request(StoreAction action, int client, int request)
                throws ServiceTimeoutException {
            throw new ServiceTimeoutException();
        }
    }

    private static class NoopInternalStub implements DStoreInternal {
        @Override
        public void prepare(int view, StoreAction action, int op, int commit) {
        }

        @Override
        public void prepareOk(int view, int op, int replica) {
        }

        @Override
        public void commit(int view, int commit) {
        }

        @Override
        public void startViewChange(int view, int replica) {
        }

        @Override
        public void doViewChange(int view, List<StoreAction> log, int oldView, int op, int commit, int replica) {
        }

        @Override
        public void startView(int view, List<StoreAction> log, int op, int commit) {
        }

        @Override
        public void recovery(int replica, int nonce) {
        }

        @Override
        public void recoveryResponse(int view, int nonce, List<StoreAction> log, int op, int commit, int replica) {
        }
    }
}
