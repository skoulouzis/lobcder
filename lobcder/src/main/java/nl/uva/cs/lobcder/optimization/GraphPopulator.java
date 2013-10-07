/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.java.Log;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.resources.StorageSite;

/**
 *
 * @author S. Koulouzis
 */
@Log
class GraphPopulator implements Runnable {

    private final DataSource datasource;
    private Graph graph;

    public GraphPopulator(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void run() {
        List<String> nodes = new ArrayList<>();
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement s = connection.createStatement()) {
                try (ResultSet rs = s.executeQuery("select requestURL from requests_table where methodName='GET'")) {
                    while (rs.next()) {
                        nodes.add(rs.getString(1));
                    }
                }

            } catch (SQLException e) {
                GraphPopulator.log.log(Level.SEVERE, null, e);
            }
        } catch (SQLException ex) {
            Logger.getLogger(GraphPopulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        populateGraph(nodes);
    }

    private void populateGraph(List<String> nodes) {
        String next;
        int nextI;
        graph = new Graph();
        for (int i = 0; i < nodes.size(); i++) {
            if (i >= nodes.size() - 1) {
//                nextI = i;
                break;
            } else {
                nextI = i + 1;
            }
            next = nodes.get(nextI);
            Vertex v1 = new Vertex(nodes.get(i));
            if (!graph.containsVertex(v1)) {
                graph.addVertex(v1);
            }
            Vertex v2 = new Vertex(next);
            if (!graph.containsVertex(v2)) {
                graph.addVertex(v2);
            }
            Edge edge;
            double w;
            edge = new Edge(v1, v2);
            if (graph.containsEdge(edge)) {
                w = graph.getWeight(v1, v2);
//                System.out.println(w);

                edge = new Edge(v1, v2, ++w);
                graph.setEdgeWeight(edge);
                w = graph.getWeight(v1, v2);
//                System.out.println("ΑΑΑΑΑ " + nodes[i] + "->" + next + ": " + w);
            } else {
                edge = new Edge(v1, v2, 1.0);
                graph.setEdgeWeight(edge);
                w = graph.getWeight(v1, v2);
//                System.out.println("ΒΒΒΒ " + nodes[i] + "->" + next + ": " + w);
            }
        }
    }

    Graph getGraph() {
        return graph;
    }
}
