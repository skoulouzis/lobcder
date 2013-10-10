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

    private LobState v1;
    private LobState v2;
    private double wheight;

    public Edge(LobState v1, LobState v2, double wheight) {
        this.v1 = v1;
        this.v2 = v2;
        this.wheight = wheight;
    }

    Edge(LobState v1, LobState v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    LobState getVertex1() {
        return v1;
    }

    LobState getVertex2() {
        return v2;
    }

    double getWeight() {
        return wheight;
    }

    String getID() {
        return v1.getMethod()+","+v1.getResourceName() + ":" + v2.getMethod()+","+v2.getResourceName();
    }
}
