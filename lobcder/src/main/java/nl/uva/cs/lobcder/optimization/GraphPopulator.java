/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import io.milton.common.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.java.Log;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author S. Koulouzis
 */
@Log
class GraphPopulator implements Runnable {

    private final DataSource datasource;
//    private Graph graph;

    public GraphPopulator(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void run() {
        try (Connection connection = datasource.getConnection()) {
            LogicalData root = getLogicalDataByUid(Long.valueOf(1), connection);

            ArrayList<Path> nodes = getNodes(Path.root, root, connection, null);

            String msg = "";
            for (Path d : nodes) {
                msg += d + "\n";
            }
            log.log(Level.INFO, "Nodes: " + msg);

            Graph graph = getGraph(nodes);
            ArrayList<Path> transitions = getTransitions(connection);

            graph = addEdges(graph, transitions);
        } catch (SQLException ex) {
            Logger.getLogger(GraphPopulator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public Collection<LogicalData> getChildrenByParentRef(Long parentRef, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT uid, ownerId, datatype, ldName, createDate, modifiedDate, ldLength, "
                        + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                        + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                        + "description, locationPreference "
                        + "FROM ldata_table WHERE ldata_table.parentRef = ?")) {
            preparedStatement.setLong(1, parentRef);
            ResultSet rs = preparedStatement.executeQuery();
            LinkedList<LogicalData> res = new LinkedList<LogicalData>();
            while (rs.next()) {
                LogicalData element = new LogicalData();
                element.setUid(rs.getLong(1));
                element.setParentRef(parentRef);
                element.setOwner(rs.getString(2));
                element.setType(rs.getString(3));
                element.setName(rs.getString(4));
                element.setCreateDate(rs.getTimestamp(5).getTime());
                element.setModifiedDate(rs.getTimestamp(6).getTime());
                element.setLength(rs.getLong(7));
                element.setContentTypesAsString(rs.getString(8));
                element.setPdriGroupId(rs.getLong(9));
                element.setSupervised(rs.getBoolean(10));
                element.setChecksum(rs.getString(11));
                element.setLastValidationDate(rs.getLong(12));
                element.setLockTokenID(rs.getString(13));
                element.setLockScope(rs.getString(14));
                element.setLockType(rs.getString(15));
                element.setLockedByUser(rs.getString(16));
                element.setLockDepth(rs.getString(17));
                element.setLockTimeout(rs.getLong(18));
                element.setDescription(rs.getString(19));
                element.setDataLocationPreference(rs.getString(20));
                res.add(element);
            }
            return res;
        }
    }

    public LogicalData getLogicalDataByUid(Long UID, @Nonnull Connection connection) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement("SELECT parentRef, ownerId, datatype, ldName, "
                        + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                        + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                        + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                        + "FROM ldata_table WHERE ldata_table.uid = ?")) {
            ps.setLong(1, UID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(UID);
                res.setParentRef(rs.getLong(1));
                res.setOwner(rs.getString(2));
                res.setType(rs.getString(3));
                res.setName(rs.getString(4));
                res.setCreateDate(rs.getTimestamp(5).getTime());
                res.setModifiedDate(rs.getTimestamp(6).getTime());
                res.setLength(rs.getLong(7));
                res.setContentTypesAsString(rs.getString(8));
                res.setPdriGroupId(rs.getLong(9));
                res.setSupervised(rs.getBoolean(10));
                res.setChecksum(rs.getString(11));
                res.setLastValidationDate(rs.getLong(12));
                res.setLockTokenID(rs.getString(13));
                res.setLockScope(rs.getString(14));
                res.setLockType(rs.getString(15));
                res.setLockedByUser(rs.getString(16));
                res.setLockDepth(rs.getString(17));
                res.setLockTimeout(rs.getLong(18));
                res.setDescription(rs.getString(19));
                res.setDataLocationPreference(rs.getString(20));
                res.setStatus(rs.getString(21));
                return res;
            } else {
                return null;
            }
        }
    }

