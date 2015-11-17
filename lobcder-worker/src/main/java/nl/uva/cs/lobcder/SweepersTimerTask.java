/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.IOException;
import java.util.TimerTask;
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
class SweepersTimerTask extends TimerTask {
     private final WorkerReplicateSweep replicateSweep;
    
    public SweepersTimerTask(Catalogue cat) throws IOException{
        replicateSweep = new WorkerReplicateSweep(cat);
    }

    @Override
    public void run() {
        replicateSweep.run();
    }
    
}
