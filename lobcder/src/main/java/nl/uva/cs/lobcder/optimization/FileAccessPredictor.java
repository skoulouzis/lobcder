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
        timer.schedule(gcTask, 3600000, 3600000);
    }

    public LobState predictNextState(LobState state) throws MalformedURLException {
        return predictNextState(state.getMethod(), state.getResourceName());
    }

    public LobState predictNextState(Request.Method method, String startingNode) throws MalformedURLException {
        String strPath = new URL(startingNode).getPath();
        String[] parts = strPath.split("/lobcder/dav");
        String resource = null;
        if (parts != null && parts.length > 1) {
            resource = parts[1];
        }


        LobState node0 = new LobState(method, Path.path(resource).toString());

        Graph graph = graphPopulator.getGraph();
        if (graph == null) {
            graphPopulator.run();
        }

        List<LobState> set = graphPopulator.getGraph().vertexSet();


        // Compute the total weight of all items together
        double totalWeight = 0.0d;
        for (LobState i : set) {
            totalWeight += graphPopulator.getGraph().getWeight(node0, i);
            log.log(Level.INFO, "totalWeight: " + totalWeight);
        }

        // Now choose a random item
        int randomIndex = -1;
        double random = Math.random() * totalWeight;

        LobState[] vertexArray = new LobState[set.size()];
        vertexArray = set.toArray(vertexArray);
        for (int i = 0; i < vertexArray.length; ++i) {

            random -= graphPopulator.getGraph().getWeight(node0, vertexArray[i]);
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        LobState myRandomItem = vertexArray[randomIndex];
//        System.out.println("Will go to: " + myRandomItem + " " + node0 + "->" + myRandomItem + ": " + graphPopulator.getGraph().getWeight(node0, myRandomItem));
        return myRandomItem;
    }

    public void stopGraphPopulation() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
