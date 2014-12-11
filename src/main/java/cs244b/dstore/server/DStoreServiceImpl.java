package cs244b.dstore.server;

import cs244b.dstore.api.DStoreService;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcClient;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DStoreServiceImpl implements DStoreService {
    private static final Logger logger = Logger.getLogger("cs244b.VR");
    private DStoreInternalImpl internal;
    private int clientId = 0;
    private Map<Integer, Integer> actionMap;
    private Map<Integer, StoreResponse> responseMap;

    public DStoreServiceImpl(DStoreInternalImpl impl) {
        internal = impl;
        actionMap = new HashMap<>();
        responseMap = new HashMap<>();
    }

    private void log(String str) {
        if (DStoreSetting.MONITOR == null) {
            logger.log(Level.INFO, str);
        } else {
            try {
                RpcClient.monitorStub().log(internal.getReplicaNumber(), "C: " + str);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not log to monitor");
                logger.log(Level.INFO, str);
            }
        }
    }

    @Override
    public synchronized int id() {
        // TODO: clean up for client leaving?
        int result = clientId++;
        actionMap.put(result, -1);
        log("id() => " + result);
        return result;
    }

    @Override
    public int primary() {
        log("primary() => " + internal.getPrimary());
        return internal.getPrimary();
    }

    @Override
    public StoreResponse request(StoreAction action, int client, int request) {
        log("request(action, " + client + ", " + request + ")");
        // Check if it is primary
        if (!internal.isPrimary()) {
            return new StoreResponse(StoreResponse.Status.NOT_PRIMARY, internal.getPrimary());
        }
        // Check for client id
        if (!actionMap.containsKey(client)) {
            return new StoreResponse(StoreResponse.Status.INVALID_CLIENT_ID, null);
        }
        int prevRequest = actionMap.get(client);
        // Replay the previous request
        if (prevRequest == request) {
            return responseMap.get(client);
        }
        // Drop request with old request number
        if (prevRequest > request) {
            return new StoreResponse(StoreResponse.Status.INVALID_REQUEST_NUM, null);
        }
        // Sending prepare
        int op = internal.startTransactionPrimary(action);
        // Collecting prepare ok (or abort)
        StoreResponse response = internal.doCommitPrimary(op);
        responseMap.put(client, response);
        actionMap.put(client, request);
        // TODO: reply view number?
        return response;
    }
}
