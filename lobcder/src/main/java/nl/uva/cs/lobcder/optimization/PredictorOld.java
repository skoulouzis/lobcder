/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import io.milton.common.Path;
import io.milton.http.Request;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import javax.naming.NamingException;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.MyDataSource;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class PredictorOld extends MyDataSource {

    private Timer timer;
    private GraphPopulator graphPopulator;

    public PredictorOld() throws NamingException {
    }

    public void startGraphPopulation() {
        graphPopulator = new GraphPopulator(getDatasource());
        TimerTask gcTask = new MyTask(graphPopulator);
//        
        timer = new Timer(true);
        timer.schedule(gcTask, 3600000, 3600000);
    }

//    public LobState predictNextState(LobState state) throws MalformedURLException {
//        return predictNextState(state.getMethod(), state.getResourceName());
//    }

    public Vertex predictNextState(Vertex state) {
        Graph graph = graphPopulator.getGraph();
        if (graph == null) {
            graphPopulator.run();
        }

        List<Vertex> set = graphPopulator.getGraph().vertexSet();


        // Compute the total weight of all items together
        double totalWeight = 0.0d;
        for (Vertex i : set) {
            totalWeight += graphPopulator.getGraph().getWeight(state, i);
            log.log(Level.INFO, "totalWeight: {0}", totalWeight);
        }

        // Now choose a random item
        int randomIndex = -1;
        double random = Math.random() * totalWeight;

        Vertex[] vertexArray = new Vertex[set.size()];
        vertexArray = set.toArray(vertexArray);
        for (int i = 0; i < vertexArray.length; ++i) {

            random -= graphPopulator.getGraph().getWeight(state, vertexArray[i]);
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        Vertex myRandomItem = vertexArray[randomIndex];
//        System.out.println("Will go to: " + myRandomItem + " " + node0 + "->" + myRandomItem + ": " + graphPopulator.getGraph().getWeight(node0, myRandomItem));
        return myRandomItem;
    }

    public void stopGraphPopulation() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
