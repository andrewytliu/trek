package cs244b.dstore.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcServer;
import cs244b.dstore.storage.StoreAction;

import java.util.ArrayList;
import java.util.List;

public class DStoreServer extends RpcServer {
    private DStoreInternalImpl internal;
    private DStoreServiceImpl service;
    private DStoreTestingImpl testing;
    private int replicaNumber;

    public void setup(int number) {
        replicaNumber = number;
        testing = new DStoreTestingImpl(DStoreSetting.SERVER.size(), this, replicaNumber);
        internal = new DStoreInternalImpl(replicaNumber);
        service = new DStoreServiceImpl(internal);

        addServlet(internal, "/internal");
        addServlet(service, "/service");
        addServlet(testing, "/testing");
    }

    public List<StoreAction> getCommitLog() {
        if (internal != null) {
            return internal.getCommitLog();
        } else {
            return new ArrayList<>();
        }
    }

    public void kill() {
        if (internal == null || service == null) return;
        internal.kill();
        removeServlet("/internal");
        removeServlet("/service");
        internal = null;
        service = null;
    }

    public void restart() {
        if (internal == null) {
            internal = new DStoreInternalImpl(replicaNumber);
            service = new DStoreServiceImpl(internal);
            addServlet(internal, "/internal");
            addServlet(service, "/service");
        }
    }

    public void recover() {
        restart();
        internal.recover();
    }

    public boolean isAlive() {
        return internal != null;
    }

    private static class DStoreServerParameter {
        @Parameter(names = "-server", description = "Server address", required = true)
        private String serverList;
        @Parameter(names = "-index", description = "Index of the server", required = true)
        private Integer serverIndex;
        @Parameter(names = "-recovery", description = "Recovery mode")
        private boolean recovery = false;
        @Parameter(names = "-monitor", description = "Monitor address")
        private String monitor;
        @Parameter(names = "-help", help = true)
        private boolean help;
    }

    public static void main(String[] args) {
        DStoreServerParameter param = new DStoreServerParameter();
        new JCommander(param, args);

        if (param.monitor != null) {
            DStoreSetting.setMonitor(param.monitor);
        }
        DStoreSetting.setServer(param.serverList);
        DStoreServer server = new DStoreServer();
        server.setup(param.serverIndex);
        if (param.recovery) {
            server.recover();
        }
        server.start();
    }
}
