/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import java.io.IOException;
import java.util.TimerTask;
import javax.sql.DataSource;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 *
 * @author S. Koulouzis
 */
class SweeprsTimerTask extends TimerTask {

    private final DeleteSweep deleteSweep;
    private final ReplicateSweep replicateSweep;
//    private final TokensDeleteSweep tokensDeleteSweep;
    private final Boolean useRepo;
    private WP4Sweep wp4Sweep = null;

    SweeprsTimerTask(DataSource datasource) throws IOException {
        deleteSweep = new DeleteSweep(datasource);
        replicateSweep = new ReplicateSweep(datasource);
//        tokensDeleteSweep = new TokensDeleteSweep(datasource);

        useRepo = PropertiesHelper.useMetadataReposetory();
        if (useRepo) {
            String metadataReposetory = PropertiesHelper.getMetadataReposetoryURL();
            wp4Sweep = new WP4Sweep(datasource,
                    new WP4Sweep.WP4Connector(metadataReposetory));
        }

    }

    @Override
    public void run() {
        deleteSweep.run();
        replicateSweep.run();
        if (wp4Sweep != null) {
            wp4Sweep.run();
        }
//        tokensDeleteSweep.run();
    }
}
