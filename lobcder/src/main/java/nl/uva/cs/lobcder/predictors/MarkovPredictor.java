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
import nl.uva.cs.lobcder.util.MyDataSource;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MarkovPredictor extends MyDataSource implements Predictor {

    Graph graph = new Graph();

    public MarkovPredictor() throws NamingException {
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

        public Graph() throws NamingException {
        }

        void addTransision(LobState from, LobState to) throws SQLException {
            double getWeight = -1;
            int uid = -1;
            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("select uid, weight from "
                        + "successor_table where keyVal = ? ")) {
                    ps.setString(1, from.getID());
                    ps.setString(1, to.getID());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        uid = rs.getInt(1);
                        getWeight = rs.getDouble(2);
                    }
                }
                if (getWeight > -1) {
                    getWeight++;
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            " UPDATE graph_table SET keyVal = ?, lobStateID = ? , weight = ? , WHERE uid = ?", Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement.setInt(1, uid);
                        preparedStatement.executeUpdate();
                        ResultSet rs = preparedStatement.getGeneratedKeys();
                        rs.next();
                    }
                } else {
                    getWeight = 1;
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            "INSERT INTO graph_table(keyVal, lobStateID , weight)"
                            + " VALUES (?, ?, ? )", Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement.setString(1, from.getID());
                        preparedStatement.setString(1, to.getID());
                        preparedStatement.setDouble(3, getWeight);
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
                        nextState = new LobState(Request.Method.valueOf(methodNRes[0]), methodNRes[1]);
                    }
                }
                connection.commit();
            }
            return nextState;
        }
    }
}
