/*
 * Copyright 2014 S. Koulouzis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain aplha copy of the License at
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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class SDNSweep implements Runnable {

    private static SimpleWeightedGraph<String, DefaultWeightedEdge> graph;

    /**
     * @return the graph
     */
    public static SimpleWeightedGraph<String, DefaultWeightedEdge> getGraph() {
        return graph;
    }

    /**
     * @return the networkEntitesCache
     */
    public static Map<String, NetworkEntity> getNetworkEntitesCache() {
        return networkEntitesCache;
    }

    /**
     * @return the switchLinks
     */
    public static List<Link> getSwitchLinks() {
        return switchLinks;
    }

    /**
     * @return the interval
     */
    public static long getInterval() {
        return interval;
    }

    /**
     * @return the statsMap
     */
    public static Map<String, Double> getStatsMap() {
        return statsMap;
    }

    /**
     * @return the METRIC_NAMES
     */
    public static String[] getMETRIC_NAMES() {
        return METRIC_NAMES;
    }

    /**
     * @return the collisionsMap
     */
    public static Map<String, Double> getCollisionsMap() {
        return collisionsMap;
    }

    /**
     * @return the receiveBytesMap
     */
    public static Map<String, Double> getReceiveBytesMap() {
        return receiveBytesMap;
    }

    /**
     * @return the receiveCRCErrorsMap
     */
    public static Map<String, Double> getReceiveCRCErrorsMap() {
        return receiveCRCErrorsMap;
    }

    /**
     * @return the receiveDroppedMap
     */
    public static Map<String, Double> getReceiveDroppedMap() {
        return receiveDroppedMap;
    }

    /**
     * @return the receiveErrorsMap
     */
    public static Map<String, Double> getReceiveErrorsMap() {
        return receiveErrorsMap;
    }

    /**
     * @return the receiveFrameErrorsMap
     */
    public static Map<String, Double> getReceiveFrameErrorsMap() {
        return receiveFrameErrorsMap;
    }

    /**
     * @return the receiveOverrunErrorsMap
     */
    public static Map<String, Double> getReceiveOverrunErrorsMap() {
        return receiveOverrunErrorsMap;
    }

    /**
     * @return the receivePacketsMap
     */
    public static Map<String, Double> getReceivePacketsMap() {
        return receivePacketsMap;
    }

    /**
     * @return the transmitBytesMap
     */
    public static Map<String, Double> getTransmitBytesMap() {
        return transmitBytesMap;
    }

    /**
     * @return the transmitDroppedMap
     */
    public static Map<String, Double> getTransmitDroppedMap() {
        return transmitDroppedMap;
    }

    /**
     * @return the transmitErrorsMap
     */
    public static Map<String, Double> getTransmitErrorsMap() {
        return transmitErrorsMap;
    }

    /**
     * @return the transmitPacketsMap
     */
    public static Map<String, Double> getTransmitPacketsMap() {
        return transmitPacketsMap;
    }

    /**
     * @return the oFlowsMap
     */
    public static Map<String, OFlow> getoFlowsMap() {
        return oFlowsMap;
    }

    /**
     * @return the averageLinkUsageMap
     */
    public static Map<String, Double> getAverageLinkUsageMap() {
        return averageLinkUsageMap;
    }

    /**
     * @return the flowPushed
     */
    public static boolean isFlowPushed() {
        return flowPushed;
    }

    /**
     * @return the arpFlowPushed
     */
    public static boolean isArpFlowPushed() {
        return arpFlowPushed;
    }

    /**
     * @return the topologyURL
     */
    public static String getTopologyURL() {
        return topologyURL;
    }

    /**
     * @return the flukesTopology
     */
    public static Document getFlukesTopology() {
        return flukesTopology;
    }

    /**
     * @return the switches
     */
    public static Map<String, Switch> getSwitches() {
        return switches;
    }


    private final DataSource datasource;
    private final String uri;
    private final int floodlightPort = 8080;
    private final int sflowRTPrt = 8008;
    private final Client client;
    private static Map<String, NetworkEntity> networkEntitesCache;

    private static List<Link> switchLinks;
    private static Map<String, Switch> switches;
