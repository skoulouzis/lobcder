/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import java.util.TimerTask;

/**
 *
 * @author S. Koulouzis
 */
public class MyTask extends TimerTask {
    private Runnable task;

    public MyTask(Runnable task) {
        this.task = task;
    }

    @Override
    public void run() {
        task.run();
    }
}
