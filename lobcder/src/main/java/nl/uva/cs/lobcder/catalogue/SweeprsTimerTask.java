/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
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
            wp4Sweep = new WP4Sweep(datasource);
        }
    }

    @Override
    public void run() {
        try {
            deleteSweep.run();
            replicateSweep.run();
            if (wp4Sweep != null) {
                wp4Sweep.run();
            }
        } catch (Throwable th) {
            log.log(Level.SEVERE, "One of the sweepers encountered and error.", th);
        }
    }

    @Override
    public boolean cancel() {
        boolean res = super.cancel();
//        replicateSweep.stop();
        return res;

    }
}
