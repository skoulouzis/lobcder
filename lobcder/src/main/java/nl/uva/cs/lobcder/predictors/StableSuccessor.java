/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;
import static nl.uva.cs.lobcder.predictors.DBMapPredictor.type;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.method;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.resource;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.state;

/**
 * Predicts the LS which was observed at least N times in a row. If no successor
 * has been observed N times in a row, SS declines to predict
 *
 * @author S. Koulouzis
 */
public class StableSuccessor extends DBMapPredictor {

//    Map<String, LobState> lastS = new HashMap<>();
//    Map<String, Integer> observedMap = new HashMap<>();
    static Integer N;

    public StableSuccessor() throws NamingException, IOException, SQLException {
        super();
        N = PropertiesHelper.getStableSuccessorN();
        deleteAll();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        try {
            String currentID;
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

            LobState nextState = getSuccessor(currentID);
            //            if (lastS.containsKey(currentState.getID())) {
            //                nextState = lastS.get(currentState.getID());
            //            }
            Integer occurrences = null;
//            if (nextState != null && observedMap.containsKey(currentState.getID() + nextState.getID())) {
//                occurrences = observedMap.get(currentState.getID() + nextState.getID());
//            }
            if (nextState != null) {
                occurrences = getOoccurrences(currentID + nextState.getID());
            }
            if (occurrences != null && occurrences >= N) {
                return nextState;
            }
            return null;
        } catch (SQLException ex) {
            Logger.getLogger(StableSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        try {
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
//            lastS.put(prevState.getID(), currentState);
            putSuccessor(prevID, currentID, true);

//            Integer occurrences = observedMap.get(prevState.getID() + currentState.getID());
            Integer occurrences = getOoccurrences(prevState.getID() + currentState.getID());
            if (occurrences == null) {
                occurrences = 1;
            } else {
                occurrences++;
            }
//            observedMap.put(prevState.getID() + currentState.getID(), occurrences);
            putOoccurrences(prevState.getID() + currentState.getID(), occurrences);
        } catch (SQLException | UnknownHostException ex) {
            Logger.getLogger(StableSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
