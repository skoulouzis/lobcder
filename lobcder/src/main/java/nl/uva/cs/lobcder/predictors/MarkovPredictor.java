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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import nl.uva.cs.lobcder.optimization.LobState;
import java.util.List;
import javax.naming.NamingException;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.MyDataSource;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MarkovPredictor extends MyDataSource implements Predictor {

    Graph graph = new Graph();

    public MarkovPredictor() throws NamingException {
    }

    @Override
    public void stop() {
    }

    @Override
    public LobState getNextState(LobState currentState) {
        return predictNextState(currentState);
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        if (!graph.containsState(prevState)) {
            graph.addVertex(prevState);
        }
        if (!graph.containsState(currentState)) {
            graph.addVertex(currentState);
        }
        double wheight = graph.getWeight(prevState, currentState);
        Edge edge = new Edge(prevState, currentState, ++wheight);
        graph.setEdgeWeight(edge);
    }

    private LobState predictNextState(LobState state) {

        List<LobState> set = graph.vertexSet();

        if (set.isEmpty()) {
            return null;
        }
        // Compute the total weight of all items together
        double totalWeight = 0.0d;
        for (LobState i : set) {
            totalWeight += graph.getWeight(state, i);
        }

        // Now choose a random item
        int randomIndex = -1;
        double random = Math.random() * totalWeight;

        LobState[] vertexArray = new LobState[set.size()];
        vertexArray = set.toArray(vertexArray);
        for (int i = 0; i < vertexArray.length; ++i) {

            random -= graph.getWeight(state, vertexArray[i]);
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        LobState myRandomItem = vertexArray[randomIndex];
        return myRandomItem;
    }

    class Graph {

        private List<LobState> vertices = new ArrayList<>();
        private Map<String, Edge> edges = new HashMap<>();

        public Graph() {
        }

        boolean containsState(LobState vertex) {
            for (LobState v : vertices) {
                if (v.getResourceName().equals(vertex.getResourceName())
                        && v.getMethod().toString().equals(vertex.getMethod().toString())) {
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
            return v1.getMethod() + "," + v1.getResourceName() + ":" + v2.getMethod() + "," + v2.getResourceName();
        }

        List<LobState> vertexSet() {
            return this.vertices;
        }
    }

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
            return v1.getMethod() + "," + v1.getResourceName() + ":" + v2.getMethod() + "," + v2.getResourceName();
        }
    }
}
