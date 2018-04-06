/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import io.milton.common.Path;
import io.milton.http.Request.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sql.DataSource;

import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author S. Koulouzis
 */
class GraphPopulator implements Runnable {

    private final DataSource datasource;
//    private Graph graph;
    private Graph graph;

    GraphPopulator(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void run() {
        try (Connection connection = datasource.getConnection()) {
            LogicalData root = getLogicalDataByUid(Long.valueOf(1), connection);
            ArrayList<Path> nodes = getNodes(Path.root, root, connection, null);

//            String msg = "";
//            for (Path d : nodes) {
//                msg += d + "\n";
//            }
//            log.log(Level.INFO, "Nodes: {0}", msg);
            Graph graph = populateGraph(nodes);
            ArrayList<Vertex> transitions = getTransitions(connection, nodes);

            graph = addEdges(graph, transitions);

            this.graph = graph;
        } catch (MalformedURLException | SQLException ex) {
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
            LinkedList<LogicalData> res = new LinkedList<>();
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
//                element.setDataLocationPreference(rs.getString(20));
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
//                res.setDataLocationPreference(rs.getString(20));
                res.setStatus(rs.getString(21));
                return res;
            } else {
                return null;
            }
        }
    }

    private Graph populateGraph(List<Path> nodes) {
        graph = new Graph();

        for (int i = 0; i < nodes.size(); i++) {
            for (Method m : Method.values()) {
                Vertex v1 = new Vertex(m, nodes.get(i).toString());
                if (!graph.containsState(v1)) {
                    graph.addVertex(v1);
                }
            }
        }
        return graph;
    }

    private ArrayList<Path> getNodes(Path p, LogicalData node, Connection connection,
            ArrayList<Path> nodes) throws SQLException {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        if (!nodes.contains(p)) {
            nodes.add(p);
        }

        Collection<LogicalData> children = getChildrenByParentRef(node.getUid(), connection);
        for (LogicalData ld : children) {
            if (ld.getUid() != node.getUid()) {
//                log.log(Level.INFO, "children: " + ld.getName());
                Path nextPath = Path.path(p, ld.getName());
//                log.log(Level.INFO, "node: " + nextPath);
                if (ld.isFolder()) {
                    getNodes(nextPath, ld, connection, nodes);
                }
                if (!nodes.contains(nextPath)) {
                    nodes.add(nextPath);
                }
            }
        }
        return nodes;
    }

    private ArrayList<Vertex> getTransitions(Connection connection, ArrayList<Path> nodes) throws SQLException, MalformedURLException {
        ArrayList<Vertex> trans = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT requestURL, methodName FROM requests_table WHERE ");

        String queryPath;
        if (nodes.get(0).isRoot()) {
            queryPath = "/lobcder/dav";
        } else {
            queryPath = nodes.get(0).toString();
        }

        query.append("(requestURL LIKE '%" + queryPath + "' ");
        for (int i = 1; i < nodes.size(); i++) {
            if (nodes.get(i).isRoot()) {
                queryPath = "/lobcder/dav";
            } else {
                queryPath = nodes.get(i).toString();
            }
            query.append("OR requestURL LIKE '%" + queryPath + "' ");
        }
        query.append(")");

        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String url = rs.getString(1);
                String method = rs.getString(2);
//                log.log(Level.FINE, "URL: {0}", new URL(url).getPath());
                //                String[] parts = url.split("http://localhost:8080/lobcder/dav");
                String strPath = new URL(url).getPath();
                String[] parts = strPath.split("/lobcder/dav");
                Path path;
                if (parts != null && parts.length > 1) {
                    path = Path.path(parts[1]);
                } else {
                    path = Path.root;
                }
                trans.add(new Vertex(Method.valueOf(method), path.toString()));
//                log.log(Level.FINE, "path: {0} method {1}, size: {2}", new Object[]{path, method, trans.size()});
            }
        }
        return trans;
    }

    private Graph addEdges(Graph graph, ArrayList<Vertex> transitions) {
        for (int i = 0; i < transitions.size(); i++) {
            if (i >= transitions.size() - 1) {
//                nextI = i;
                break;
            } else {
                Vertex v1 = transitions.get(i);
                Vertex v2 = transitions.get(i + 1);
                if (graph.containsState(v1) && graph.containsState(v2)) {
//                    log.log(Level.INFO, "V1: {0}: V2: {1}", new Object[]{v1.getID(),v2.getID()});
                    Edge e = new Edge(v1, v2);
                    double w = 0;
                    if (graph.containsEdge(e)) {
                        w = graph.getWeight(v1, v2);
                    }
                    e = new Edge(v1, v2, ++w);
                    graph.setEdgeWeight(e);
//                    log.log(Level.INFO, "Edge: {0}: {1}", new Object[]{e.getID(), w});
                }
            }
        }
        return graph;
    }

    Graph getGraph() {
        return graph;
    }
}
