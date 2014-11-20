/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.Vertex;
import static nl.uva.cs.lobcder.predictors.DBMapPredictor.type;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.method;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.resource;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.state;

/**
 * If the last observed successor of A is B, then B will be predicted as the
 * successor every time A is observed
 *
 * @author S. Koulouzis
 */
public class LastSuccessor extends DBMapPredictor {

//    Map<String, LobState> lastS = new HashMap<>();
    public LastSuccessor() throws NamingException, SQLException, IOException {
        super();
        deleteAll();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public Vertex getNextState(Vertex currentState) {
        try {
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
            //        LobState nextState = lastS.get(currentState.getID());
            Vertex nextState = getSuccessor(currentID);
            return nextState;
        } catch (SQLException ex) {
            Logger.getLogger(LastSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(Vertex prevState, Vertex currentState) {
        try {

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
            //            lastS.put(prevState.getID(), currentState);
            putSuccessor(prevID, currentID, true);

        } catch (SQLException | UnknownHostException ex) {
            Logger.getLogger(LastSuccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
