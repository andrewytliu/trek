package cs244b.dstore.api;

import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

public interface DStoreService {
    public int id();
    public StoreResponse request(StoreAction action, int client, int request);
}
