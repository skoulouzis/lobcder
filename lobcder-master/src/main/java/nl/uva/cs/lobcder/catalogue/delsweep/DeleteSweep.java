package nl.uva.cs.lobcder.catalogue.delsweep;

import java.util.logging.Level;
import java.util.logging.Logger;

import nl.uva.cs.lobcder.catalogue.beans.PdriBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriGroupBean;
import nl.uva.cs.lobcder.util.DeleteHelper;

/**
 * User: dvasunin Date: 25.02.13 Time: 16:31 To change this template use File |
 * Settings | File Templates.
 */
public class DeleteSweep implements Runnable {

    private final ConnectorI connector;
//    private int sleeTime = 10;

    public DeleteSweep(ConnectorI connector) {
        this.connector = connector;
    }

    @Override
    public void run() {
        try {
            for (PdriGroupBean pdriGroupBean : connector.getPdriGroupsToProcess()) {
                if (pdriGroupBean.getPdri() != null) {
                    for (PdriBean pdriBean : pdriGroupBean.getPdri()) {
                        DeleteHelper.delete(pdriBean);
                    }
                }
                connector.confirmPdriGroup(pdriGroupBean.getId());
            }
//            sleeTime = 10;
        } catch (Exception e) {
            Logger.getLogger(DeleteSweep.class.getName()).log(Level.SEVERE, null, e);

//            try {
//                sleeTime = sleeTime * 2;
//                Thread.sleep(sleeTime);
//            } catch (InterruptedException ex2) {
//            }
        }
    }
}
