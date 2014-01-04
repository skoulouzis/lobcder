/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import io.milton.common.Path;
import io.milton.http.Request;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
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
        graphPopulator = new GraphPopulator(getDatasource());
        TimerTask gcTask = new MyTask(graphPopulator);
//        
        timer = new Timer(true);
        timer.schedule(gcTask, 3600000, 3600000);
    }

    public LobState predictNextState(LobState state) throws SQLException {
        graphPopulator.run();
        return graphPopulator.getNextState(state);
    }

    public void stopGraphPopulation() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
