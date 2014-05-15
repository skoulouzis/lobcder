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

import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;
import io.milton.http.Request;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.MyDataSource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.frontend.RequestWapper;

/**
 *
 * Uses the DB as its hashmap
 *
 * @author S. Koulouzis
 */
@Log
public class DBMapPredictor extends MyDataSource implements Predictor {

    public DBMapPredictor() throws NamingException {
    }

    protected void putSuccessor(String key, LobState currentState, boolean replace) throws SQLException, UnknownHostException {
        try (Connection connection = getConnection()) {
            if (!replace) {
                String query = "INSERT INTO successor_table (keyVal, lobStateID) "
                        + "SELECT * FROM (SELECT '" + key + "', '" + currentState.getID() + "') AS tmp WHERE NOT EXISTS "
                        + "(SELECT keyVal FROM successor_table WHERE keyVal = '" + key + "') LIMIT 1";
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        query, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.execute();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                } catch (Exception ex) {
                    if (ex.getMessage().contains("Duplicate column name")) {
                        connection.rollback();
                    }
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO successor_table(keyVal, lobStateID)"
                        + " VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, key);
                    preparedStatement.setString(2, currentState.getID());
                    preparedStatement.execute();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                }
            }
            connection.commit();
            connection.close();
        }
    }

    protected Integer getOoccurrences(String key) throws SQLException {
        Integer value = null;
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("select occurrences from "
                    + "occurrences_table where keyVal= ? ")) {
                ps.setString(1, key);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    value = rs.getInt(1);
                }
            }
            connection.commit();
        }
        return value;
    }

    protected void putOoccurrences(String key, Integer occurrences) throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO occurrences_table(keyVal, occurrences)"
                    + " VALUES (?, ? )", Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, key);
                preparedStatement.setInt(2, occurrences);
                preparedStatement.executeUpdate();
                ResultSet rs = preparedStatement.getGeneratedKeys();
                rs.next();
            }
            connection.commit();
        }

    }

    protected LobState getSuccessor(String key) throws SQLException {
        LobState ls = null;
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("select "
                    + "lobStateID from successor_table WHERE keyVal = ? ")) {
                ps.setString(1, key);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String id = rs.getString(1);
                    String method = id.split(",")[0];
                    String resource = id.split(",")[1];
                    ls = new LobState(Request.Method.valueOf(method), resource);
                }
            }
            connection.commit();
        }
        return ls;
    }

    protected void deleteAll() throws SQLException {
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

    @Override
    public void stop() {
    }

    @Override
    public LobState getNextState(LobState currentState) {
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
    }
}
