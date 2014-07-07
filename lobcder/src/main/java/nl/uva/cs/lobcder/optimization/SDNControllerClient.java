/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 *
 * @author S. Koulouzis
 */
public class SDNControllerClient {

    private final Client client;
    private String uri;
    private int floodlightPort = 8080;
    private int sflowRTPrt = 8008;
    private List<Switch> switches;
    private Map<String, String> networkEntitySwitchMap;
    private HashMap<String, Integer> sFlowHostPortMap;

    public SDNControllerClient(String uri) throws IOException {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);


        this.uri = uri;
    }

    public List<FloodlightStats> getStats() {
//        try {
//            WebResource webResource = client.resource(uri + ":" + floodlightPort);
//
//            WebResource res = webResource.path("wm").path("device/");
//            String s = res.get(String.class);
//
//            Object obj = JSONValue.parse(s);
//            JSONArray array = (JSONArray) obj;
//            ArrayList<FloodlightStats> stats = new ArrayList<>();
//            for (Object o : array) {
//                JSONObject jsonObj = (JSONObject) o;
//                FloodlightStats fs = new FloodlightStats();
//                org.json.simple.JSONArray ipArray = (org.json.simple.JSONArray) jsonObj.get("ipv4");
//                if (!ipArray.isEmpty()) {
//                    fs.ip = (String) ipArray.get(0);
//                } else {
//                    continue;
//                }
//
//                JSONArray attachmentPointArray = (org.json.simple.JSONArray) jsonObj.get("attachmentPoint");
//                org.json.simple.JSONObject attachmentPoint = (org.json.simple.JSONObject) attachmentPointArray.get(0);
//                Object val = JSONValue.parse(attachmentPoint.toJSONString());
//                JSONObject jsonPort = (JSONObject) val;
//                fs.port = (Long) jsonPort.get("port");
//                fs.switchDPID = (String) jsonPort.get("switchDPID");
//
//                stats.add(getSwitchStats(fs, webResource));
//            }
//
//            return stats;
//        } catch (Exception ex) {
//            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return null;
    }

    public Map<String, FloodlightStats> getStatsMap() {
//        WebResource webResource = client.resource(uri + ":" + floodlightPort);
//
//        WebResource res = webResource.path("wm").path("device/");
//        String s = res.get(String.class);
//
//        Object obj = JSONValue.parse(s);
//        JSONArray array = (JSONArray) obj;
//        Map<String, FloodlightStats> statsMap = new HashMap<>();
//        for (Object o : array) {
//            JSONObject jsonObj = (JSONObject) o;
//            FloodlightStats fs = new FloodlightStats();
//            org.json.simple.JSONArray ipArray = (org.json.simple.JSONArray) jsonObj.get("ipv4");
//            if (!ipArray.isEmpty()) {
//                fs.ip = (String) ipArray.get(0);
//            } else {
//                continue;
//            }
//
//            JSONArray attachmentPointArray = (org.json.simple.JSONArray) jsonObj.get("attachmentPoint");
//            org.json.simple.JSONObject attachmentPoint = (org.json.simple.JSONObject) attachmentPointArray.get(0);
//            Object val = JSONValue.parse(attachmentPoint.toJSONString());
//            JSONObject jsonPort = (JSONObject) val;
//            fs.port = (Long) jsonPort.get("port");
//            fs.switchDPID = (String) jsonPort.get("switchDPID");
//
//            statsMap.put(fs.ip, getSwitchStats(fs, webResource));
//        }
        return null;
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
//        WebResource res = webResource.path("wm").path("core").path("switch").path(fs.switchDPID).path("port").path("json");
//        String s = res.get(String.class);
//        s = s.substring(27, s.indexOf("]}"));
//        s += "]";
//        org.json.simple.JSONArray statJVal = (org.json.simple.JSONArray) JSONValue.parse(s);
//
//        for (Object sjo : statJVal) {
//            JSONObject jsonOb2j = (JSONObject) sjo;
//            java.lang.Long portNumber = (java.lang.Long) jsonOb2j.get("portNumber");
//            if (portNumber == fs.port) {
//                fs.receiveOverrunErrors = (java.lang.Long) jsonOb2j.get("receiveOverrunErrors");
//                fs.transmitErrors = (java.lang.Long) jsonOb2j.get("transmitErrors");
//                fs.receiveDropped = (java.lang.Long) jsonOb2j.get("receiveDropped");
//                fs.receiveErrors = (java.lang.Long) jsonOb2j.get("receiveErrors");
//                fs.receiveFrameErrors = (java.lang.Long) jsonOb2j.get("receiveFrameErrors");
//                fs.receiveCRCErrors = (java.lang.Long) jsonOb2j.get("receiveCRCErrors");
//                fs.collisions = (java.lang.Long) jsonOb2j.get("collisions");
//                fs.transmitBytes = (java.lang.Long) jsonOb2j.get("transmitBytes");
//                fs.transmitPackets = (java.lang.Long) jsonOb2j.get("transmitPackets");
//                fs.receivePackets = (java.lang.Long) jsonOb2j.get("receivePackets");
//                fs.transmitDropped = (java.lang.Long) jsonOb2j.get("transmitDropped");
//                break;
//            }
//        }
        return null;
    }

    public String getLowestCostWorker(String dest, Set<String> sources) {
        SimpleWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        dest = "192.168.100.1";

        List<NetworkEntity> destinationEntityArray = getNetworkEntity(dest);
        NetworkEntity destinationEntity = destinationEntityArray.get(0);
        String ipv4 = null;
        if (!destinationEntity.ipv4.isEmpty()) {
            ipv4 = destinationEntity.ipv4.get(0);
        }
        graph.addVertex(ipv4);

        String mac = destinationEntity.mac.get(0);
        String switchDPID = destinationEntity.attachmentPoint.get(0).switchDPID;

        int port = destinationEntity.attachmentPoint.get(0).port;
        graph.addVertex(switchDPID);

        DefaultWeightedEdge e1 = graph.addEdge(ipv4, switchDPID);
        graph.setEdgeWeight(e1, getCost(ipv4, switchDPID));

        List<NetworkEntity> sourceEntityArray = getNetworkEntity(sources);

        for (NetworkEntity ne : sourceEntityArray) {

            if (!ne.ipv4.isEmpty()) {
                ipv4 = ne.ipv4.get(0);
            }
            graph.addVertex(ipv4);

            mac = ne.mac.get(0);
            switchDPID = ne.attachmentPoint.get(0).switchDPID;
            port = ne.attachmentPoint.get(0).port;
            graph.addVertex(switchDPID);

            DefaultWeightedEdge e2 = graph.addEdge(ipv4, switchDPID);
            graph.setEdgeWeight(e2, getCost(ipv4, switchDPID));
        }

        List<Link> links = getSwitchLinks();
        for (Link l : links) {
            graph.addVertex(l.srcSwitch);
            graph.addVertex(l.dstSwitch);
            DefaultWeightedEdge e3 = graph.addEdge(l.srcSwitch, l.dstSwitch);
            graph.setEdgeWeight(e3, getCost(l.srcSwitch, l.dstSwitch));
        }


        double cost = Double.MAX_VALUE;
        List<DefaultWeightedEdge> shortestPath = null;

        for (String s : sources) {
            if (graph.containsVertex(dest) && graph.containsVertex(s)) {
                List<DefaultWeightedEdge> shorPath = DijkstraShortestPath.findPathBetween(graph, s, dest);
                double w = 0;
                for (DefaultWeightedEdge e : shorPath) {
                    w += graph.getEdgeWeight(e);
                }
                if (w <= cost) {
                    cost = w;
                    shortestPath = shorPath;
                }
            }
        }
        DefaultWeightedEdge e = shortestPath.get(0);
        String[] workerSwitch = e.toString().split(" : ");
        String worker = workerSwitch[0].substring(1);

        return worker;
    }

    private List<NetworkEntity> getNetworkEntity(String address) {
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
        WebResource res = null;
        if (address.contains(".")) {
            // http://145.100.133.131:8080/wm/device/?ipv4=192.168.100.1
            res = webResource.path("wm").path("device/").queryParam("ipv4", address);
        } else {
            // http://145.100.133.131:8080/wm/device/?mac=fe:16:3e:00:26:b1
            res = webResource.path("wm").path("device/").queryParam("mac", address);
        }
        return res.get(new GenericType<List<NetworkEntity>>() {
        });
    }

    private List<NetworkEntity> getNetworkEntity(Set<String> sources) {
        List<NetworkEntity> entities = new ArrayList<>();
        for (String e : sources) {
            entities.addAll(getNetworkEntity(e));
        }
        return entities;
    }

    private List<Link> getSwitchLinks() {
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
        WebResource res = webResource.path("wm").path("topology").path("links").path("json");
        return res.get(new GenericType<List<Link>>() {
        });
    }

    private double getCost(String v1, String v2) {

        if (sFlowHostPortMap == null) {
            sFlowHostPortMap = new HashMap<>();
        }
        if (v1.contains(":") && v2.contains(":")) {
            String switch1IP = getSwitchIPFromDPI(v1);
            String switch2IP = getSwitchIPFromDPI(v2);

            List<Flow> flows = getAgentFlows(switch1IP);
            for (Flow f : flows) {
                String[] keys = f.flowKeys.split(",");
                String from = keys[0];
                String to = keys[1];
                if (!isAttached(from, v1) && isAttached(to, v1)) {
                    Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "Switch: " + switch1IP + " -> " + f.dataSource);
                    break;
                }

            }
        } else {
            String switchIP = null;
            String hostIP = null;
            if (v1.contains(".")) {
                switchIP = getSwitchIPFromHostIP(v1);
                hostIP = v1;
            } else {
                switchIP = getSwitchIPFromHostIP(v2);
                hostIP = v2;
            }

            if (!sFlowHostPortMap.containsKey(hostIP)) {
                List<Flow> flows = getAgentFlows(switchIP);
                for (Flow f : flows) {
                    String[] keys = f.flowKeys.split(",");
                    if (keys[0].equals(hostIP)) {
                        sFlowHostPortMap.put(hostIP, f.dataSource);
                        break;
                    }
                }
            }
//        TT = [({MTU} / {bps}) + RTT] * [ {MTU}/{FS}]
            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "Host: " + hostIP + " is attached to: " + switchIP + " port: " + sFlowHostPortMap.get(hostIP));
        }

        return 1.0;
    }

    private Map<String, Integer> getifNameOpenFlowPortNumberMap(String dpi) {
        HashMap<String, Integer> ifNamePortMap = new HashMap<>();
        List<Switch> sw = getSwitches();
        for (Switch s : sw) {
            if (s.dpid.equals(dpi)) {
                List<Port> ports = s.ports;
                for (Port p : ports) {
                    ifNamePortMap.put(p.name, p.portNumber);
                }
                break;
            }
        }
        return ifNamePortMap;
    }

    private List<Switch> getSwitches() {
        if (switches == null) {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = webResource.path("wm").path("core").path("controller").path("switches").path("json");
            switches = res.get(new GenericType<List<Switch>>() {
            });
        }
        return switches;
    }

    private String getSwitchIPFromHostIP(String address) {
        if (networkEntitySwitchMap == null) {
            networkEntitySwitchMap = new HashMap<>();
        }
        if (!networkEntitySwitchMap.containsKey(address)) {
            List<NetworkEntity> ne = getNetworkEntity(address);
            String dpi = ne.get(0).attachmentPoint.get(0).switchDPID;
            for (Switch sw : getSwitches()) {
                if (sw.dpid.equals(dpi)) {
                    String ip = sw.inetAddress.split(":")[0].substring(1);
                    networkEntitySwitchMap.put(address, ip);
                    break;
                }
            }
        }

        return networkEntitySwitchMap.get(address);
    }

    private List<Flow> getAgentFlows(String switchIP) {
        List<Flow> agentFlows = new ArrayList<>();
        for (Flow f : getAllFlows()) {
            if (f.agent.equals(switchIP)) {
                agentFlows.add(f);
            }
        }
        return agentFlows;
    }

    private List<Flow> getAllFlows() {
        WebResource webResource = client.resource(uri + ":" + sflowRTPrt);
        WebResource res = webResource.path("flows").path("json");
        return res.get(new GenericType<List<Flow>>() {
        });
    }

    private boolean isAttached(String from, String dpi) {
        for (NetworkEntity ne : getNetworkEntity(from)) {
            for (AttachmentPoint ap : ne.attachmentPoint) {
                if (ap.switchDPID.equals(dpi)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getSwitchIPFromDPI(String dpi) {
        for (Switch s : getSwitches()) {
            if (s.dpid.equals(dpi)) {
                return s.inetAddress.split(":")[0].substring(1);
            }
        }
        return null;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Flow {

        @XmlElement(name = "agent")
        String agent;
        @XmlElement(name = "dataSource")
        int dataSource;
        @XmlElement(name = "end")
        String end;
        @XmlElement(name = "flowID")
        int flowID;
        @XmlElement(name = "flowKeys")
        String flowKeys;
        @XmlElement(name = "name")
        String name;
        @XmlElement(name = "start")
        long start;
        @XmlElement(name = "value")
        double value;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NetworkEntity {

        @XmlElement(name = "entityClass")
        String entityClass;
        @XmlElement(name = "lastSeen")
        String lastSeen;
        @XmlElement(name = "ipv4")
        List<String> ipv4;
        @XmlElement(name = "vlan")
        List<String> vlan;
        @XmlElement(name = "mac")
        List<String> mac;
        @XmlElement(name = "attachmentPoint")
        List<AttachmentPoint> attachmentPoint;
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

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Switch {

        @XmlElement(name = "actions")
        int actions;
        @XmlElement(name = "attributes")
        Attributes attributes;
        @XmlElement(name = "ports")
        List<Port> ports;
        @XmlElement(name = "buffers")
        int buffers;
        @XmlElement(name = "description")
        Description description;
        @XmlElement(name = "capabilities")
        int capabilities;
        @XmlElement(name = "inetAddress")
        String inetAddress;
        @XmlElement(name = "connectedSince")
        long connectedSince;
        @XmlElement(name = "dpid")
        String dpid;
        @XmlElement(name = "harole")
        String harole;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Description {

        @XmlElement(name = "software")
        String software;
        @XmlElement(name = "hardware")
        String hardware;
        @XmlElement(name = "manufacturer")
        String manufacturer;
        @XmlElement(name = "serialNum")
        String serialNum;
        @XmlElement(name = "datapath")
        String datapath;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Port {

        @XmlElement(name = "portNumber")
        int portNumber;
        @XmlElement(name = "hardwareAddress")
        String hardwareAddress;
        @XmlElement(name = "name")
        String name;
        @XmlElement(name = "config")
        int config;
        @XmlElement(name = "state")
        int state;
        @XmlElement(name = "currentFeatures")
        int currentFeatures;
        @XmlElement(name = "advertisedFeatures")
        int advertisedFeatures;
        @XmlElement(name = "supportedFeatures")
        int supportedFeatures;
        @XmlElement(name = "peerFeatures")
        int peerFeatures;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Attributes {

        @XmlElement(name = "supportsOfppFlood")
        boolean supportsOfppFlood;
        @XmlElement(name = "supportsNxRole")
        boolean supportsNxRole;
        @XmlElement(name = "FastWildcards")
        int fastWildcards;
        @XmlElement(name = "supportsOfppTable")
        boolean supportsOfppTable;
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
