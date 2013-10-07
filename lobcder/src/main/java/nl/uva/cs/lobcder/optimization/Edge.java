/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

/**
 *
 * @author S. Koulouzis
 */
class Edge {

    private Vertex v1;
    private Vertex v2;
    private double wheight;

    public Edge(Vertex v1, Vertex v2, double wheight) {
        this.v1 = v1;
        this.v2 = v2;
        this.wheight = wheight;
    }

    Edge(Vertex v1, Vertex v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    Vertex getVertex1() {
        return v1;
    }

    Vertex getVertex2() {
        return v2;
    }

    double getWeight() {
        return wheight;
    }

    String getID() {
        return v1.getID() + ":" + v2.getID();
    }
}
