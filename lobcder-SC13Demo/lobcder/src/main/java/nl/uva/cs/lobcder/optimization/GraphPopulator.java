/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import io.milton.http.Request.Method;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
class GraphPopulator implements Runnable {

    private final DataSource datasource;
    private boolean buildGlobalGraph = true;

    GraphPopulator(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void run() {
        if (buildGlobalGraph) {
            try (Connection connection = datasource.getConnection()) {
                try {
                    buildOrUpdateGlobalGraph(connection);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(GraphPopulator.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (SQLException ex) {
                Logger.getLogger(GraphPopulator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void saveAsCSV(Graph graph) throws IOException {
        Map<String, Edge> edges = graph.getEdges();
        Set<Entry<String, Edge>> entrySet = edges.entrySet();
        Iterator<Entry<String, Edge>> iter = entrySet.iterator();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource("/webapp/data/stateTrans.csv");
//        FileWriter writer = new FileWriter(null);
        String data = "source;target;weight\n";
        while (iter.hasNext()) {
            Entry<String, Edge> entry = iter.next();
            Edge edge = entry.getValue();
//            if (edge.getWeight() > 1) {
            data += edge.getVertex1().getID() + ";" + edge.getVertex2().getID() + ";" + edge.getWeight() + "\n";
//            }
        }
        log.log(Level.INFO, data);
    }

    private void buildOrUpdateGlobalGraph(Connection connection) throws SQLException, MalformedURLException {
        connection.setAutoCommit(false);
        try (Statement s = connection.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT methodName, requestURL, timeStamp FROM requests_table")) {
                LobState prevState = new LobState();
                if (rs.first()) {
                    prevState.setMethod(Method.valueOf(rs.getString(1)));
                    URL url = new URL(rs.getString(2));
                    prevState.setResourceName(url.getPath());
                }
                while (rs.next()) {
                    LobState currentState = new LobState();
                    currentState.setMethod(Method.valueOf(rs.getString(1)));
                    URL url = new URL(rs.getString(2));
                    currentState.setResourceName(url.getPath());
                    //                    java.sql.Date timeStamp = rs.getDate(3);
                    Timestamp timeStamp = rs.getTimestamp(3);
                    insertOrUpdateState(connection, prevState, currentState, timeStamp);
//                    log.log(Level.INFO, "source: " + prevState.getID() + " target: " + currentState.getID() + " timeStamp: " + timeStamp);
                    prevState = currentState;
                    connection.commit();
                }
            }
            connection.commit();
        }
    }

    private void insertOrUpdateState(Connection connection, LobState prevState, LobState currentState, Timestamp timeStamp) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("select uid, weight from state_table WHERE sourceState = ? AND targetState = ?")) {
            ps.setString(1, prevState.getID());
            ps.setString(2, currentState.getID());
            ResultSet rs2 = ps.executeQuery();
            double weight = 1.0;
            if (rs2.next()) {
                long uid = rs2.getLong(1);
                weight = rs2.getDouble(2);
                weight++;
                updateState(connection, uid, weight);
            } else {
                insertState(connection, prevState.getID(), currentState.getID(), timeStamp);
            }
            log.log(Level.INFO, "source: {0} target: {1} weight: {2} timeStamp: {3}", new Object[]{prevState.getID(), currentState.getID(), weight, timeStamp});
            connection.commit();
        }
    }

    private void updateState(Connection connection, long uid, double weight) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE state_table SET `weight` = ? WHERE uid = ?")) {
            ps.setDouble(1, weight);
            ps.setLong(2, uid);
            ps.executeUpdate();
            connection.commit();
        }
    }

    private void insertState(Connection connection, String iD1, String iD2, Timestamp timeStamp) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(""
                        + "INSERT INTO state_table (sourceState, targetState, weight, timeStamp) VALUES (?, ?, ?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, iD1);
            preparedStatement.setString(2, iD2);
            preparedStatement.setDouble(3, 1.0);
            preparedStatement.setTimestamp(4, timeStamp);
            preparedStatement.executeUpdate();
            connection.commit();
        }
    }
    
    LobState getNextState(LobState state) throws SQLException {
        LobState nextState = null;
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("SELECT targetState FROM state_table where sourceState=? ORDER BY weight*RAND() DESC LIMIT 1")) {
                ps.setString(1, state.getID());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String targetState = rs.getString(1);
                    String[] id = targetState.split(",");
                    nextState = new LobState(Method.valueOf(id[0]), id[1]);
                }
                connection.commit();
            }
            connection.close();
        }
        return nextState;
    }
}
