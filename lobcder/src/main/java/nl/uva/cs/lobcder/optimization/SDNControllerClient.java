/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import com.google.common.collect.HashBiMap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author S. Koulouzis
 */
public class SDNControllerClient {

    private final Client client;
    private String uri;
    private int floodlightPort = 8080;
    private int sflowRTPrt = 8008;

    public SDNControllerClient(String uri) throws IOException {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);


        this.uri = uri;
    }

    public List<FloodlightStats> getStats() {
        try {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);

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
            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public Map<String, FloodlightStats> getStatsMap() {

        WebResource webResource = client.resource(uri + ":" + floodlightPort);

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
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
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

    public Map<String, ArrayList<String>> getPathsMap(String dest, List<String> sources) {
        dest = "fe:16:3e:00:26:b1";


        ArrayList<NetworkEntity> destinationEntityArray = getNetworkEntity(dest);

        Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "--------------destination-------------");
        for (NetworkEntity ne : destinationEntityArray) {
            ArrayList<String> ips = ne.ipv4;
            for (String ip : ips) {
                Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "ipv4: " + ip);
            }
            ArrayList<String> macs = ne.mac;
            for (String mac : macs) {
                Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "mac: " + mac);
            }
            ArrayList<AttachmentPoint> aps = ne.attachmentPoint;
            for (AttachmentPoint ap : aps) {
                String switchDPID = ap.switchDPID;
                int port = ap.port;
                Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "switchDPID: " + switchDPID + " port: " + port);
            }
        }

        sources = new ArrayList<>();
//        sources.add("fe:16:3e:00:26:b1");
        sources.add("fe:16:3e:00:57:9e");
        Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "--------------sources-------------");
        ArrayList<NetworkEntity> sourceEntityArray = getNetworkEntity(sources);

        for (NetworkEntity ne : sourceEntityArray) {
            ArrayList<String> ips = ne.ipv4;
            for (String ip : ips) {
                Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "ipv4: " + ip);
            }
            ArrayList<String> macs = ne.mac;
            for (String mac : macs) {
                Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "mac: " + mac);
            }
            ArrayList<AttachmentPoint> aps = ne.attachmentPoint;
            for (AttachmentPoint ap : aps) {
                String switchDPID = ap.switchDPID;
                int port = ap.port;
                Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "switchDPID: " + switchDPID + " port: " + port);
            }
        }

        ArrayList<Link> links = getSwitchLinks();
        for (Link l : links) {
            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "srcSwitch: " + l.srcSwitch + " srcPort: " + l.srcPort + " dstSwitch: " + l.dstSwitch + " dstPort:" + l.dstPort);
        }

        return null;
    }

    private ArrayList<NetworkEntity> getNetworkEntity(String dest) {
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
        WebResource res = null;
        if (dest.contains(".")) {
            // http://145.100.133.131:8080/wm/device/?ipv4=192.168.100.1
            res = webResource.path("wm").path("device/").queryParam("ipv4", dest);
        } else {
            // http://145.100.133.131:8080/wm/device/?mac=fe:16:3e:00:26:b1
            res = webResource.path("wm").path("device/").queryParam("mac", dest);
        }
        return res.get(new GenericType<ArrayList<NetworkEntity>>() {
        });
    }

    private ArrayList<NetworkEntity> getNetworkEntity(List<String> sources) {
        ArrayList<NetworkEntity> entities = new ArrayList<>();
        for (String e : sources) {
            entities.addAll(getNetworkEntity(e));
        }
        return entities;
    }

    private ArrayList<Link> getSwitchLinks() {
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
        WebResource res = webResource.path("wm").path("topology").path("links").path("json");
        return res.get(new GenericType<ArrayList<Link>>() {
        });
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NetworkEntity {

        @XmlElement(name = "entityClass")
        String entityClass;
        @XmlElement(name = "lastSeen")
        String lastSeen;
        @XmlElement(name = "ipv4")
        ArrayList<String> ipv4;
        @XmlElement(name = "vlan")
        ArrayList<String> vlan;
        @XmlElement(name = "mac")
        ArrayList<String> mac;
        @XmlElement(name = "attachmentPoint")
        ArrayList<AttachmentPoint> attachmentPoint;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AttachmentPoint {

        @XmlElement(name = "port")
        int port;
        @XmlElement(name = "errorStatus")
        String errorStatus;
        @XmlElement(name = "switchDPID")
        String switchDPID;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Link {

        @XmlElement(name = "src-switch")
        String srcSwitch;
        @XmlElement(name = "src-port")
        int srcPort;
        @XmlElement(name = "dst-switch")
        String dstSwitch;
        @XmlElement(name = "dst-port")
        int dstPort;
        @XmlElement(name = "type")
        String type;
        @XmlElement(name = "direction")
        String direction;
    }

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
