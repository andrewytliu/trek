package cs244b.dstore.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import cs244b.dstore.api.DStoreInternal;
import cs244b.dstore.api.DStoreSetting;
import cs244b.dstore.rpc.RpcServer;
import cs244b.dstore.storage.StoreAction;

import java.util.List;

public class DStoreServer extends RpcServer {
    private DStoreInternalImpl internal;
    private DStoreServiceImpl service;
    private DStoreTestingImpl testing;
    private int replicaNumber;

    public void setup(int number) {
        replicaNumber = number;
        testing = new DStoreTestingImpl(DStoreSetting.SERVER.size(), this);
        internal = new DStoreInternalImpl(replicaNumber);
        service = new DStoreServiceImpl(internal);

        addServlet(internal, "/internal.json");
        addServlet(service, "/service.json");
        addServlet(testing, "/testing.json");
    }

    public List<StoreAction> comittedLog() {
        return internal.comittedLog();
    }

    public void kill() {
        internal.kill();
        removeServlet(DStoreInternal.class);
    }

    public void recover() {
        if (internal == null) {
            internal = new DStoreInternalImpl(replicaNumber);
            addServlet(internal, "/internal.json");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(DStoreSetting.RECOVERY_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    internal.startRecovery();
                    internal.doRecovery();
                }
            }
        }).start();
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
