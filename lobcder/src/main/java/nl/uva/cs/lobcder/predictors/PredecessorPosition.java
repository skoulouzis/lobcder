/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class PredecessorPosition extends DBMapPredictor {

    List<LobState> stateList = new ArrayList<>();
    List<String> keyList = new ArrayList<>();
//    Map<String, LobState> stateMap = new HashMap<>();
    static Integer len;

    public PredecessorPosition() throws NamingException, IOException, SQLException {
        deleteAll();
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
            try {
                String key = "";
                for (int i = 0; i < len; i++) {
                    key += stateList.get(i).getID();
                }
                stateList.remove(0);
                return getSuccessor(key);
                //            return stateMap.get(key);
            } catch (SQLException ex) {
                Logger.getLogger(PredecessorPosition.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        keyList.add(prevState.getID());

        if (keyList.size() >= len) {
            try {
                String key = "";
                for (int i = 0; i < len; i++) {
                    key += keyList.get(i);
                }
                putSuccessor(key, currentState, false);
                //            if (!stateMap.containsKey(key)) {
                //                stateMap.put(key, currentState);
                //            }
                keyList.remove(0);
            } catch (SQLException ex) {
                Logger.getLogger(PredecessorPosition.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnknownHostException ex) {
                Logger.getLogger(PredecessorPosition.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
