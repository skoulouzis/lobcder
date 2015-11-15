/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.IOException;
import java.util.TimerTask;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.ReplicateSweep;

/**
 *
 * @author S. Koulouzis
 */
@Log
class SweepersTimerTask extends TimerTask {
     private final WorkerReplicateSweep replicateSweep;
    
    public SweepersTimerTask() throws IOException{
        replicateSweep = new WorkerReplicateSweep();
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