    private Graph getGraph(List<Path> nodes) {
        Graph graph = new Graph();

        for (int i = 0; i < nodes.size(); i++) {
            Vertex v1 = new Vertex(nodes.get(i).toString());
            if (!graph.containsVertex(v1)) {
                graph.addVertex(v1);
            }
        }

        return graph;
//        for (int i = 0; i < nodes.size(); i++) {
//            Vertex v1 = new Vertex(nodes.get(i).toString());
//            for (int j = 0; j < nodes.size(); j++) {
//                Vertex v2 = new Vertex(nodes.get(j).toString());
//                Edge edge = new Edge(v1, v2);
//                graph.setEdgeWeight(edge);
//                log.log(Level.INFO, "edge: " + v1 + " : " + v2);
//            }
//        }
//        log.log(Level.INFO, "Graph: " + graph);

//        String next;
//        int nextI;
//        graph = new Graph();
//        for (int i = 0; i < nodes.size(); i++) {
//            if (i >= nodes.size() - 1) {
////                nextI = i;
//                break;
//            } else {
//                nextI = i + 1;
//            }
//            
//            next = nodes.get(nextI);
//            Vertex v1 = new Vertex(nodes.get(i));
//            if (!graph.containsVertex(v1)) {
//                graph.addVertex(v1);
//            }
//            Vertex v2 = new Vertex(next);
//            if (!graph.containsVertex(v2)) {
//                graph.addVertex(v2);
//            }
//            Edge edge;
//            double w;
//            edge = new Edge(v1, v2);
//            if (graph.containsEdge(edge)) {
//                w = graph.getWeight(v1, v2);
////                System.out.println(w);
//
//                edge = new Edge(v1, v2, ++w);
//                graph.setEdgeWeight(edge);
//                w = graph.getWeight(v1, v2);
////                System.out.println("ΑΑΑΑΑ " + nodes[i] + "->" + next + ": " + w);
//            } else {
//                edge = new Edge(v1, v2, 1.0);
//                graph.setEdgeWeight(edge);
//                w = graph.getWeight(v1, v2);
////                System.out.println("ΒΒΒΒ " + nodes[i] + "->" + next + ": " + w);
//            }
//        }
    }

    private ArrayList<Path> getNodes(Path p, LogicalData node, Connection connection,
            ArrayList<Path> nodes) throws SQLException {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        Collection<LogicalData> children = getChildrenByParentRef(node.getUid(), connection);
        for (LogicalData ld : children) {
            if (ld.getUid() != node.getUid()) {
                log.log(Level.INFO, "children: " + ld.getName());
                Path nextPath = Path.path(p, ld.getName());
//                log.log(Level.INFO, "node: " + nextPath);
                if (ld.isFolder()) {
                    getNodes(nextPath, ld, connection, nodes);
                } else {
                    log.log(Level.INFO, "node: " + nextPath);
                    nodes.add(nextPath);
                }
            }
        }
        return nodes;

    }

    private ArrayList<Path> getTransitions(Connection connection) throws SQLException {
        ArrayList<Path> trans = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("select requestURL "
                        + "from requests_table where methodName = 'GET'")) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String url = rs.getString(1);
                log.log(Level.FINE, "URL: " + url);
                String[] parts = url.split("http://localhost:8080/lobcder/dav");
                if (parts != null && parts.length > 1) {
                    Path path = Path.path(parts[1]);
                    trans.add(path);
                    log.log(Level.FINE, "URL: " + url + " size: " + trans.size());
                }
            }
        }
        return trans;
    }

    private Graph addEdges(Graph graph, ArrayList<Path> transitions) {
        for (int i = 0; i < transitions.size(); i++) {
            if (i >= transitions.size() - 1) {
//                nextI = i;
                break;
            } else {
                Vertex v1 = new Vertex(transitions.get(i).toString());
                Vertex v2 = new Vertex(transitions.get(i + 1).toString());
                if (graph.containsVertex(v1) && graph.containsVertex(v2)) {
                    Edge e = new Edge(v1, v2);
                    double w = 0;
                    if (graph.containsEdge(e)) {
                        w = graph.getWeight(v1, v2);
                    }
                    e = new Edge(v1, v2, ++w);
                    graph.setEdgeWeight(e);
                    log.log(Level.INFO, "Edge: {0}: {1}", new Object[]{e, w});
                }
            }
        }
        return graph;
    }
}
