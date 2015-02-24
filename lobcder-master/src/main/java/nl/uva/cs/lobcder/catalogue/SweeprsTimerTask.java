/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;

/**
 *
 * @author S. Koulouzis
 */
@Log
class SweeprsTimerTask extends TimerTask {

    private final DeleteSweep deleteSweep;
    private final ReplicateSweep replicateSweep;
    private final Boolean useRepo;
    private final Boolean useSDN;
    private WP4SweepOLD wp4Sweep = null;
    private SDNSweep sdnSweep = null;

    SweeprsTimerTask(DataSource datasource) throws IOException, NamingException, ClassNotFoundException {
        deleteSweep = new DeleteSweep(datasource);
        replicateSweep = new ReplicateSweep(datasource);
        useRepo = PropertiesHelper.useMetadataRepository();
        useSDN = PropertiesHelper.useSDN();
        if (useRepo) {
            wp4Sweep = new WP4SweepOLD(datasource);

        }
        if (useSDN) {
            sdnSweep = new SDNSweep(datasource);
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
            if (sdnSweep != null) {
                sdnSweep.run();
            }

        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "One of the sweepers encountered and error.", e);
            return; // Keep working
        } catch (Throwable e) {
            log.log(Level.SEVERE, "One of the sweepers encountered and error.", e);
            return; // Keep working
        }

    }

    @Override
    public boolean cancel() {
        boolean res = super.cancel();
//        replicateSweep.stop();
        return res;

    }
}
