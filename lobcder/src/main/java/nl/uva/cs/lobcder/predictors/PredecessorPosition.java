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

    List<LobState> stateList = new ArrayList<>();
    List<String> keyList = new ArrayList<>();
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
        stateList.add(currentState);
        if (stateList.size() >= len) {
            String key = "";
            for (int i = 0; i < len; i++) {
                key += stateList.get(i).getID();
            }
            stateList.remove(0);
            return stateMap.get(key);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        keyList.add(prevState.getID());

        if (keyList.size() >= len) {
            String key = "";
            for (int i = 0; i < len; i++) {
                key += keyList.get(i);
            }
            if (!stateMap.containsKey(key)) {
                stateMap.put(key, currentState);
            }
            keyList.remove(0);
        }
    }
}