//    private static List<Switch> switches;
    private static long interval = 1000;

    private static Map<String, Double> statsMap;
    private static final String[] METRIC_NAMES = new String[]{"collisions",
        "receiveBytes", "receiveCRCErrors", "receiveDropped", "receiveErrors",
        "receiveFrameErrors", "receiveOverrunErrors", "receivePackets",
        "transmitBytes", "transmitDropped", "transmitErrors", "transmitPackets"};

    private static Map<String, Double> collisionsMap = new HashMap<>();

    private static Map<String, Double> receiveBytesMap = new HashMap<>();

    private static Map<String, Double> receiveCRCErrorsMap = new HashMap<>();

    private static Map<String, Double> receiveDroppedMap = new HashMap<>();

    private static Map<String, Double> receiveErrorsMap = new HashMap<>();

    private static Map<String, Double> receiveFrameErrorsMap = new HashMap<>();

    private static Map<String, Double> receiveOverrunErrorsMap = new HashMap<>();

    private static Map<String, Double> receivePacketsMap = new HashMap<>();

    private static Map<String, Double> transmitBytesMap = new HashMap<>();

    private static Map<String, Double> transmitDroppedMap = new HashMap<>();

    private static Map<String, Double> transmitErrorsMap = new HashMap<>();

    private static Map<String, Double> transmitPacketsMap = new HashMap<>();

    private static Map<String, OFlow> oFlowsMap = new HashMap<>();

    private static Map<String, Double> averageLinkUsageMap = new HashMap<>();
    private long iterations = 1;
    private static boolean flowPushed = true;
    private static boolean arpFlowPushed = false;
    private static String topologyURL;
    private static Document flukesTopology;
    private final SDNControllerClient sdnCC;
