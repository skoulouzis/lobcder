/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;

/**
 * If the last observed successor of A is B, then B will be predicted as the
 * successor every time A is observed
 *
 * @author S. Koulouzis
 */
public class LastSuccessor  extends DBMapPredictor {

//    Map<String, LobState> lastS = new HashMap<>();

    public LastSuccessor() throws NamingException, SQLException {
        deleteAll();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
//        LobState nextState = lastS.get(currentState.getID());
        LobState nextState = getNextState(currentState);
        return nextState;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        try {
//            lastS.put(prevState.getID(), currentState);
            putSuccessor(prevState.getID(), currentState, true);
        } catch (SQLException | UnknownHostException ex) {
            Logger.getLogger(LastSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
