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
 * The RP, or j-out-of-k predictor is an extension of SS. It predicts the most
 * frequent successor to appear in at least k of the last j requests. If no such
 * file is found, RP declines to issue a prediction. It maintains the last k
 * observed successors for each file. It predicts the most frequent successor to
 * appear in at least j of the last k requests, with recent as a tiebreaker
 *
 * @author S. Koulouzis
 */
public class RecentPopularity extends MyDataSource implements Predictor {

    Map<String, List<LobState>> lastObservedK = new HashMap<>();
    static Integer j;
    static Integer k;

    public RecentPopularity() throws NamingException, IOException {
        j = PropertiesHelper.RecentPopularityJ();
        k = PropertiesHelper.RecentPopularityK();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        List<LobState> listOfKSuccessors = lastObservedK.get(currentState.getID());
        if (listOfKSuccessors != null && listOfKSuccessors.size() >= k) {
            return getPopularState(listOfKSuccessors);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        List<LobState> listOfKSuccessors = lastObservedK.get(prevState.getID());
        if (listOfKSuccessors == null) {
            listOfKSuccessors = new ArrayList<>();
        }
        if (listOfKSuccessors.size() >= k) {
            listOfKSuccessors.remove(0);
        }
        listOfKSuccessors.add(prevState);
        lastObservedK.put(prevState.getID(), listOfKSuccessors);
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
