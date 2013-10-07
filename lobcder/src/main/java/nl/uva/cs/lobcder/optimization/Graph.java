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

    private List<Vertex> vertices = new ArrayList<Vertex>();
    private Map<String, Edge> edges = new HashMap<String, Edge>();

    public Graph() {
    }

    boolean containsVertex(Vertex vertex) {
        return vertices.contains(vertex);
    }

    void addVertex(Vertex v) {
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

    double getWeight(Vertex v1, Vertex v2) {
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

    private String getEdgeID(Vertex v1, Vertex v2) {
        return v1.getID() + ":" + v2.getID();
    }

    public String toString() {
        return edges.keySet().toString();
    }

    List<Vertex> vertexSet() {
        return this.vertices;
    }
}
