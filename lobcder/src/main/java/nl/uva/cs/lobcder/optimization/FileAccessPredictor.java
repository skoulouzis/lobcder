/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.naming.NamingException;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.MyDataSource;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class FileAccessPredictor extends MyDataSource {

    private Timer timer;
    private GraphPopulator graphPopulator;

    public FileAccessPredictor() throws NamingException {
    }

    public void startGraphPopulation() {
        FileAccessPredictor.log.fine("... aaaand where we go! ");
        graphPopulator = new GraphPopulator(getDatasource());
        TimerTask gcTask = new MyTask(graphPopulator);
//        
        timer = new Timer(true);
        timer.schedule(gcTask, 3000, 3000);
    }

    public String predictNextFile(String startingNode) {
//        Vertex node0 = new Vertex(startingNode);
//        List<Vertex> set = graphPopulator.getGraph().vertexSet();
//
//
//        // Compute the total weight of all items together
//        double totalWeight = 0.0d;
//        for (Vertex i : set) {
//            totalWeight += graphPopulator.getGraph().getWeight(node0, i);
////            System.out.println("totalWeight: " + totalWeight);
//        }
//
//        // Now choose a random item
//        int randomIndex = -1;
//        double random = Math.random() * totalWeight;
//
//        Vertex[] vertexArray = new Vertex[set.size()];
//        vertexArray = set.toArray(vertexArray);
//
//        for (int i = 0; i < vertexArray.length; ++i) {
//
//            random -= graphPopulator.getGraph().getWeight(node0, vertexArray[i]);
//            if (random <= 0.0d) {
//                randomIndex = i;
//                break;
//            }
//        }
//        Vertex myRandomItem = vertexArray[randomIndex];
////        System.out.println("Will go to: " + myRandomItem + " " + node0 + "->" + myRandomItem + ": " + graphPopulator.getGraph().getWeight(node0, myRandomItem));
//        return myRandomItem.getID();
        return null;
    }

    public void stopGraphPopulation() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
