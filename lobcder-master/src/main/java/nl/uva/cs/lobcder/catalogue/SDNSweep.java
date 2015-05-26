/*
 * Copyright 2014 S. Koulouzis.
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
package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.optimization.SDNControllerClient;
import nl.uva.cs.lobcder.rest.wrappers.AttachmentPoint;
import nl.uva.cs.lobcder.rest.wrappers.Link;
import nl.uva.cs.lobcder.rest.wrappers.NetworkEntity;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class SDNSweep implements Runnable {

    private static SimpleWeightedGraph<String, DefaultWeightedEdge> graph;
    private final DataSource datasource;
    private final String uri;
    private final int floodlightPort = 8080;
    private final int sflowRTPrt = 8008;
    private final Client client;
    private static Map<String, NetworkEntity> networkEntitesCache;
    @Getter
    private static List<Link> switchLinks;
    private static Map<String, Switch> switches;
//    private static List<Switch> switches;
    public static long interval = 800;
    @Getter
    private static Map<String, Double> statsMap;
    public static final String[] METRIC_NAMES = new String[]{"collisions",
        "receiveBytes", "receiveCRCErrors", "receiveDropped", "receiveErrors",
        "receiveFrameErrors", "receiveOverrunErrors", "receivePackets",
        "transmitBytes", "transmitDropped", "transmitErrors", "transmitPackets"};
    @Getter
    private static Map<String, Double> collisionsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> receiveBytesMap = new HashMap<>();
    @Getter
    private static Map<String, Double> receiveCRCErrorsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> receiveDroppedMap = new HashMap<>();
    @Getter
    private static Map<String, Double> receiveErrorsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> receiveFrameErrorsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> receiveOverrunErrorsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> receivePacketsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> transmitBytesMap = new HashMap<>();
    @Getter
    private static Map<String, Double> transmitDroppedMap = new HashMap<>();
    @Getter
    private static Map<String, Double> transmitErrorsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> transmitPacketsMap = new HashMap<>();
    @Getter
    private static Map<String, OFlow> oFlowsMap = new HashMap<>();
    @Getter
    private static Map<String, Double> averageLinkUsageMap = new HashMap<>();
    private long iterations = 1;
    private static boolean flowPushed = true;
    private static boolean arpFlowPushed = false;
    private static String topologyURL;
    private static Document flukesTopology;
    private final SDNControllerClient sdnCC;

    public SDNSweep(DataSource datasource) throws IOException, ParserConfigurationException, SAXException {
        this.datasource = datasource;
        uri = PropertiesHelper.getSDNControllerURL();
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);
        topologyURL = PropertiesHelper.getTopologyURL();
        if (topologyURL != null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            flukesTopology = db.parse(new URL(topologyURL).openStream());
        }
        sdnCC = new SDNControllerClient(uri);
    }

    private void init() throws InterruptedException, IOException {
        getAllSwitches();
        getAllNetworkEntites();
        getAllSwitchLinks();
        if (!flowPushed) {
//            pushFlowIntoOnePort();
        }
        if (!arpFlowPushed) {
            pushARPFlow();
            arpFlowPushed = true;
        }
    }

    public static NetworkEntity getNetworkEntity(String dest) throws IOException {
        return networkEntitesCache.get(dest);
    }

    @Override
    public void run() {
        try {
            iterations++;
            init();
            updateMetrics();
        } catch (IOException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateMetrics() throws IOException, InterruptedException {
        for (Switch sw : getAllSwitches()) {
            List<FloodlightStats> stats1 = getFloodlightPortStats(sw.dpid);
            Thread.sleep(interval);
            List<FloodlightStats> stats2 = getFloodlightPortStats(sw.dpid);

            for (int i = 0; i < stats1.size(); i++) {
//                for(String mn  : METRIC_NAMES){
//                    String key = mn+"-"+sw.dpid + "-" + stats1.get(i).portNumber;
//                }
                String key = sw.dpid + "-" + stats1.get(i).portNumber;


                Double val = collisionsMap.get(key);
                Double oldValue = ((val == null) ? 1.0 : val);
                Double newValue = Double.valueOf(stats2.get(i).collisions - stats1.get(i).collisions);
                newValue = (newValue + oldValue) / 2.0;
                collisionsMap.put(key, newValue);

                val = receiveBytesMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveBytes - stats1.get(i).receiveBytes);
                Double averageLinkUsage = averageLinkUsageMap.get(key);

                if ((newValue / interval) > 200) {
                    long durationSeconds = interval * iterations;

                    if (averageLinkUsage == null) {
                        averageLinkUsageMap.put(key, Double.valueOf(durationSeconds));
                    } else {
                        //$d_{new} = α ∗ d_{old} + (1 - α ) ∗ d_{sample}$ 
                        //with α ∈ [0 , 1] being the weighting factor, d_{sample} 
                        //being the new sample, d_{old} the current metric value 
                        //and d_{new} the newly calculated value. In fact, 
                        //this calculation implements a discrete low pass filter
                        double a = 0.5;
                        averageLinkUsage = a * averageLinkUsage + (1 - a) * durationSeconds;
//                        averageLinkUsage = (averageLinkUsage + durationSeconds) / 2.0;
                        averageLinkUsageMap.put(key, averageLinkUsage);
                    }
                }
                val = ((newValue > oldValue) ? newValue : oldValue);
                receiveBytesMap.put(key, val);


                val = transmitBytesMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitBytes - stats1.get(i).transmitBytes);
                if ((newValue / interval) > 200) {
                    long durationSeconds = interval * iterations;
                    if (averageLinkUsage == null) {
                        averageLinkUsageMap.put(key, Double.valueOf(durationSeconds));
                    } else {
                        //$d_{new} = α ∗ d_{old} + (1 - α ) ∗ d_{sample}$ 
                        //with α ∈ [0 , 1] being the weighting factor, d_{sample} 
                        //being the new sample, d_{old} the current metric value 
                        //and d_{new} the newly calculated value. In fact, 
                        //this calculation implements a discrete low pass filter
                        double a = 0.5;
                        averageLinkUsage = a * averageLinkUsage + (1 - a) * durationSeconds;
//                        averageLinkUsage = (averageLinkUsage + durationSeconds) / 2.0;
                        averageLinkUsageMap.put(key, averageLinkUsage);
                    }
                }

                val = ((newValue > oldValue) ? newValue : oldValue);
                transmitBytesMap.put(key, val);


                val = receiveCRCErrorsMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveCRCErrors - stats1.get(i).receiveCRCErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveCRCErrorsMap.put(key, newValue);


                val = receiveDroppedMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveDropped - stats1.get(i).receiveDropped);
                newValue = (newValue + oldValue) / 2.0;
                receiveDroppedMap.put(key, newValue);



                val = receiveErrorsMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveErrors - stats1.get(i).receiveErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveErrorsMap.put(key, newValue);


                val = receiveFrameErrorsMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveFrameErrors - stats1.get(i).receiveFrameErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveErrorsMap.put(key, newValue);

                val = receiveOverrunErrorsMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveOverrunErrors - stats1.get(i).receiveOverrunErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveOverrunErrorsMap.put(key, newValue);



                val = receivePacketsMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receivePackets - stats1.get(i).receivePackets);
                val = ((newValue > oldValue) ? newValue : oldValue);
                receivePacketsMap.put(key, val);


                val = transmitDroppedMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitDropped - stats1.get(i).transmitDropped);
                newValue = (newValue + oldValue) / 2.0;
                transmitDroppedMap.put(key, newValue);


                val = transmitErrorsMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitErrors - stats1.get(i).transmitErrors);
                newValue = (newValue + oldValue) / 2.0;
                transmitErrorsMap.put(key, newValue);

                val = transmitPacketsMap.get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitPackets - stats1.get(i).transmitPackets);
                val = ((newValue > oldValue) ? newValue : oldValue);
                transmitPacketsMap.put(key, val);

            }
        }
//        for (Switch sw : getAllSwitches()) {
//            List<OFlow> flows = getOflow(sw.dpid);
//            for (OFlow f : flows) {
//                if (f != null) {
//                    String key = sw.dpid + "-" + f.match.inputPort;
//                    f.timeStamp = System.currentTimeMillis();
//                    oFlowsMap.put(key, f);
//
//                    Double val = receiveBytesMap.get(key);
//                    double oldValue = (val == null) ? 1.0 : val;
//                    Double newValue = Double.valueOf(f.byteCount / f.durationSeconds * 1.0);
//                    val = ((newValue > oldValue) ? newValue : oldValue);
//                    receiveBytesMap.put(key, val);
//
//                    val = receivePacketsMap.get(key);
//                    oldValue = (val == null) ? 1.0 : val;
//                    newValue = Double.valueOf(f.packetCount / f.durationSeconds * 1.0);
//                    val = ((newValue > oldValue) ? newValue : oldValue);
//                    receivePacketsMap.put(key, val);
//                    Double averageLinkUsage = averageLinkUsageMap.get(key);
//                    if (averageLinkUsage == null) {
//                        averageLinkUsageMap.put(key, Double.valueOf(f.durationSeconds));
//                    } else {
//                        //$d_{new} = α ∗ d_{old} + (1 - α ) ∗ d_{sample}$ 
//                        //with α ∈ [0 , 1] being the weighting factor, d_{sample} 
//                        //being the new sample, d_{old} the current metric value 
//                        //and d_{new} the newly calculated value. In fact, 
//                        //this calculation implements a discrete low pass filter
//                        double a = 0.5;
//                        averageLinkUsage = a * averageLinkUsage + (1 - a) * f.durationSeconds;
//                        averageLinkUsage = (averageLinkUsage + f.durationSeconds) / 2.0;
//                        averageLinkUsageMap.put(key, averageLinkUsage);
//                    }
//                }
//            }
//        }

    }

    private List<FloodlightStats> getFloodlightPortStats(String dpi) throws IOException {
        //http://145.100.133.130:8080/wm/core/switch/00:00:4e:cd:a6:8d:c9:44/port/json
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
        WebResource res = webResource.path("wm").path("core").path("switch").path(dpi).path("port").path("json");
        String output = res.get(String.class);
//        String out = output.substring(27, output.length() - 1);
        String out = output.substring(output.indexOf("[{"), output.length() - 1);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(out, mapper.getTypeFactory().constructCollectionType(List.class, FloodlightStats.class));
    }

    private List<OFlow> getOflow(String dpi) throws IOException {
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
        WebResource res = webResource.path("wm").path("core").path("switch").path(dpi).path("flow").path("json");
        String output = res.get(String.class);
        String out = output.substring(27, output.length() - 1);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(out, mapper.getTypeFactory().constructCollectionType(List.class, OFlow.class));
    }

    private Collection<NetworkEntity> getAllNetworkEntites() {
        if (networkEntitesCache == null) {
            networkEntitesCache = new HashMap<>();
        }
        if (networkEntitesCache.isEmpty() || iterations % 5 == 0) {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = null;
            // http://145.100.133.131:8080/wm/device/?getIpv4=192.168.100.1
            res = webResource.path("wm").path("device/");
            List<NetworkEntity> neList = res.get(new GenericType<List<NetworkEntity>>() {
            });
            String ipKey = null;
            for (NetworkEntity ne : neList) {
                if (!ne.getIpv4().isEmpty()) {
                    ipKey = ne.getIpv4().get(0);
                    for (String ip : ne.getIpv4()) {
                        if (!ip.startsWith("0")) {
                            ipKey = ip;
                            break;
                        }
                    }
                }
                String swIPFromFlukes = null;
                if (!ne.getIpv4().isEmpty() && !networkEntitesCache.containsKey(ipKey)) {
                    if (flukesTopology != null) {
                        swIPFromFlukes = findAttachedSwitch(ipKey);
                        String swDPI = switches.get(swIPFromFlukes).dpid;
                        List<AttachmentPoint> ap = new ArrayList<>();
                        for (AttachmentPoint p : ne.getAttachmentPoint()) {
                            if (p.getSwitchDPID().equals(swDPI)) {
                                ap.add(p);
                                break;
                            }
                        }
                        ne.setAttachmentPoint(ap);
                    }
                    networkEntitesCache.put(ipKey, ne);
                }
            }
        }
        return networkEntitesCache.values();
    }

    private String findAttachedSwitch(String ipKey) {
        NodeList desc = flukesTopology.getElementsByTagName("rdf:Description");
        String ipForTop = ipKey.replaceAll("\\.", "-");
        String linkNum = null;
        String swName = null;
        String swIP = null;

        for (int i = 0; i < desc.getLength(); i++) {
            Node n = desc.item(i);
            NamedNodeMap att = n.getAttributes();
            for (int j = 0; j < att.getLength(); j++) {
                Node n2 = att.item(j);
                if (n2.getNodeValue().contains(ipForTop) && n2.getNodeValue().contains("Link")) {
                    linkNum = n2.getNodeValue().substring(n2.getNodeValue().lastIndexOf("#Link") + 1);
                    linkNum = linkNum.substring(0, linkNum.indexOf("-"));
                    break;
                }
            }
            if (linkNum != null) {
                break;
            }
        }
        if (linkNum != null) {
            for (int i = 0; i < desc.getLength(); i++) {
                Node n = desc.item(i);
                NamedNodeMap att = n.getAttributes();
                for (int j = 0; j < att.getLength(); j++) {
                    Node n2 = att.item(j);
                    if (n2.getNodeValue().contains(linkNum) && n2.getNodeValue().contains("Switch")) {
                        swName = n2.getNodeValue().substring(n2.getNodeValue().lastIndexOf(linkNum) + linkNum.length() + 1);
//                        Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, " swName: " + swName);
                        break;
                    }
                }
                if (swName != null) {
                    break;
                }
            }
        }
        if (swName != null) {
            for (int i = 0; i < desc.getLength(); i++) {
                Node n = desc.item(i);
                NamedNodeMap att = n.getAttributes();
                for (int j = 0; j < att.getLength(); j++) {
                    Node n2 = att.item(j);

                    if (n2.getNodeValue().contains(swName + "-ip")) {
                        swIP = n2.getNodeValue().substring(n2.getNodeValue().lastIndexOf(swName + "-ip") + 1 + (swName + "ip-").length());
//                        Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, " swIP: " + swIP);
                        break;
                    }
                }
                if (swIP != null) {
                    break;
                }
            }
        }
        if (swIP != null) {
            return swIP.replaceAll("-", "\\.");
        }
        return null;
    }

    public void pushFlows(List<String> rules) {
        WebResource webResource = client.resource(uri + ":" + floodlightPort).path("wm").path("staticflowentrypusher").path("json");
        for (String r : rules) {
            ClientResponse response = webResource.post(ClientResponse.class, r);
            Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, r);
            Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, response.toString());
        }
    }

    private List<Link> getAllSwitchLinks() {
        if (switchLinks == null) {
            switchLinks = new ArrayList<>();
        }
        if (switchLinks.isEmpty() || iterations % 5 == 0) {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = webResource.path("wm").path("topology").path("links").path("json");
            switchLinks = res.get(new GenericType<List<Link>>() {
            });
            res = webResource.path("wm").path("topology").path("external-links").path("json");
            switchLinks.addAll(res.get(new GenericType<List<Link>>() {
            }));
        }
        return switchLinks;
    }

    private Collection<Switch> getAllSwitches() {
        if (switches == null) {
            switches = new HashMap<>();
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = webResource.path("wm").path("core").path("controller").path("switches").path("json");
            List<Switch> switchesList = res.get(new GenericType<List<Switch>>() {
            });
            for (Switch s : switchesList) {
                String swIP = s.inetAddress.substring(1, s.inetAddress.lastIndexOf(":"));
                switches.put(swIP, s);
            }
        }
        return switches.values();
    }

    public static Collection<Switch> getSwitches() {
        return switches.values();
    }

    private void pushARPFlow() throws InterruptedException, IOException {
        Iterator<Switch> swIter = getAllSwitches().iterator();
        List<String> arpFlows = new ArrayList<>();
        String dst = null;
        Set<String> sources = new HashSet<>();
        int count = 0;
        while (swIter.hasNext()) {
            count++;
            Switch sw = swIter.next();
            String flow = "{\"switch\":\"" + sw.dpid + "\", "
                    + "\"name\":\"arp" + sw.dpid + "\", "
                    + "\"ether-type\":\"0x0806\", "
                    + "\"actions\":\"output=flood\"}";
            arpFlows.add(flow);
            Collection<NetworkEntity> nes = getNetworkEntitiesForSwDPI(sw.dpid);
            Iterator<NetworkEntity> neIter = nes.iterator();
            while (neIter.hasNext()) {
                NetworkEntity ne = neIter.next();
                
                for (String mac : ne.getMac()) {
                    for (AttachmentPoint ap : ne.getAttachmentPoint()) {
                        flow = "{\"switch\": \"" + sw.dpid + "\", "
                                + "\"name\":\"flow-to-" + mac + "\", "
                                + "\"cookie\":\"0\", "
                                + "\"priority\":\"32768\", "
                                + "\"dst-mac\":\"" + mac + "\","
                                + "\"active\":\"true\", "
                                + "\"actions\":"
                                + "\"output=" + ap.getPort() + "\"}";
                        arpFlows.add(flow);
                    }
                }
            }
        }
        List<DefaultWeightedEdge> path = sdnCC.getShortestPath(dst, sources);
        sdnCC.pushFlow(path);
        pushFlows(arpFlows);
    }

    private void pushFlowIntoOnePort() {

        String s1 = getAllSwitches().iterator().next().dpid;// get(0).dpid;
        String s2 = getAllSwitches().iterator().next().dpid; //get(1).dpid;
        Number s1ToS2Port = 0;
        Number s2ToS1Port = 0;
        for (Link l : getAllSwitchLinks()) {
            if (l.srcSwitch.equals(s1)) {
                s1ToS2Port = l.srcPort;
                s2ToS1Port = l.dstPort;
                break;
            } else if (l.srcSwitch.equals(s2)) {
                s1ToS2Port = l.dstPort;
                s2ToS1Port = l.srcPort;
                break;
            }
        }

        List<String> s1Hosts = new ArrayList<>();
        List<String> s2Hosts = new ArrayList<>();

        for (NetworkEntity ne : getAllNetworkEntites()) {
            if (ne.getAttachmentPoint().get(0).getSwitchDPID().equals(s1)) {
                s1Hosts.add(ne.getIpv4().get(0));
            } else if (ne.getAttachmentPoint().get(0).getSwitchDPID().equals(s2)) {
                s2Hosts.add(ne.getIpv4().get(0));
            }
        }



        List<String> rules = new ArrayList<>();
        //s1 to s2
        for (String h1 : s1Hosts) {
            for (String h2 : s2Hosts) {
                String rule1To2 = "{\"switch\": \"" + s1 + "\", \"name\":\"" + h1 + "To" + h2 + "\", \"cookie\":\"0\", \"priority\":\"10\", "
                        + "\"src-mac\":\"" + networkEntitesCache.get(h1).getMac().get(0) + "\", \"ingress-port\":\"" + networkEntitesCache.get(h1).getAttachmentPoint().get(0).getPort() + "\", "
                        + "\"dst-mac\": \"" + networkEntitesCache.get(h2).getMac().get(0) + "\", \"active\":\"true\","
                        + "\"actions\":\"output=" + s1ToS2Port + "\"}";
                rules.add(rule1To2);
                Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, rule1To2);
            }
        }

        //s2 to s1
        for (String h1 : s2Hosts) {
            for (String h2 : s1Hosts) {
                String rule2To1 = "{\"switch\": \"" + s2 + "\", \"name\":\"" + h1 + "To" + h2 + "\", \"cookie\":\"0\", \"priority\":\"10\", "
                        + "\"src-mac\":\"" + networkEntitesCache.get(h1).getMac().get(0) + "\", \"ingress-port\":\"" + networkEntitesCache.get(h1).getAttachmentPoint().get(0).getPort() + "\", "
                        + "\"dst-mac\": \"" + networkEntitesCache.get(h2).getMac().get(0) + "\", \"active\":\"true\","
                        + "\"actions\":\"output=" + s2ToS1Port + "\"}";

                rules.add(rule2To1);
                Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, rule2To1);
            }
        }
        pushFlows(rules);
        flowPushed = true;
    }

    private Collection<NetworkEntity> getNetworkEntitiesForSwDPI(String dpid) {
        Iterator<NetworkEntity> iter = getAllNetworkEntites().iterator();
        Collection<NetworkEntity> nes = new ArrayList<>();
        while (iter.hasNext()) {
            NetworkEntity ne = iter.next();
            for (AttachmentPoint ap : ne.getAttachmentPoint()) {
                if (ap.getSwitchDPID().equals(dpid)) {
                    nes.add(ne);
                }
            }
        }
        return nes;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Ifpkts {

        @XmlElement(name = "agent")
        public String agent;
        @XmlElement(name = "dataSource")
        public int dataSource;
        @XmlElement(name = "lastUpdate")
        public long lastUpdate;
        /**
         * The lastUpdateMax and lastUpdateMin values indicate how long ago (in
         * milliseconds) the most recent and oldest updates
         */
        @XmlElement(name = "lastUpdateMax")
        public long lastUpdateMax;
        @XmlElement(name = "lastUpdateMin")
        /**
         * The metricN field in the query result indicates the number of data
         * sources that contributed to the summary metrics
         */
        public long lastUpdateMin;
        @XmlElement(name = "metricN")
        public int metricN;
        @XmlElement(name = "metricName")
        public String metricName;
        @XmlElement(name = "metricValue")
        public double value;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SFlow {

        @XmlElement(name = "agent")
        public String agent;
        @XmlElement(name = "dataSource")
        public int dataSource;
        @XmlElement(name = "end")
        public String end;
        @XmlElement(name = "flowID")
        public int flowID;
        @XmlElement(name = "flowKeys")
        public String flowKeys;
        @XmlElement(name = "name")
        public String name;
        @XmlElement(name = "start")
        public long start;
        @XmlElement(name = "value")
        public double value;
    }

