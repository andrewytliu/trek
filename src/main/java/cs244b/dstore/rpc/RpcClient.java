package cs244b.dstore.rpc;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import cs244b.dstore.api.*;
import cs244b.dstore.server.DStoreServer;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RpcClient {
    private static List<Boolean> partitioned;

    private static List<Integer> killList = new LinkedList<>();
    private static Map<Integer, List<Boolean>> partitionList = new HashMap<>();

    public synchronized static void setPartitioned(List<Boolean> values) {
        partitioned = values;
    }

    public synchronized static void setPartitioned(List<Boolean> values, int rpcCount) {
        if (rpcCount == 0) {
            setPartitioned(values);
        } else {
            partitionList.put(rpcCount, values);
        }
    }

    private synchronized static boolean isPartitioned(int sid) {
        return partitioned.get(sid);
    }

    public synchronized static void setKill(DStoreServer server, int rpcCount) {
        if (rpcCount == 0) {
            server.kill();
        } else {
            RpcClient.server = server;
            killList.add(rpcCount);
        }
    }

    public synchronized static void clear() {
        killList.clear();
        partitionList.clear();
    }

    // TODO: need to decouple server here
    private static DStoreServer server;
    private synchronized static void updateRpcCount() {
        List<Integer> updatedKillList = new LinkedList<>();
        for (int r : killList) {
            if (r == 1) {
                server.kill();
            } else {
                updatedKillList.add(--r);
            }
        }
        killList = updatedKillList;

        Map<Integer, List<Boolean>> updatedPartitionList = new HashMap<>();
        for (int r : partitionList.keySet()) {
            List<Boolean> old = partitionList.get(r);
            if (r == 1) {
                setPartitioned(old);
            } else {
                updatedPartitionList.put(--r, old);
            }
        }
        partitionList = updatedPartitionList;
    }

    private static JsonRpcHttpClient getClient(int sid, String path) {
        return getUrl(DStoreSetting.SERVER.get(sid), path);
    }

    private static JsonRpcHttpClient getUrl(String url, String path) {
        try {
            return new JsonRpcHttpClient(new URL(url + path));
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
                        try {
                            Object arguments = ReflectionUtil.parseArguments(method, args, false);

                            String methodName = method.getName();
                            JsonRpcMethod methodAnnotation = method.getAnnotation(JsonRpcMethod.class);
                            if (methodAnnotation != null && methodAnnotation.value() != null) {
                                methodName = methodAnnotation.value();
                            }

                            Object returned = client.invoke(
                                    methodName, arguments, method.getGenericReturnType(), new HashMap<String, String>());
                            updateRpcCount();
                            return returned;
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
                getClient(sid, "service/req.json"));
    }

    public static DStoreInternal internalStub(int sid) {
        if (isPartitioned(sid)) {
            return new NoopInternalStub();
        }
        DStoreInternal internal = createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreInternal.class,
                getClient(sid, "internal/req.json"));
        if (internal == null) {
            return new NoopInternalStub();
        } else {
            return internal;
        }
    }

    public static DStoreTesting testingStub(int sid) {
        return ProxyUtil.createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreTesting.class,
                getClient(sid, "testing/req.json"));
    }

    public static DStoreMonitor monitorStub() {
        return ProxyUtil.createClientProxy(
                RpcClient.class.getClassLoader(),
                DStoreMonitor.class,
                getUrl(DStoreSetting.MONITOR, "monitor/req.json"));
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
            updateRpcCount();
        }

        @Override
        public void prepareOk(int view, int op, int replica) {
            updateRpcCount();
        }

        @Override
        public void commit(int view, int commit) {
            updateRpcCount();
        }

        @Override
        public void startViewChange(int view, int replica) {
            updateRpcCount();
        }

        @Override
        public void doViewChange(int view, List<StoreAction> log, int oldView, int op, int commit, int replica) {
            updateRpcCount();
        }

        @Override
        public void startView(int view, List<StoreAction> log, int op, int commit) {
            updateRpcCount();
        }

        @Override
        public void recovery(int replica, int nonce) {
            updateRpcCount();
        }

        @Override
        public void recoveryResponse(int view, int nonce, List<StoreAction> log, int op, int commit, int replica) {
            updateRpcCount();
        }
    }
}
