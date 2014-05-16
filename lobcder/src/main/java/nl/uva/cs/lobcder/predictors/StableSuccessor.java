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
import nl.uva.cs.lobcder.util.PropertiesHelper;

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
            LobState nextState = getSuccessor(currentState.getID());
            //            if (lastS.containsKey(currentState.getID())) {
            //                nextState = lastS.get(currentState.getID());
            //            }
            Integer occurrences = null;
//            if (nextState != null && observedMap.containsKey(currentState.getID() + nextState.getID())) {
//                occurrences = observedMap.get(currentState.getID() + nextState.getID());
//            }
            if (nextState != null) {
                occurrences = getOoccurrences(currentState.getID() + nextState.getID());
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
//            lastS.put(prevState.getID(), currentState);
            putSuccessor(prevState.getID(), currentState, true);

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
