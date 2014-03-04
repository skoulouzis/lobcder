/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import nl.uva.cs.lobcder.optimization.LobState;

/**
 *
 * @author S. Koulouzis
 */
public interface Predictor {

    public void stop();

    public LobState getNextState(LobState currentState);

    public void setPreviousStateForCurrent(LobState prevState, LobState currentState);
    
}
