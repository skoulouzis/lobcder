package nl.uva.cs.lobcder.catalogue.delsweep;

import java.util.logging.Level;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.beans.PdriBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriGroupBean;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.DeleteHelper;

/**
 * User: dvasunin Date: 25.02.13 Time: 16:31 To change this template use File |
 * Settings | File Templates.
 */
@Log
public class DeleteSweep implements Runnable {

    private final ConnectorI connector;

    public DeleteSweep(ConnectorI connector) {
        this.connector = connector;
    }

    @Override
    public void run() {
        try {
            for (PdriGroupBean pdriGroupBean : connector.getPdriGroupsToProcess()) {
                if(pdriGroupBean.getPdri() != null) {
                    for (PdriBean pdriBean : pdriGroupBean.getPdri()) {
                        DeleteHelper.delete(pdriBean);
                    }
                }
                connector.confirmPdriGroup(pdriGroupBean.getId());
            }
        } catch (Exception e) {
            DeleteSweep.log.log(Level.SEVERE, null, e);
        }
    }
}
