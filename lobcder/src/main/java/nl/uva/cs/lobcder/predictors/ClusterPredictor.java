/*
 * Copyright 2014 alogo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.uva.cs.lobcder.predictors;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LDClustering;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.MyDataSource;

/**
 *
 * @author S. Koulouzis
 */
public class ClusterPredictor implements Predictor {

    private LDClustering ldc;

    public ClusterPredictor() throws NamingException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        super();
        if (ldc == null) {
            ldc = new LDClustering();
            ldc.run();
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public LobState getNextState(LobState currentState) {
        try {
            return ldc.getNextState(currentState);
        } catch (Exception ex) {
            Logger.getLogger(ClusterPredictor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