//    private static final Object lock = new Object();
    private final double aplha;
    private final Double factor;

    public SDNSweep(DataSource datasource) throws IOException, ParserConfigurationException, SAXException {
        this.datasource = datasource;
        uri = PropertiesHelper.getSDNControllerURL();
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);
        initFlukesTopology();

        sdnCC = new SDNControllerClient(getUri());

        aplha = PropertiesHelper.getAlphaforAverageLinkUsage();
        factor = PropertiesHelper.getDelayFactor();
    }

    private void init() throws InterruptedException, IOException, ParserConfigurationException, SAXException {
        getAllSwitches();
        getAllNetworkEntites();
        getAllSwitchLinks();
        if (!isFlowPushed()) {
//            pushFlowIntoOnePort();
        }
        if (!isArpFlowPushed() && PropertiesHelper.pushARPFlow()) {
            pushARPFlow();
            arpFlowPushed = true;
        }
    }

    public static NetworkEntity getNetworkEntity(String dest) throws IOException {
        return getNetworkEntitesCache().get(dest);
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
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateMetrics() throws IOException, InterruptedException {
        for (Switch sw : getAllSwitches()) {
            List<FloodlightStats> stats1 = getFloodlightPortStats(sw.dpid);
            Thread.sleep(getInterval());
            List<FloodlightStats> stats2 = getFloodlightPortStats(sw.dpid);

            for (int i = 0; i < stats1.size(); i++) {
//                for(String mn  : METRIC_NAMES){
//                    String key = mn+"-"+srcSW.dpid + "-" + stats1.get(i).portNumber;
//                }
                String key = sw.dpid + "-" + stats1.get(i).portNumber;

                Double val = getCollisionsMap().get(key);
                Double oldValue = ((val == null) ? 1.0 : val);
                Double newValue = Double.valueOf(stats2.get(i).collisions - stats1.get(i).collisions);
                newValue = (newValue + oldValue) / 2.0;
                getCollisionsMap().put(key, newValue);

                val = getReceiveBytesMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveBytes - stats1.get(i).receiveBytes);
                Double averageLinkUsage = getAverageLinkUsageMap().get(key);

                if ((newValue / getInterval()) > 200) {
                    long durationSeconds = getInterval() * getIterations();

                    if (averageLinkUsage == null) {
                        getAverageLinkUsageMap().put(key, Double.valueOf(durationSeconds));
                    } else {
                        //$d_{new} = α ∗ d_{old} + (1 - α ) ∗ d_{sample}$ 
                        //with α ∈ [0 , 1] being the weighting factor, d_{sample} 
                        //being the new sample, d_{old} the current metric value 
                        //and d_{new} the newly calculated value. In fact, 
                        //this calculation implements aplha discrete low pass filter

                        averageLinkUsage = getAplha() * averageLinkUsage + (1 - getAplha()) * durationSeconds;
//                        averageLinkUsage = (averageLinkUsage + durationSeconds) / 2.0;
                        getAverageLinkUsageMap().put(key, averageLinkUsage);
                    }
                } else if (averageLinkUsage != null) {
                    averageLinkUsage = averageLinkUsage / (getFactor() * 200);
//                        averageLinkUsage = aplha * averageLinkUsage + (1 - aplha) * durationSeconds;
//                        averageLinkUsage = (averageLinkUsage + durationSeconds) / 2.0;
                    getAverageLinkUsageMap().put(key, (averageLinkUsage > 0) ? averageLinkUsage : 1.0);
                }
                val = ((newValue > oldValue) ? newValue : oldValue);
                getReceiveBytesMap().put(key, val);

                val = getTransmitBytesMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitBytes - stats1.get(i).transmitBytes);
                if ((newValue / getInterval()) > 200) {
                    long durationSeconds = getInterval() * getIterations();
                    if (averageLinkUsage == null) {
                        getAverageLinkUsageMap().put(key, Double.valueOf(durationSeconds));
                    } else {
                        //$d_{new} = α ∗ d_{old} + (1 - α ) ∗ d_{sample}$ 
                        //with α ∈ [0 , 1] being the weighting factor, d_{sample} 
                        //being the new sample, d_{old} the current metric value 
                        //and d_{new} the newly calculated value. In fact, 
                        //this calculation implements aplha discrete low pass filter
                        averageLinkUsage = getAplha() * averageLinkUsage + (1 - getAplha()) * durationSeconds;
//                        averageLinkUsage = (averageLinkUsage + durationSeconds) / 2.0;
                        getAverageLinkUsageMap().put(key, averageLinkUsage);
                    }
                } else if (averageLinkUsage != null) {
                    averageLinkUsage = averageLinkUsage / (getFactor() * 200);
//                        averageLinkUsage = aplha * averageLinkUsage + (1 - aplha) * durationSeconds;
//                        averageLinkUsage = (averageLinkUsage + durationSeconds) / 2.0;
                    getAverageLinkUsageMap().put(key, (averageLinkUsage > 0) ? averageLinkUsage : 1.0);
                }

                val = ((newValue > oldValue) ? newValue : oldValue);
                getTransmitBytesMap().put(key, val);

                val = getReceiveCRCErrorsMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveCRCErrors - stats1.get(i).receiveCRCErrors);
                newValue = (newValue + oldValue) / 2.0;
                getReceiveCRCErrorsMap().put(key, newValue);

                val = getReceiveDroppedMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveDropped - stats1.get(i).receiveDropped);
                newValue = (newValue + oldValue) / 2.0;
                getReceiveDroppedMap().put(key, newValue);

                val = getReceiveErrorsMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveErrors - stats1.get(i).receiveErrors);
                newValue = (newValue + oldValue) / 2.0;
                getReceiveErrorsMap().put(key, newValue);

                val = getReceiveFrameErrorsMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveFrameErrors - stats1.get(i).receiveFrameErrors);
                newValue = (newValue + oldValue) / 2.0;
                getReceiveErrorsMap().put(key, newValue);

                val = getReceiveOverrunErrorsMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receiveOverrunErrors - stats1.get(i).receiveOverrunErrors);
                newValue = (newValue + oldValue) / 2.0;
                getReceiveOverrunErrorsMap().put(key, newValue);

                val = getReceivePacketsMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).receivePackets - stats1.get(i).receivePackets);
                val = ((newValue > oldValue) ? newValue : oldValue);
                getReceivePacketsMap().put(key, val);

                val = getTransmitDroppedMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitDropped - stats1.get(i).transmitDropped);
                newValue = (newValue + oldValue) / 2.0;
                getTransmitDroppedMap().put(key, newValue);

                val = getTransmitErrorsMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitErrors - stats1.get(i).transmitErrors);
                newValue = (newValue + oldValue) / 2.0;
                getTransmitErrorsMap().put(key, newValue);

                val = getTransmitPacketsMap().get(key);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats2.get(i).transmitPackets - stats1.get(i).transmitPackets);
                val = ((newValue > oldValue) ? newValue : oldValue);
                getTransmitPacketsMap().put(key, val);

            }
        }
    }

    private List<FloodlightStats> getFloodlightPortStats(String dpi) throws IOException {
        //http://145.100.133.130:8080/wm/core/switch/00:00:4e:cd:a6:8d:c9:44/port/json
        WebResource webResource = getClient().resource(getUri() + ":" + getFloodlightPort());
        WebResource res = webResource.path("wm").path("core").path("switch").path(dpi).path("port").path("json");
        String output = res.get(String.class);
//        String out = output.substring(27, output.length() - 1);
        String out = output.substring(output.indexOf("[{"), output.length() - 1);
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(out, mapper.getTypeFactory().constructCollectionType(List.class, FloodlightStats.class));
    }

    private List<OFlow> getOflow(String dpi) throws IOException {
        WebResource webResource = getClient().resource(getUri() + ":" + getFloodlightPort());
        WebResource res = webResource.path("wm").path("core").path("switch").path(dpi).path("flow").path("json");
        String output = res.get(String.class);
        String out = output.substring(27, output.length() - 1);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(out, mapper.getTypeFactory().constructCollectionType(List.class, OFlow.class));
    }

    private Collection<NetworkEntity> getAllNetworkEntites() throws IOException, ParserConfigurationException, SAXException {
        if (getNetworkEntitesCache() == null) {
            networkEntitesCache = new HashMap<>();
        }
        if (getNetworkEntitesCache().isEmpty() || getIterations() % 5 == 0) {
            WebResource webResource = getClient().resource(getUri() + ":" + getFloodlightPort());
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
                    if (getFlukesTopology() == null) {
                        initFlukesTopology();
                    }
                    if (getFlukesTopology() != null) {
                        swIPFromFlukes = findAttachedSwitch(ipKey);
                        String swDPI = null;//switches.get(swIPFromFlukes).dpid;
                        Switch sw = getSwitches().get(swIPFromFlukes);
                        if (sw != null) {
                            swDPI = sw.dpid;
                        }
                        List<AttachmentPoint> ap = new ArrayList<>();
                        for (AttachmentPoint p : ne.getAttachmentPoint()) {
                            if (swDPI != null && p.getSwitchDPID().equals(swDPI)) {
                                ap.add(p);
                                break;
                            }
                        }
                        ne.setAttachmentPoint(ap);
                    }
                    getNetworkEntitesCache().put(ipKey, ne);
                }
            }
        }
