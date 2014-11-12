package cs244b.dstore.server;

import cs244b.dstore.api.DStoreService;
import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

import java.util.HashMap;
import java.util.Map;

public class DStoreServiceImpl implements DStoreService {
    private DStoreInternalImpl internal;
    private int clientId = 0;
    private Map<Integer, Integer> actionMap;
    private Map<Integer, StoreResponse> responseMap;

    public DStoreServiceImpl(DStoreInternalImpl impl) {
        internal = impl;
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
    public StoreResponse request(StoreAction action, int client, int request) {
        if (!actionMap.containsKey(client)) {
            return new StoreResponse(StoreResponse.Status.INVALID_CLIENT_ID, null);
        }
        int prevRequest = actionMap.get(client);
        if (prevRequest == request) {
            return responseMap.get(client);
        }
        if (prevRequest > request) {
            return new StoreResponse(StoreResponse.Status.INVALID_REQUEST_NUM, null);
        }
        int op = internal.proceedClient(action);
        StoreResponse response = internal.doCommit(op);
        responseMap.put(client, response);
        return response;
    }
}
