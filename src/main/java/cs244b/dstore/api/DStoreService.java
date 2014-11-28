package cs244b.dstore.api;

import cs244b.dstore.storage.StoreAction;
import cs244b.dstore.storage.StoreResponse;

public interface DStoreService {
    public int id() throws ServiceTimeoutException;
    public int primary() throws ServiceTimeoutException;
    public StoreResponse request(StoreAction action, int client, int request)
            throws ServiceTimeoutException;

    public static class ServiceTimeoutException extends Exception {}
}
