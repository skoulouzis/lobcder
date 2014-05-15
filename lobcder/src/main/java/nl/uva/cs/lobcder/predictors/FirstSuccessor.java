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
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.PropertiesHelper;

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

//    Map<String, LobState> fos = new HashMap<>();
//    Map<String, Integer> observedMap = new HashMap<>();
    static Integer N;

    public FirstSuccessor() throws NamingException, IOException, SQLException {
        N = PropertiesHelper.getFirstSuccessorrN();
        
        deleteAll();
    }

    @Override
    public void stop() {
    }

    @Override
    public LobState getNextState(LobState currentState) {
        try {
            //        LobState nextState = fos.get(currentState.getID());
            LobState nextState = getSuccessor(currentState.getID());
            return nextState;
        } catch (SQLException ex) {
            Logger.getLogger(FirstSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        try {
            Integer occurrences = getOoccurrences(prevState.getID() + currentState.getID());
            //        Integer occurrences = observedMap.get(prevState.getID() + currentState.getID());
            if (occurrences == null) {
                occurrences = 1;
            } else {
                occurrences++;
            }

            if (occurrences >= N) {
                putSuccessor(prevState.getID(), currentState, false);
            }
//            if (!fos.containsKey(prevState.getID()) && occurrences >= N) {
//                fos.put(prevState.getID(), currentState);
//            }
//            observedMap.put(prevState.getID() + currentState.getID(), occurrences);
            putOoccurrences(prevState.getID() + currentState.getID(), occurrences);
        } catch (SQLException | UnknownHostException ex) {
            Logger.getLogger(FirstSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


}
