/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.Vertex;
import static nl.uva.cs.lobcder.predictors.DBMapPredictor.type;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.method;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.resource;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.state;

/**
 * If the first observed successor of A is B, then B will be predicted as the
 * successor every time A is observed.
 *
 * First Stable Successor (FSS): Extension to the FS algorithm. FSS does not
 * make any predictions until N consecutive occurrences of a successor to a file
 * are observed, after which that successor is always predicted as a successor
 * to that file
 *
 * @author S. Koulouzis
 */
public class FirstSuccessor extends DBMapPredictor {

    static Integer N;

    public FirstSuccessor() throws NamingException, IOException, SQLException {
        super();
        N = PropertiesHelper.getFirstSuccessorrN();
        deleteAll();
    }

    @Override
    public void stop() {
    }

    @Override
    public Vertex getNextState(Vertex currentState) {
        try {
            String currentID = null;
            switch (type) {
                case state:
                    currentID = currentState.getID();
                    break;
                case resource:
                    currentID = currentState.getResourceName();
                    break;
                case method:
                    currentID = currentState.getMethod().code;
                    break;
                default:
                    currentID = currentState.getID();
                    break;
            }
            //        LobState nextState = fos.get(currentState.getID());
            Vertex nextState = getSuccessor(currentID);
//            LobState nextState = getSuccessor(currentState.getMethod().code);
            return nextState;
        } catch (SQLException ex) {
            Logger.getLogger(FirstSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(Vertex prevState, Vertex currentState) {
        try {
            Integer occurrences = null;
            String prevID = null;
            String currentID = null;
            switch (type) {
                case state:
                    prevID = prevState.getID();
                    currentID = currentState.getID();
                    break;
                case resource:
                    prevID = prevState.getResourceName();
                    currentID = currentState.getResourceName();
                    break;
                case method:
                    prevID = prevState.getMethod().code;
                    currentID = currentState.getMethod().code;
                    break;
                default:
                    prevID = prevState.getID();
                    currentID = currentState.getID();
                    break;
            }
            occurrences = getOoccurrences(prevID + currentID);
            //        Integer occurrences = observedMap.get(prevState.getID() + currentState.getID());
            if (occurrences == null) {
                occurrences = 1;
            } else {
                occurrences++;
            }

            if (occurrences >= N) {
                putSuccessor(prevID, currentID, false);
            }
//            if (!fos.containsKey(prevState.getID()) && occurrences >= N) {
//                fos.put(prevState.getID(), currentState);
//            }
//            observedMap.put(prevState.getID() + currentState.getID(), occurrences);
            putOoccurrences(prevID + currentID, occurrences);
        } catch (SQLException | UnknownHostException ex) {
            Logger.getLogger(FirstSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
