/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import com.google.common.collect.HashBiMap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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

            WebResource res = webResource.path("wm").path("device/");
            String s = res.get(String.class);

            Object obj = JSONValue.parse(s);
            JSONArray array = (JSONArray) obj;
            ArrayList<FloodlightStats> stats = new ArrayList<>();
            for (Object o : array) {
                JSONObject jsonObj = (JSONObject) o;
                FloodlightStats fs = new FloodlightStats();
                org.json.simple.JSONArray ipArray = (org.json.simple.JSONArray) jsonObj.get("ipv4");
                if (!ipArray.isEmpty()) {
                    fs.ip = (String) ipArray.get(0);
                } else {
                    continue;
                }

                JSONArray attachmentPointArray = (org.json.simple.JSONArray) jsonObj.get("attachmentPoint");
                org.json.simple.JSONObject attachmentPoint = (org.json.simple.JSONObject) attachmentPointArray.get(0);
                Object val = JSONValue.parse(attachmentPoint.toJSONString());
                JSONObject jsonPort = (JSONObject) val;
                fs.port = (Long) jsonPort.get("port");
                fs.switchDPID = (String) jsonPort.get("switchDPID");

                stats.add(getSwitchStats(fs, webResource));
            }

            return stats;
        } catch (Exception ex) {
            Logger.getLogger(NewQoSPlannerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public Map<String, FloodlightStats> getStatsMap() {
       
        WebResource webResource = client.resource(uri);

            WebResource res = webResource.path("wm").path("device/");
            String s = res.get(String.class);

            Object obj = JSONValue.parse(s);
            JSONArray array = (JSONArray) obj;
            Map<String, FloodlightStats> statsMap = new HashMap<>();
            for (Object o : array) {
                JSONObject jsonObj = (JSONObject) o;
                FloodlightStats fs = new FloodlightStats();
                org.json.simple.JSONArray ipArray = (org.json.simple.JSONArray) jsonObj.get("ipv4");
                if (!ipArray.isEmpty()) {
                    fs.ip = (String) ipArray.get(0);
                } else {
                    continue;
                }

                JSONArray attachmentPointArray = (org.json.simple.JSONArray) jsonObj.get("attachmentPoint");
                org.json.simple.JSONObject attachmentPoint = (org.json.simple.JSONObject) attachmentPointArray.get(0);
                Object val = JSONValue.parse(attachmentPoint.toJSONString());
                JSONObject jsonPort = (JSONObject) val;
                fs.port = (Long) jsonPort.get("port");
                fs.switchDPID = (String) jsonPort.get("switchDPID");
                
                statsMap.put(fs.ip, getSwitchStats(fs, webResource));
            }
        return statsMap;
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

    private FloodlightStats getSwitchStats(FloodlightStats fs, WebResource webResource) {
        WebResource res = webResource.path("wm").path("core").path("switch").path(fs.switchDPID).path("port").path("json");
        String s = res.get(String.class);
        s = s.substring(27, s.indexOf("]}"));
        s += "]";
        org.json.simple.JSONArray statJVal = (org.json.simple.JSONArray) JSONValue.parse(s);

        for (Object sjo : statJVal) {
            JSONObject jsonOb2j = (JSONObject) sjo;
            java.lang.Long portNumber = (java.lang.Long) jsonOb2j.get("portNumber");
            if (portNumber == fs.port) {
                fs.receiveOverrunErrors = (java.lang.Long) jsonOb2j.get("receiveOverrunErrors");
                fs.transmitErrors = (java.lang.Long) jsonOb2j.get("transmitErrors");
                fs.receiveDropped = (java.lang.Long) jsonOb2j.get("receiveDropped");
                fs.receiveErrors = (java.lang.Long) jsonOb2j.get("receiveErrors");
                fs.receiveFrameErrors = (java.lang.Long) jsonOb2j.get("receiveFrameErrors");
                fs.receiveCRCErrors = (java.lang.Long) jsonOb2j.get("receiveCRCErrors");
                fs.collisions = (java.lang.Long) jsonOb2j.get("collisions");
                fs.transmitBytes = (java.lang.Long) jsonOb2j.get("transmitBytes");
                fs.transmitPackets = (java.lang.Long) jsonOb2j.get("transmitPackets");
                fs.receivePackets = (java.lang.Long) jsonOb2j.get("receivePackets");
                fs.transmitDropped = (java.lang.Long) jsonOb2j.get("transmitDropped");
                break;
            }
        }
        return fs;
    }

//    @Data
//    @XmlRootElement(name = "FloodlightStats")
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class FloodlightStats {
//    }
//    @XmlRootElement(name = "00:00:c2:b3:aa:aa:2d:41")
//    @XmlAccessorType(XmlAccessType.NONE)
//    public static class FloodlightStats {
//
//        @XmlElement(name = "portNumber")
//        public int portNumber;
//        @XmlElement(name = "receivePackets")
//        public long receivePackets;
//        @XmlElement(name = "transmitPackets")
//        public long transmitPackets;
//        @XmlElement(name = "receiveBytes")
//        public long receiveBytes;
//        @XmlElement(name = "transmitBytes")
//        public long transmitBytes;
//        @XmlElement(name = "transmitDropped")
//        public long transmitDropped;
//        @XmlElement(name = "receiveErrors")
//        public long receiveErrors;
//        @XmlElement(name = "receiveDropped")
//        public long receiveDropped;
//        @XmlElement(name = "transmitErrors")
//        public long transmitErrors;
//        @XmlElement(name = "receiveFrameErrors")
//        public long receiveFrameErrors;
//        @XmlElement(name = "receiveOverrunErrors")
//        public long receiveOverrunErrors;
//        @XmlElement(name = "receiveCRCErrors")
//        public long receiveCRCErrors;
//        @XmlElement(name = "collisions")
//        public int collisions;
//        public long totalPackets;
//    }
    public static class FloodlightStats {

        public long receivePackets;
        public long transmitPackets;
        public long collisions;
        public long receiveCRCErrors;
        public long receiveDropped;
        public long receiveErrors;
        public long receiveFrameErrors;
        public long receiveOverrunErrors;
        public long transmitBytes;
        public long transmitDropped;
        public long transmitErrors;
        public long receiveBytes;
        public String ip;
        public Long port;
        public String switchDPID;

        public FloodlightStats() {
        }
    }
}
