/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import nl.uva.cs.lobcder.optimization.Vertex;

/**
 *
 * @author S. Koulouzis
 */
public interface Predictor {

    public void stop();

    public Vertex getNextState(Vertex currentState);

    public void setPreviousStateForCurrent(Vertex prevState, Vertex currentState);
    
}
