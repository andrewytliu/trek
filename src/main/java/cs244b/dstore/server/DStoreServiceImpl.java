package cs244b.dstore.server;

import cs244b.dstore.api.DStoreService;
import cs244b.dstore.api.DStoreTesting;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.util.HashMap;
import java.util.Map;

public class DStoreServiceImpl implements DStoreService {
    private DStoreInternalImpl internal;
    private DStoreTestingImpl testing;
    private int clientId = 0;
    private Map<Integer, Integer> actionMap;
    private Map<Integer, StoreResponse> responseMap;

    public DStoreServiceImpl(DStoreInternalImpl impl, DStoreTestingImpl test) {
        internal = impl;
        testing = test;
        actionMap = new HashMap<>();
        responseMap = new HashMap<>();
    }

    @Override
    public synchronized int id() {
        // TODO: clean up for client leaving?
        int result = clientId++;
        actionMap.put(result, -1);
        return result;
    }

    @Override
    public int primary() {
        return internal.getPrimary();
    }

    @Override
    public StoreResponse request(StoreAction action, int client, int request) {
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
