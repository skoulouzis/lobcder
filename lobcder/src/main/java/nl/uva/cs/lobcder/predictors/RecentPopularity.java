/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;
import static nl.uva.cs.lobcder.predictors.DBMapPredictor.type;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.method;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.resource;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.state;

/**
 * The RP, or j-out-of-k predictor is an extension of SS. It predicts the most
 * frequent successor to appear in at least k of the last j requests. If no such
 * file is found, RP declines to issue a prediction. It maintains the last k
 * observed successors for each file. It predicts the most frequent successor to
 * appear in at least j of the last k requests, with recent as a tiebreaker
 *
 * @author S. Koulouzis
 */
public class RecentPopularity extends DBMapPredictor {

    Map<String, List<LobState>> lastObservedK = new HashMap<>();
    static Integer j;
    static Integer k;

    public RecentPopularity() throws NamingException, IOException, SQLException {
        super();
        j = PropertiesHelper.RecentPopularityJ();
        k = PropertiesHelper.RecentPopularityK();
        deleteAll();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {

        String currentID;
        switch (type) {
            case state:
                currentID = currentState.getID();
                break;
            case resource:
                currentID = currentState.getResourceName();
                break;
            case method:
                currentID = currentState.getMethod().code;
                break;
            default:
                currentID = currentState.getID();
                break;
        }
        List<LobState> listOfKSuccessors = lastObservedK.get(currentID);
        if (listOfKSuccessors != null && listOfKSuccessors.size() >= k) {
            return getPopularState(listOfKSuccessors);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        String prevID = null;
        String currentID = null;
        switch (type) {
            case state:
                prevID = prevState.getID();
                currentID = currentState.getID();
                break;
            case resource:
                prevID = prevState.getResourceName();
                currentID = currentState.getResourceName();
                break;
            case method:
                prevID = prevState.getMethod().code;
                currentID = currentState.getMethod().code;
                break;
            default:
                prevID = prevState.getID();
                currentID = currentState.getID();
                break;
        }

        List<LobState> listOfKSuccessors = lastObservedK.get(prevID);
        if (listOfKSuccessors == null) {
            listOfKSuccessors = new ArrayList<>();
        }
        if (listOfKSuccessors.size() >= k) {
            listOfKSuccessors.remove(0);
        }
        listOfKSuccessors.add(prevState);
        lastObservedK.put(prevID, listOfKSuccessors);
    }

    private LobState getPopularState(List<LobState> listOfKSuccessors) {
        int count = 1, tempCount;
        LobState popular = listOfKSuccessors.get(0);
        LobState temp;
        for (int i = 0; i < (listOfKSuccessors.size() - 1); i++) {
            temp = listOfKSuccessors.get(i);
            tempCount = 0;
            for (int j = 1; j < listOfKSuccessors.size(); j++) {
                if (temp.getID().equals(listOfKSuccessors.get(j).getID())) {
                    tempCount++;
                }
            }
            if (tempCount > count) {
                popular = temp;
                count = tempCount;
            }
        }
        if (count >= j) {
            return popular;
        } else {
            return null;
        }
    }
}