//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class NetworkEntity {
//
//        @XmlElement(name = "entityClass")
//        public String entityClass;
//        @XmlElement(name = "lastSeen")
//        public String lastSeen;
//        @XmlElement(name = "getIpv4()")
//        public List<String> getIpv4();
//        @XmlElement(name = "vlan")
//        public List<String> vlan;
//        @XmlElement(name = "mac")
//        public List<String> mac;
//        @XmlElement(name = "getAttachmentPoint()")
//        public List<AttachmentPoint> getAttachmentPoint();
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class AttachmentPoint {
//
//        @XmlElement(name = "port")
//        public int port;
//        @XmlElement(name = "errorStatus")
//        public String errorStatus;
//        @XmlElement(name = "getSwitchDPID()")
//        public String getSwitchDPID();
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class Link {
//
//        @XmlElement(name = "src-switch")
//        public String srcSwitch;
//        @XmlElement(name = "src-port")
//        public int srcPort;
//        @XmlElement(name = "dst-switch")
//        public String dstSwitch;
//        @XmlElement(name = "dst-port")
//        public int dstPort;
//        @XmlElement(name = "type")
//        public String type;
//        @XmlElement(name = "direction")
//        public String direction;
//    }
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Switch {

        @XmlElement(name = "actions")
        public int actions;
        @XmlElement(name = "attributes")
        public Attributes attributes;
        @XmlElement(name = "ports")
        public List<Port> ports;
        @XmlElement(name = "buffers")
        public int buffers;
        @XmlElement(name = "description")
        public Description description;
        @XmlElement(name = "capabilities")
        public int capabilities;
        @XmlElement(name = "inetAddress")
        public String inetAddress;
        @XmlElement(name = "connectedSince")
        public long connectedSince;
        @XmlElement(name = "dpid")
        public String dpid;
        @XmlElement(name = "harole")
        public String harole;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Description {

        @XmlElement(name = "software")
        public String software;
        @XmlElement(name = "hardware")
        public String hardware;
        @XmlElement(name = "manufacturer")
        public String manufacturer;
        @XmlElement(name = "serialNum")
        public String serialNum;
        @XmlElement(name = "datapath")
        public String datapath;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Port {

        @XmlElement(name = "portNumber")
        public int portNumber;
        @XmlElement(name = "hardwareAddress")
        public String hardwareAddress;
        @XmlElement(name = "name")
        public String name;
        @XmlElement(name = "config")
        public int config;
        @XmlElement(name = "state")
        public int state;
        @XmlElement(name = "currentFeatures")
        public int currentFeatures;
        @XmlElement(name = "advertisedFeatures")
        public int advertisedFeatures;
        @XmlElement(name = "supportedFeatures")
        public int supportedFeatures;
        @XmlElement(name = "peerFeatures")
        public int peerFeatures;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Attributes {

        @XmlElement(name = "supportsOfppFlood")
        public boolean supportsOfppFlood;
        @XmlElement(name = "supportsNxRole")
        public boolean supportsNxRole;
        @XmlElement(name = "FastWildcards")
        public int fastWildcards;
        @XmlElement(name = "supportsOfppTable")
        public boolean supportsOfppTable;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FloodlightStats {

        @JsonProperty("portNumber")
        public String portNumber;
        @JsonProperty("receivePackets")
        public long receivePackets;
        @JsonProperty("transmitPackets")
        public long transmitPackets;
        @JsonProperty("receiveBytes")
        public long receiveBytes;
        @JsonProperty("transmitBytes")
        public long transmitBytes;
        @JsonProperty("receiveDropped")
        public long receiveDropped;
        @JsonProperty("transmitDropped")
        public long transmitDropped;
        @JsonProperty("receiveErrors")
        public long receiveErrors;
        @JsonProperty("transmitErrors")
        public long transmitErrors;
        @JsonProperty("receiveFrameErrors")
        public long receiveFrameErrors;
        @JsonProperty("receiveOverrunErrors")
        public long receiveOverrunErrors;
        @JsonProperty("receiveCRCErrors")
        public long receiveCRCErrors;
        @JsonProperty("collisions")
        public long collisions;
    }

    public static class Actions {

        @JsonProperty("type")
        public String type;
        @JsonProperty("length")
        public int length;
        @JsonProperty("port")
        public int port;
        @JsonProperty("maxLength")
        public int maxLength;
        @JsonProperty("lengthU")
        public int lengthU;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Match {

        @JsonProperty("dataLayerDestination")
        public String dataLayerDestination;
        @JsonProperty("dataLayerSource")
        public String dataLayerSource;
        @JsonProperty("dataLayerType")
        public String dataLayerType;
        @JsonProperty("dataLayerVirtualLan")
        public int dataLayerVirtualLan;
        @JsonProperty("dataLayerVirtualLanPriorityCodePoint")
        public int dataLayerVirtualLanPriorityCodePoint;
        @JsonProperty("inputPort")
        public int inputPort;
        @JsonProperty("networkDestination")
        public String networkDestination;
        @JsonProperty("networkDestinationMaskLen")
        public int networkDestinationMaskLen;
        @JsonProperty("networkSourceMaskLen")
        public int networkSourceMaskLen;
        @JsonProperty("networkProtocol")
        public int networkProtocol;
        @JsonProperty("networkSource")
        public String networkSource;
        @JsonProperty("networkTypeOfService")
        public int networkTypeOfService;
        @JsonProperty("transportDestination")
        public int transportDestination;
        @JsonProperty("transportSource")
        public int transportSource;
        @JsonProperty("wildcards")
        public int wildcards;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OFlow {

        @JsonProperty("tableId")
        public int tableId;
        @JsonProperty("match")
        public Match match;
        @JsonProperty("durationSeconds")
        public int durationSeconds;
        @JsonProperty("durationNanoseconds")
        public long durationNanoseconds;
        @JsonProperty("priority")
        public int priority;
        @JsonProperty("idleTimeout")
        public int idleTimeout;
        @JsonProperty("hardTimeout")
        public int hardTimeout;
        @JsonProperty("cookie")
        public long cookie;
        @JsonProperty("packetCount")
        public long packetCount;
        @JsonProperty("byteCount")
        public long byteCount;
        @JsonProperty("actions")
        public List<Actions> actions;
        public long timeStamp;
    }
}
