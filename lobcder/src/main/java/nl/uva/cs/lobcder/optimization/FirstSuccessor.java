/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import java.util.HashMap;
import java.util.Map;

/**
 * If the first observed successor of A is B, then B will be predicted as the
 * successor every time A is observed
 *
 * @author S. Koulouzis
 */
public class FirstSuccessor implements Predictor {

    Map<String, LobState> fos = new HashMap<>();

    public FirstSuccessor() {
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        LobState nextState = fos.get(currentState.getID());
        if (nextState == null) {
        }
        return nextState;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        if (!fos.containsKey(prevState.getID())) {
            fos.put(prevState.getID(), currentState);
        }
    }
}
