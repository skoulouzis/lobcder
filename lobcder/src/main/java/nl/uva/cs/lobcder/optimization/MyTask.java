/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import java.util.TimerTask;

/**
 *
 * @author alogo
 */
class MyTask extends TimerTask {
    private Runnable graphPopulator;

    MyTask(Runnable graphPopulator) {
        this.graphPopulator = graphPopulator;
    }

    @Override
    public void run() {
        graphPopulator.run();
    }
}
