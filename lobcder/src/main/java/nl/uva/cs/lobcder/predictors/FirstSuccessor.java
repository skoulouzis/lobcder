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
import static nl.uva.cs.lobcder.predictors.StableSuccessor.N;
import nl.uva.cs.lobcder.util.MyDataSource;
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
public class FirstSuccessor extends MyDataSource implements Predictor {

    Map<String, LobState> fos = new HashMap<>();
    Map<String, Integer> observedMap = new HashMap<>();
    static Integer N;

    public FirstSuccessor() throws NamingException, IOException {
        N = PropertiesHelper.getFirstSuccessorrN();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        LobState nextState = fos.get(currentState.getID());
        return nextState;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        Integer occurrences = observedMap.get(prevState.getID() + currentState.getID());
        if (occurrences == null) {
            occurrences = 1;
        } else {
            occurrences++;
        }

        if (!fos.containsKey(prevState.getID()) && occurrences >= N) {
            fos.put(prevState.getID(), currentState);
        }

        observedMap.put(prevState.getID() + currentState.getID(), occurrences);
    }
}
