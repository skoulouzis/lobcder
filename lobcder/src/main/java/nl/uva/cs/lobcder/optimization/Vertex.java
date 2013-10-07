/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

/**
 *
 * @author S. Koulouzis
 */
class Vertex {

    private final String id;

    Vertex(String id) {
        this.id = id;
    }

    String getID() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
