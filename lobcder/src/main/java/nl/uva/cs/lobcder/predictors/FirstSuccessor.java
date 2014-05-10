/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.Cache;
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
public class FirstSuccessor extends Cache implements Predictor {

//    Map<String, LobState> fos = new HashMap<>();
    Map<String, Integer> observedMap = new HashMap<>();
    static Integer N;

    public FirstSuccessor() throws NamingException, IOException {
        super(FirstSuccessor.class.getSimpleName());
        N = PropertiesHelper.getFirstSuccessorrN();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        LobState nextState = (LobState) getJcsCache().get(currentState.getID());
//        LobState nextState = fos.get(currentState.getID());
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
        LobState state = (LobState) getJcsCache().get(prevState.getID());
        if (state == null && occurrences >= N) {
            try {
                getJcsCache().put(prevState.getID(), currentState);
            } catch (Exception ex) {
                Logger.getLogger(FirstSuccessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        observedMap.put(prevState.getID() + currentState.getID(), occurrences);
    }
}