//        for (NetworkEntity ne : networkEntitesCache.values()) {
//            Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, "ne: {0}/{1}-> {2}-{3}", new Object[]{ne.getIpv4().get(0), ne.getMac().get(0), ne.getAttachmentPoint().get(0).getSwitchDPID(), ne.getAttachmentPoint().get(0).getPort()});
//        }
        return getNetworkEntitesCache().values();
    }

    private String findAttachedSwitch(String ipKey) {
        NodeList desc = getFlukesTopology().getElementsByTagName("rdf:Description");
        String ipForTopology = ipKey.replaceAll("\\.", "-");
        String linkNum = null;
        String swName = null;
        String swIP = null;

        for (int i = 0; i < desc.getLength(); i++) {
            Node n = desc.item(i);
            NamedNodeMap att = n.getAttributes();
            for (int j = 0; j < att.getLength(); j++) {
                Node n2 = att.item(j);
                if (n2.getNodeValue().contains(ipForTopology) && n2.getNodeValue().contains("Link")) {
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

    private ArrayList<String> findConnectedSwitches(String ipKey) {
        NodeList desc = getFlukesTopology().getElementsByTagName("rdf:Description");
        String ipForTopology = ipKey.replaceAll("\\.", "-");
        String switchName = null;

        for (int i = 0; i < desc.getLength(); i++) {
            Node n = desc.item(i);
            NamedNodeMap att = n.getAttributes();
            for (int j = 0; j < att.getLength(); j++) {
                Node n2 = att.item(j);
                //Get the srcSW name 
                if (n2.getNodeValue().contains(ipForTopology)) {
                    switchName = n2.getNodeValue().substring(n2.getNodeValue().lastIndexOf("Switch"));
                    switchName = switchName.substring(0, switchName.indexOf("-"));
                    break;
                }
            }
        }
        ArrayList<String> linkNames = new ArrayList<>();
        if (switchName != null) {
            for (int i = 0; i < desc.getLength(); i++) {
                Node n = desc.item(i);
                NamedNodeMap att = n.getAttributes();
                for (int j = 0; j < att.getLength(); j++) {
                    Node n2 = att.item(j);
                    //Get the srcSW links
                    if (n2.getNodeValue().contains(switchName) && n2.getNodeValue().contains("Link")) {
                        String linkNum = n2.getNodeValue().substring(n2.getNodeValue().lastIndexOf("#Link") + 1);
                        linkNum = linkNum.substring(0, linkNum.indexOf("-"));
                        if (!linkNames.contains(linkNum)) {
                            linkNames.add(linkNum);
                        }
                    }
                }
            }
        }

        ArrayList<String> dstSWNames = new ArrayList<>();
        if (!linkNames.isEmpty()) {
            for (int i = 0; i < desc.getLength(); i++) {
                Node n = desc.item(i);
                NamedNodeMap att = n.getAttributes();
                for (int j = 0; j < att.getLength(); j++) {
                    Node n2 = att.item(j);
                    //Get the srcSW the links connect
                    for (String ln : linkNames) {
                        if (n2.getNodeValue().contains(ln) && n2.getNodeValue().contains("Switch") && !n2.getNodeValue().contains(switchName)) {
                            String dstSwName = n2.getNodeValue().substring(n2.getNodeValue().lastIndexOf("#Link") + 1);
                            dstSwName = dstSwName.split("-")[1];
                            if (!dstSWNames.contains(dstSwName)) {
                                dstSWNames.add(dstSwName);
                            }
                        }
                    }
                }
            }
        }

        ArrayList<String> dstSWIP = new ArrayList<>();
        if (!dstSWNames.isEmpty()) {
            for (int i = 0; i < desc.getLength(); i++) {
                Node n = desc.item(i);
                NamedNodeMap att = n.getAttributes();
                for (int j = 0; j < att.getLength(); j++) {
                    Node n2 = att.item(j);
                    //Get the srcSW the links connect
                    for (String sw : dstSWNames) {
                        if (n2.getNodeValue().contains(sw + "-ip-")) {
                            String swIP = n2.getNodeValue().substring(n2.getNodeValue().lastIndexOf(sw + "-ip") + 1 + (sw + "ip-").length());
                            if (!dstSWIP.contains(swIP)) {
                                dstSWIP.add(swIP.replaceAll("-", "\\."));
                            }
                        }
                    }
                }
            }
        }

        return dstSWIP;
    }

    public void pushFlows(List<String> rules) {
        WebResource webResource = getClient().resource(getUri() + ":" + getFloodlightPort()).path("wm").path("staticflowentrypusher").path("json");
        for (String r : rules) {
            ClientResponse response = webResource.post(ClientResponse.class, r);
            if (response.getStatus() != 200) {
                Logger.getLogger(SDNSweep.class.getName()).log(Level.WARNING, "Failed to push: " + r);
            }
//            Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, r);
//            Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, response.toString());
        }
    }

    private List<Link> getAllSwitchLinks() throws IOException, ParserConfigurationException, SAXException {
        if (getSwitchLinks() == null) {
            switchLinks = new ArrayList<>();
        }
        if (getSwitchLinks().isEmpty() || getIterations() % 5 == 0) {
            WebResource webResource = getClient().resource(getUri() + ":" + getFloodlightPort());
            WebResource res = webResource.path("wm").path("topology").path("links").path("json");
            switchLinks = res.get(new GenericType<List<Link>>() {
            });
            res = webResource.path("wm").path("topology").path("external-links").path("json");
            getSwitchLinks().addAll(res.get(new GenericType<List<Link>>() {
            }));

//            ArrayList<Link> addedLinks = fillInLinkGaps(switchLinks);
//            switchLinks.addAll(addedLinks);
//            Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, switchLinks.toString());
        }

        return getSwitchLinks();
    }

    private Collection<Switch> getAllSwitches() {
        if (getSwitches() == null) {
            switches = new HashMap<>();
            WebResource webResource = getClient().resource(getUri() + ":" + getFloodlightPort());
            WebResource res = webResource.path("wm").path("core").path("controller").path("switches").path("json");
            List<Switch> switchesList = res.get(new GenericType<List<Switch>>() {
            });
            for (Switch s : switchesList) {
                String swIP = s.inetAddress.substring(1, s.inetAddress.lastIndexOf(":"));
                getSwitches().put(swIP, s);
            }
        }
        return getSwitches().values();
    }

    private void pushARPFlow() throws InterruptedException, IOException, ParserConfigurationException, SAXException {
        Iterator<Switch> swIter = getAllSwitches().iterator();
        List<String> arpFlows = new ArrayList<>();
        while (swIter.hasNext()) {
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
        List<Link> links = getAllSwitchLinks();
        StringBuilder sb = new StringBuilder();
        for (Link l : links) {
            Collection<NetworkEntity> nes = getNetworkEntitiesForSwDPI(l.dstSwitch);
            for (NetworkEntity neDst : nes) {
                for (String mac : neDst.getMac()) {
                    sb.append("{\"switch\": \"").
                            append(l.srcSwitch).
                            append("\"," + "\"name\":\"").
                            append(l.srcSwitch).
                            append("-to-").
                            append(mac).
                            append("\", " + "\"cookie\":\"0\", " + "\"priority\":\"32768\", " + "\"dst-mac\":\"").
                            append(mac).
                            append("\"," + "\"active\":\"true\"," + "\"actions\":\"output=").
                            append(l.srcPort).
                            append("\"}\"");
                    arpFlows.add(sb.toString());
//                    Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, sb.toString());
                    sb.setLength(0);
                }
            }
            nes = getNetworkEntitiesForSwDPI(l.srcSwitch);
            for (NetworkEntity neDst : nes) {
                for (String mac : neDst.getMac()) {
                    sb.append("{\"switch\": \"").
                            append(l.dstSwitch).
                            append("\"," + "\"name\":\"").
                            append(l.dstSwitch).
                            append("-to-").
                            append(mac).
                            append("\", " + "\"cookie\":\"0\", " + "\"priority\":\"32768\", " + "\"dst-mac\":\"").
                            append(mac).
                            append("\"," + "\"active\":\"true\"," + "\"actions\":\"output=").
                            append(l.dstPort).
                            append("\"}\"");
                    arpFlows.add(sb.toString());
//                    Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, sb.toString());
                    sb.setLength(0);
                }
            }
        }
        pushFlows(arpFlows);
    }

    private void pushFlowIntoOnePort() throws IOException, ParserConfigurationException, SAXException {

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
                        + "\"src-mac\":\"" + getNetworkEntitesCache().get(h1).getMac().get(0) + "\", \"ingress-port\":\"" + getNetworkEntitesCache().get(h1).getAttachmentPoint().get(0).getPort() + "\", "
                        + "\"dst-mac\": \"" + getNetworkEntitesCache().get(h2).getMac().get(0) + "\", \"active\":\"true\","
                        + "\"actions\":\"output=" + s1ToS2Port + "\"}";
                rules.add(rule1To2);
//                Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, rule1To2);
            }
        }

        //s2 to s1
        for (String h1 : s2Hosts) {
            for (String h2 : s1Hosts) {
                String rule2To1 = "{\"switch\": \"" + s2 + "\", \"name\":\"" + h1 + "To" + h2 + "\", \"cookie\":\"0\", \"priority\":\"10\", "
                        + "\"src-mac\":\"" + getNetworkEntitesCache().get(h1).getMac().get(0) + "\", \"ingress-port\":\"" + getNetworkEntitesCache().get(h1).getAttachmentPoint().get(0).getPort() + "\", "
                        + "\"dst-mac\": \"" + getNetworkEntitesCache().get(h2).getMac().get(0) + "\", \"active\":\"true\","
                        + "\"actions\":\"output=" + s2ToS1Port + "\"}";

                rules.add(rule2To1);
//                Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, rule2To1);
            }
        }
        pushFlows(rules);
        flowPushed = true;
    }

    private Collection<NetworkEntity> getNetworkEntitiesForSwDPI(String dpid) throws IOException, ParserConfigurationException, SAXException {
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

    private void initFlukesTopology() throws IOException, ParserConfigurationException, SAXException {
        //        synchronized (lock) {
        topologyURL = PropertiesHelper.getTopologyURL();
        if (getTopologyURL() != null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            URL url = new URL(getTopologyURL());
            try (InputStream stream = url.openStream()) {
                flukesTopology = db.parse(stream);
            }
        }
//        }
    }

    private ArrayList<Link> fillInLinkGaps(List<Link> switchLinks) throws IOException, ParserConfigurationException, SAXException {
        ArrayList<Link> possibleLinks = new ArrayList<>();

        for (Switch srcSW : getAllSwitches()) {
            String srcSWIP = srcSW.inetAddress.replaceAll("/", "").substring(0, srcSW.inetAddress.indexOf(":") - 1);
            int srcPort = 0;
            for (NetworkEntity ne : getNetworkEntitiesForSwDPI(srcSW.dpid)) {
                for (AttachmentPoint ap : ne.getAttachmentPoint()) {
                    for (Port p : srcSW.ports) {
                        for (Link existingLink : switchLinks) {
                            if (ap.getPort() != (Number) p.portNumber
                                    && p.portNumber != existingLink.srcPort
                                    && ap.getPort().intValue() <= 500
                                    && p.portNumber <= 500) {
                                srcPort = p.portNumber;
                                break;
                            }
                        }

                    }
                }
            }
            int dstPort;
            Link l = null;
            for (String dstSWIP : findConnectedSwitches(srcSWIP)) {
                Switch dstSW = getSwitches().get(dstSWIP);
                for (NetworkEntity ne : getNetworkEntitiesForSwDPI(dstSW.dpid)) {
                    for (AttachmentPoint ap : ne.getAttachmentPoint()) {
                        for (Port p : dstSW.ports) {
                            for (Link existingLink : switchLinks) {
                                if (ap.getPort() != (Number) p.portNumber
                                        && existingLink.dstPort != p.portNumber
                                        && ap.getPort().intValue() <= 500
                                        && p.portNumber <= 500
                                        && srcPort <= 500) {
                                    dstPort = p.portNumber;
                                    l = new Link();
                                    l.srcSwitch = srcSW.dpid;
                                    l.srcPort = srcPort;
                                    l.dstSwitch = dstSW.dpid;
                                    l.dstPort = dstPort;
                                    possibleLinks.add(l);
                                    break;
                                }
                            }
                        }
                    }
                }

            }
        }

        for (Link possible : possibleLinks) {
            String possibleSRC = possible.srcSwitch + "-" + possible.srcPort;
            String possibleDST = possible.dstSwitch + "-" + possible.dstPort;

            for (Link existing : switchLinks) {
                String existingSRC = existing.srcSwitch + "-" + existing.srcPort;
                String existingDST = existing.dstSwitch + "-" + existing.dstPort;

                if (!possibleSRC.equals(existingSRC)
                        && !possibleDST.equals(existingDST)
                        && !possibleSRC.equals(existingDST)
                        && !possibleDST.equals(existingSRC)) {
                    Logger.getLogger(SDNSweep.class.getName()).log(Level.INFO, "{0}-{1} -> {2}-{3}", new Object[]{possible.srcSwitch, possible.srcPort, possible.dstSwitch, possible.dstPort});
                }
            }

        }

        ArrayList<Link> addedLinks = new ArrayList<>();
//        for (Link possible : switchLinks) {
//            for (Link existing : possibleLinks) {
//                String src1 = possible.srcSwitch + "-" + possible.srcPort;
//                String dst1 = possible.dstSwitch + "-" + possible.dstPort;
//
//                String src2 = existing.srcSwitch + "-" + existing.srcPort;
//                String dst2 = existing.dstSwitch + "-" + existing.dstPort;
//
//                if (!src1.equals(src2) && !dst1.equals(dst2)) {
//                    addedLinks.add(existing);
//                }
//
//            }
//        }
        return addedLinks;
    }

    /**
     * @return the datasource
     */
    public DataSource getDatasource() {
        return datasource;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return the floodlightPort
     */
    public int getFloodlightPort() {
        return floodlightPort;
    }

    /**
     * @return the sflowRTPrt
     */
    public int getSflowRTPrt() {
        return sflowRTPrt;
    }

    /**
     * @return the client
     */
    public Client getClient() {
        return client;
    }

    /**
     * @return the iterations
     */
    public long getIterations() {
        return iterations;
    }

    /**
     * @return the sdnCC
     */
    public SDNControllerClient getSdnCC() {
        return sdnCC;
    }

    /**
     * @return the aplha
     */
    public double getAplha() {
        return aplha;
    }

    /**
     * @return the factor
     */
    public Double getFactor() {
        return factor;
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
