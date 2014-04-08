/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.MyDataSource;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 * PP is a simple predictor which predicts successors to pairs of files. For
 * example, if the sequence BCD is observed, then the next time the sequence BC
 * is observed, D will be predicted as the next file to be requested.
 *
 * @author S. Koulouzis
 */
public class PredecessorPosition extends MyDataSource implements Predictor {

    List<LobState> stateList1 = new ArrayList<>();
    Map<String, LobState> stateMap = new HashMap<>();
    static Integer len;

    public PredecessorPosition() throws NamingException, IOException {
        len = PropertiesHelper.PredecessorPositionLen();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        stateList1.add(currentState);
        if (stateList1.size() >= len + 1) {
            String key = stateList1.get(0).getID() + stateList1.get(1).getID();
            return stateMap.get(key);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        stateList1.add(0, prevState);
        stateList1.add(1, currentState);
        if (stateList1.size() >= len + 1) {
            String key = stateList1.get(0).getID() + stateList1.get(1).getID();
            stateMap.put(key, currentState);
        }
    }
}
