/*
 * Copyright 2014 S. Koulouzis
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

import io.milton.http.Request;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import nl.uva.cs.lobcder.optimization.LobState;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import lombok.extern.java.Log;
import static nl.uva.cs.lobcder.predictors.DBMapPredictor.type;
import nl.uva.cs.lobcder.util.MyDataSource;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MarkovPredictor extends MyDataSource implements Predictor {

    Graph graph = null;

    public MarkovPredictor() throws NamingException, SQLException, IOException {
        graph = new Graph();
    }

    @Override
    public void stop() {
    }

    @Override
    public LobState getNextState(LobState currentState) {
        try {
            return predictNextState(currentState);
        } catch (SQLException ex) {
            Logger.getLogger(MarkovPredictor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        try {
            graph.addTransision(prevState, currentState);
        } catch (SQLException ex) {
            Logger.getLogger(MarkovPredictor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private LobState predictNextState(LobState state) throws SQLException {
        return graph.getWeightedRandomState(state);
    }

    class Graph extends MyDataSource {

        public Graph() throws NamingException, SQLException, IOException {
            type = PropertiesHelper.getPredictionType();
            deleteAll();
        }

        private void deleteAll() throws SQLException {
            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM successor_table")) {
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM occurrences_table")) {
                    ps.executeUpdate();
                }
                connection.commit();
            }
        }

        void addTransision(LobState from, LobState to) throws SQLException {
            double weight = -1;
            int uid = -1;
            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("select uid, weight from "
                        + "successor_table where keyVal = ? and lobStateID = ?")) {
                    ps.setString(1, from.getID());
                    ps.setString(2, to.getID());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        uid = rs.getInt(1);
                        weight = rs.getDouble(2);
                    }
                }
                if (weight > -1) {
                    weight++;
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            "UPDATE successor_table SET weight = ? WHERE uid = ?", Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement.setDouble(1, weight);
                        preparedStatement.setInt(2, uid);
                        preparedStatement.executeUpdate();
                        ResultSet rs = preparedStatement.getGeneratedKeys();
                        rs.next();
                    }
                } else {
                    weight = 1;
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            "INSERT INTO successor_table(keyVal, lobStateID , weight)"
                            + " VALUES (?, ?, ? )", Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement.setString(1, from.getID());
                        preparedStatement.setString(2, to.getID());
                        preparedStatement.setDouble(3, weight);
                        preparedStatement.executeUpdate();
                        ResultSet rs = preparedStatement.getGeneratedKeys();
                        rs.next();
                    }
                }
                connection.commit();
            }
        }

        private LobState getWeightedRandomState(LobState from) throws SQLException {
            LobState nextState = null;
            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("select lobStateID from successor_table where keyVal = ? order by weight*rand() desc limit 1")) {
                    ps.setString(1, from.getID());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String nextID = rs.getString(1);
                        String[] methodNRes = nextID.split(",");
                        String resource;
                        if (methodNRes.length <= 1) {
                            resource = "/";
                        } else {
                            resource = methodNRes[1];
                        }
                        nextState = new LobState(Request.Method.valueOf(methodNRes[0]), resource);
                    }
                }
                connection.commit();
            }
            return nextState;
        }
    }
}
