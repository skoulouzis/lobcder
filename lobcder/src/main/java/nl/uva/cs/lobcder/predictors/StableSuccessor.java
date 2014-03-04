/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.MyDataSource;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 * If the first observed successor of A is B, then B will be predicted as the
 * successor every time A is observed
 *
 * @author S. Koulouzis
 */
public class StableSuccessor extends MyDataSource implements Predictor {

    Map<String, LobState> lastS = new HashMap<>();
    Map<String, Integer> observedMap = new HashMap<>();
    Integer N;

    public StableSuccessor() throws NamingException, IOException {
        N = PropertiesHelper.getStableSuccessorN();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        LobState nextState = null;
        if (lastS.containsKey(currentState.getID())) {
            nextState = lastS.get(currentState.getID());
        }

        Integer occurrences = null;
        if (nextState != null && observedMap.containsKey(currentState.getID() + nextState.getID())) {
            occurrences = observedMap.get(currentState.getID() + nextState.getID());
        }
        if (occurrences != null && occurrences >= N) {
            return nextState;
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        lastS.put(prevState.getID(), currentState);
        Integer occurrences = observedMap.get(prevState.getID() + currentState.getID());
        if (occurrences == null) {
            occurrences = 1;
        } else {
            occurrences++;
        }
        observedMap.put(prevState.getID() + currentState.getID(), occurrences);
    }
}
