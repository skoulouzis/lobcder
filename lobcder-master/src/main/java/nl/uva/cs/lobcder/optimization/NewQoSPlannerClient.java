/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author S. Koulouzis
 */
public class NewQoSPlannerClient {

    private final Client client;
    private String uri;

    public NewQoSPlannerClient(String uri) throws IOException {
        client = Client.create();
        this.uri = uri;
    }

    public List<FloodlightStats> getStats() {
        try {
            WebResource webResource = client.resource(uri);
//            /wm/core/switch/00:00:c2:b3:aa:aa:2d:41/port/json
            String switchName = "00:00:c2:b3:aa:aa:2d:41";
            WebResource res = webResource.path("wm").path("core").path("switch").path(switchName).path("port").path("json");

            List<FloodlightStats> stats = res.accept(MediaType.APPLICATION_JSON).
                    get(new GenericType<List<FloodlightStats>>() {
                    });
            return stats;
        } catch (Exception ex) {
            Logger.getLogger(NewQoSPlannerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void pushFlow(String switchID, int srcPort, int destPort) {
        WebResource webResource = client.resource(uri);
        WebResource res = webResource.path("wm").path("staticflowentrypusher").path("json");

        String input = "{\"switch\": \"" + switchID + "\", \"name\":\"static-flow-p" + srcPort + "-p" + destPort + "\", \"cookie\":\"0\", \"priority\":\"32768\", \"ingress-port\":\"" + srcPort + "\", \"active\":\"true\", \"actions\":\"output=" + destPort + "\"}";

        ClientResponse response = res.type("application/json")
                .post(ClientResponse.class, input);

//        if (response.getStatus() != 200) {
//            throw new RuntimeException("Failed : HTTP error code : "
//                    + response.getStatus());
//        }
    }

//    @Data
//    @XmlRootElement(name = "FloodlightStats")
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class FloodlightStats {
//    }
    @XmlRootElement(name = "00:00:c2:b3:aa:aa:2d:41")
    @XmlAccessorType(XmlAccessType.NONE)
    public static class FloodlightStats {

        @XmlElement(name = "portNumber")
        public int portNumber;
        @XmlElement(name = "receivePackets")
        public long receivePackets;
        @XmlElement(name = "transmitPackets")
        public long transmitPackets;
        @XmlElement(name = "receiveBytes")
        public long receiveBytes;
        @XmlElement(name = "transmitBytes")
        public long transmitBytes;
        @XmlElement(name = "transmitDropped")
        public long transmitDropped;
        @XmlElement(name = "receiveErrors")
        public long receiveErrors;
        @XmlElement(name = "receiveDropped")
        public long receiveDropped;
        @XmlElement(name = "transmitErrors")
        public long transmitErrors;
        @XmlElement(name = "receiveFrameErrors")
        public long receiveFrameErrors;
        @XmlElement(name = "receiveOverrunErrors")
        public long receiveOverrunErrors;
        @XmlElement(name = "receiveCRCErrors")
        public long receiveCRCErrors;
        @XmlElement(name = "collisions")
        public int collisions;
        public long totalPackets;
    }
}
