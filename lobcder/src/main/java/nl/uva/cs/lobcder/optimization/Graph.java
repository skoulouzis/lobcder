/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author S. Koulouz
 */
class Graph {

    private List<LobState> vertices = new ArrayList<LobState>();
    private Map<String, Edge> edges = new HashMap<String, Edge>();

    public Graph() {
    }

    boolean containsState(LobState vertex) {
        for (LobState v : vertices) {
            if (v.getResourceName().equals(vertex.getResourceName()) &&
                    v.getMethod().toString().equals(vertex.getMethod().toString())) {
                return true;
            }
        }
        return false;
    }

    void addVertex(LobState v) {
        vertices.add(v);
    }

    boolean containsEdge(Edge edge) {
        return edges.containsKey(edge.getID());
//        for (Edge e : edges) {
//            if (e.getVertex1().getID().equals(edge.getVertex1().getID()) && e.getVertex2().getID().equals(edge.getVertex2().getID())) {
//                return true;
//            }
//        }
//        return false;
//        return edges.contains(edge);
    }

    double getWeight(LobState v1, LobState v2) {
//        for (Edge e : edges) {
//            if (e.getVertex1().getID().equals(v1.getID()) && e.getVertex2().getID().equals(v2.getID())) {
//                return e.getWeight();
//            }
//        }
//        return -1;
        String id = getEdgeID(v1, v2);
        Edge e = edges.get(id);
        if (e != null) {
            return edges.get(id).getWeight();
        }
        return 0;
    }

    void setEdgeWeight(Edge edge) {
        this.edges.put(edge.getID(), edge);
    }

    private String getEdgeID(LobState v1, LobState v2) {
        return v1.getMethod()+","+v1.getResourceName() + ":" + v2.getMethod()+","+v2.getResourceName();
    }

    List<LobState> vertexSet() {
        return this.vertices;
    }
}
