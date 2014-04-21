/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.TimerTask;

/**
 *
 * @author S. Koulouzis
 */
class SweeprsTimerTask extends TimerTask {

    private final DeleteSweep deleteSweep;
    private final ReplicateSweep replicateSweep;
    private final Boolean useRepo;
    private WP4Sweep wp4Sweep = null;

    SweeprsTimerTask(DataSource datasource) throws IOException {
        deleteSweep = new DeleteSweep(datasource);
        replicateSweep = new ReplicateSweep(datasource);
        useRepo = PropertiesHelper.useMetadataRepository();
        if (useRepo) {
            String metadataRepository = PropertiesHelper.getMetadataRepositoryURL();
            String metadataRepositoryDev = PropertiesHelper.getMetadataRepositoryDevURL();
            wp4Sweep = new WP4Sweep(datasource,
                    new WP4Sweep.WP4Connector(metadataRepository, metadataRepositoryDev));
        }

    }

    @Override
    public void run() {
        deleteSweep.run();
        replicateSweep.run();
        if (wp4Sweep != null) {
            wp4Sweep.run();
        }
    }

    @Override
    public boolean cancel() {
        boolean res = super.cancel();
//        replicateSweep.stop();
        return res;

    }
}
