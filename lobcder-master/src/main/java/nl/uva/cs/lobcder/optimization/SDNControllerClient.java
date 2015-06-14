/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import nl.uva.cs.lobcder.catalogue.SDNSweep;
import nl.uva.cs.lobcder.catalogue.SDNSweep.Port;
import nl.uva.cs.lobcder.rest.wrappers.AttachmentPoint;
import nl.uva.cs.lobcder.rest.wrappers.Link;
import nl.uva.cs.lobcder.rest.wrappers.NetworkEntity;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.xml.sax.SAXException;

/**
 *
 * @author S. Koulouzis
 */
public class SDNControllerClient {

//    private final Client client;
    private String uri;
//    private int floodlightPort = 8080;
//    private int sflowRTPrt = 8008;
//    private static List<Switch> switches;
//    private static Map<String, String> networkEntitySwitchMap;
//    private static Map<String, Integer> sFlowHostPortMap;
//    private static Map<String, List<NetworkEntity>> networkEntityCache;
    private static SimpleWeightedGraph<String, DefaultWeightedEdge> graph;
//    private static List<Link> linkCache;
    private DOTExporter dot;

    public SDNControllerClient(String uri) throws IOException {
//        ClientConfig clientConfig = new DefaultClientConfig();
//        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
//        client = Client.create(clientConfig);
//        this.uri = uri;
    }

    public List<DefaultWeightedEdge> getShortestPath(String dest, Set<String> sources) throws InterruptedException, IOException {
        if (graph == null) {
            graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
//            dot = new DOTExporter(new IntegerNameProvider(), new VertexNameProvider() {
//                public String getVertexName(Object object) {
//                    if (object == null) {
//                        return "none";
//                    }
//                    return object.toString().replaceAll("\"", "\'");
//                }
//            }, new EdgeNameProvider<Object>() {
//                public String getEdgeName(Object object) {
//                    if (object == null) {
//                        return "none";
//                    }
//
//                    DefaultWeightedEdge e1 = (DefaultWeightedEdge) object;
////                Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, object.getClass().getName());
//                    return String.valueOf(graph.getEdgeWeight(e1)); //object.toString().replaceAll("\"", "\'");
//                }
//            });
        }

        Collection<SDNSweep.Switch> switchesColl = SDNSweep.getSwitches();

        List<SDNSweep.Switch> switches;
        if (switchesColl instanceof List) {
            switches = (List) switchesColl;
        } else {
            switches = new ArrayList(switchesColl);
        }
        for (int i = 0; i < switches.size(); i++) {
            List<Port> ports = switches.get(i).ports;
            if (ports != null) {
                for (int j = 0; j < ports.size(); j++) {
                    for (int k = 0; k < ports.size(); k++) {
                        if (ports.get(j).state == 0 && ports.get(k).state == 0 && j != k) {
                            String vertex1 = switches.get(i).dpid + "-" + ports.get(j).portNumber;
                            String vertex2 = switches.get(i).dpid + "-" + ports.get(k).portNumber;
//                            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "From: {0} to: {1}", new Object[]{vertex1, vertex2});
                            if (!graph.containsVertex(vertex1)) {
                                graph.addVertex(vertex1);
                            }
                            if (!graph.containsVertex(vertex2)) {
                                graph.addVertex(vertex2);
                            }
                            DefaultWeightedEdge e1;
                            if (!graph.containsEdge(vertex1, vertex2)) {
                                e1 = graph.addEdge(vertex1, vertex2);
                            } else {
                                e1 = graph.getEdge(vertex1, vertex2);
                            }
                            graph.setEdgeWeight(e1, 1);
                        }
                    }
                }
            }
        }
//        Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "Destination: {0}", new Object[]{dest});
        if (!graph.containsVertex(dest)) {
            graph.addVertex(dest);
        }
        NetworkEntity destinationEntityArray = SDNSweep.getNetworkEntity(dest);
//        List<NetworkEntity> destinationEntityArray = getNetworkEntity(dest);
//        for (SDNSweep.NetworkEntity ne : destinationEntityArray) {
        if (destinationEntityArray != null) {
            for (AttachmentPoint ap : destinationEntityArray.getAttachmentPoint()) {
                String vertex = ap.getSwitchDPID() + "-" + ap.getPort();
//            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "vertex: {0}", new Object[]{vertex});
                if (!graph.containsVertex(vertex)) {
                    graph.addVertex(vertex);
                }

                DefaultWeightedEdge e1;
                if (!graph.containsEdge(dest, vertex)) {
                    e1 = graph.addEdge(dest, vertex);
                } else {
                    e1 = graph.getEdge(dest, vertex);
                }
                //Don't calculate the cost from the destination to the switch. 
                //There is nothing we can do about it so why waste cycles ?
//            graph.setEdgeWeight(e1, 2);
                graph.setEdgeWeight(e1, getCost(dest, vertex));
            }
        }
//        }

//        List<NetworkEntity> sourceEntityArray = getNetworkEntity(sources);
        for (String s : sources) {
            NetworkEntity ne = SDNSweep.getNetworkEntity(s);
            if (ne != null) {
                for (String ip : ne.getIpv4()) {
                    if (!graph.containsVertex(ip)) {
                        graph.addVertex(ip);
                    }

                    for (AttachmentPoint ap : ne.getAttachmentPoint()) {
                        String vertex = ap.getSwitchDPID() + "-" + ap.getPort();
                        if (!graph.containsVertex(vertex)) {
                            graph.addVertex(vertex);
                        }
                        DefaultWeightedEdge e2;
                        if (!graph.containsEdge(ip, vertex)) {
                            e2 = graph.addEdge(ip, vertex);
                        } else {
                            e2 = graph.getEdge(ip, vertex);
                        }
                        graph.setEdgeWeight(e2, getCost(ip, vertex));
                    }
                }
            }
        }
        List<Link> links = SDNSweep.getSwitchLinks();
        for (Link l : links) {
            String srcVertex = l.srcSwitch + "-" + l.srcPort;
            if (!graph.containsVertex(srcVertex)) {
                graph.addVertex(srcVertex);
            }
            String dstVertex = l.dstSwitch + "-" + l.dstPort;
            if (!graph.containsVertex(dstVertex)) {
                graph.addVertex(dstVertex);
            }

            DefaultWeightedEdge e3;
            if (!graph.containsEdge(srcVertex, dstVertex)) {
                e3 = graph.addEdge(srcVertex, dstVertex);
            } else {
                e3 = graph.getEdge(srcVertex, dstVertex);
            }
//            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "dstVertex: {0}", new Object[]{dstVertex});
            graph.setEdgeWeight(e3, getCost(srcVertex, dstVertex));
        }
        double cost = Double.MAX_VALUE;
        List<DefaultWeightedEdge> shortestPath = null;

        StringBuilder msg = new StringBuilder();
        msg.append("\n");
        for (String s : sources) {
            if (graph.containsVertex(dest) && graph.containsVertex(s)) {
                List<DefaultWeightedEdge> shorPath = DijkstraShortestPath.findPathBetween(graph, s, dest);
                double w = 0;
                if (shorPath != null) {
                    for (DefaultWeightedEdge e : shorPath) {
                        w += graph.getEdgeWeight(e);
                    }
                    DefaultWeightedEdge p = shorPath.get(0);
                    String e = graph.getEdgeSource(p);
                    msg.append("source: ").append(e).append(" cost: ").append(w).append("\n");
                    if (w <= cost) {
                        cost = w;
                        shortestPath = shorPath;
                        if (cost <= 2) {
                            break;
                        }
                    }
                }
            }
        }
        Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, msg.toString());
//        File f = new File("/home/alogo/Downloads/sdn_graph.dot");
//        f.delete();
//        dot.export(new FileWriter(f), graph);
        return shortestPath;
    }

    private double getCost(String v1, String v2) throws InterruptedException, IOException {
//        String[] agentPort = getsFlowPort(v1, v2);
//        double tpp = getTimePerPacket(agentPort[0], Integer.valueOf(agentPort[1]));
        String dpi;
        if (v1.contains(":")) {
            dpi = v1;
        } else {
            dpi = v2;
        }

        //        SDNSweep.FloodlightStats[] stats = getFloodlightPortStats(dpi, getPort());
        Double rpps = SDNSweep.getReceivePacketsMap().get(dpi);
        Double tpps = SDNSweep.getTransmitPacketsMap().get(dpi);
        if (rpps == null) {
            rpps = Double.valueOf(1);
        }
        if (tpps == null) {
            tpps = Double.valueOf(1);
        }
//        double rrrt = (interval / rpps);
//        double trrt = (interval / tpps);

        double tpp = (rpps > tpps) ? rpps : tpps;
        if (tpp <= 0) {
            tpp = 1;
        }
        Double rbytes = SDNSweep.getReceiveBytesMap().get(dpi);
        if (rbytes == null) {
            rbytes = Double.valueOf(1);
        }
        Double tbytes = SDNSweep.getTransmitBytesMap().get(dpi);
        if (tbytes == null) {
            tbytes = Double.valueOf(1);
        }
        double rbps = rbytes / SDNSweep.interval;
        double tbps = tbytes / SDNSweep.interval;
//        double cost = 1.0 / ((rbps + tbps) / 2.0);


        if (rbytes <= 0) {
            rbytes = Double.valueOf(1);
        }
        if (tbytes <= 0) {
            tbytes = Double.valueOf(1);
        }

        double rMTU = rbytes / rpps * 1.0;
        double tMTU = tbytes / tpps * 1.0;
        double mtu = (rMTU > tMTU) ? rMTU : tMTU;
//        if (mtu <= 500) {
//            mtu = 1500;
//        }

        //TT=TpP * NoP
        //NoP = {MTU}/{FS}
        //TpP =[({MTU} / {bps}) + RTT] // is the time it takes to transmit one packet or time per packet
        //TT = [({MTU} / {bps}) + RTT] * [ {MTU}/{FS}]
        double nop = mtu / 1024.0;
        double mtt = tpp * nop;

//        SDNSweep.OFlow f = SDNSweep.getOFlowsMap().get(dpi);
//        double bps = -1;
//        if (f != null) {
//            bps = f.byteCount / f.durationSeconds * 1.0;
//            double tmp = f.packetCount / f.durationSeconds * 1.0;
//            if (tpp <= 1 && tmp > tpp) {
//                mtt = tmp * nop;
//            }
//        }
        Double averageLinkUsage = SDNSweep.getAverageLinkUsageMap().get(dpi);
        if (averageLinkUsage != null) {
//        For each sec of usage how much extra time we get ? 
//        We asume a liner ralationship 
//        The longer the usage it means either more transfers per flow or larger files or both
            mtt += averageLinkUsage + PropertiesHelper.getDelayFactor();
//            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "dpi: " + dpi + " averageLinkUsage: " + averageLinkUsage);
        }

//        Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "From: {0} to: {1} tt: {2}", new Object[]{v1, v2, mtt});
        return mtt;
    }

    public void pushFlow(final List<DefaultWeightedEdge> shortestPath) throws IOException {
        if (shortestPath != null && !shortestPath.isEmpty()) {
            Thread thread = new Thread() {
                public void run() {
                    try {
                        DefaultWeightedEdge e = shortestPath.get(0);
                        String pair = e.toString().substring(1, e.toString().length() - 1);
                        String[] workerSwitch = pair.toString().split(" : ");
                        String srcIP = workerSwitch[0];
                        String srcMac = SDNSweep.getNetworkEntity(srcIP).getMac().get(0);
                        String srcSwitchAndPort = workerSwitch[1];
                        String srcSwitch = srcSwitchAndPort.split("-")[0];
                        String srcIngressPort = String.valueOf(SDNSweep.getNetworkEntity(srcIP).getAttachmentPoint().get(0).getPort());
                        String srcOutput;

                        e = shortestPath.get(1);
                        pair = e.toString().substring(1, e.toString().length() - 1);
                        workerSwitch = pair.split(" : ");
                        if (workerSwitch[0].equals(srcSwitch + "-" + srcIngressPort)) {
                            srcOutput = workerSwitch[1].split("-")[1];
                        } else {
                            srcOutput = workerSwitch[0].split("-")[1];
                        }

                        e = shortestPath.get(shortestPath.size() - 1);
                        pair = e.toString().substring(1, e.toString().length() - 1);
                        workerSwitch = pair.toString().split(" : ");
                        String dstIP = workerSwitch[0];
                        String dstMac = SDNSweep.getNetworkEntity(dstIP).getMac().get(0);
                        String dstSwitchAndPort = workerSwitch[1];
                        String dstSwitch = dstSwitchAndPort.split("-")[0];
                        String dstOutput = String.valueOf(SDNSweep.getNetworkEntity(dstIP).getAttachmentPoint().get(0).getPort());


                        e = shortestPath.get(shortestPath.size() - 2);
                        pair = e.toString().substring(1, e.toString().length() - 1);
                        workerSwitch = pair.toString().split(" : ");
                        String node1 = workerSwitch[0];
                        String node2 = workerSwitch[1];
                        String dstIngressPort = "";
                        if (node1.equals(dstSwitch + "-" + dstOutput)) {
                            dstIngressPort = node2.split("-")[1];
                        } else {
                            dstIngressPort = node1.split("-")[1];
                        }



//                    String rulesrcToSw = "{\"switch\": \"" + srcSwitch + "\", \"name\":\"tmp\", \"cookie\":\"0\", \"priority\":\"5\", "
//                            + "\"src-ip\":\"" + srcIP + "\", \"ingress-getPort()\":\"" + srcIngressPort + "\", "
//                            + "\"dst-ip\": \"" + dstIP + "\", \"active\":\"true\",\"ether-type\":\"0x0800\", "
//                            + "\"actions\":\"output=" + srcOutput + "\"}";
//
//
//                    String ruleSwTodst = "{\"switch\": \"" + dstSwitch + "\", \"name\":\"tmp\", \"cookie\":\"0\", \"priority\":\"5\", "
//                            + "\"src-ip\":\"" + srcIP + "\", \"ingress-getPort()\":\"" + dstIngressPort + "\", "
//                            + "\"dst-ip\": \"" + dstIP + "\", \"active\":\"true\",\"ether-type\":\"0x0800\", "
//                            + "\"actions\":\"output=" + dstOutput + "\"}";

                        String rule11 = "{\"switch\": \"" + srcSwitch + "\", \"name\":\"tmp1-1\", \"cookie\":\"0\", \"priority\":\"5\", "
                                + "\"src-mac\":\"" + srcMac + "\", \"ingress-getPort()\":\"" + srcIngressPort + "\", "
                                + "\"dst-mac\": \"" + dstMac + "\", \"active\":\"true\",\"vlan-id\":\"-1\", "
                                + "\"actions\":\"output=" + srcOutput + "\"}";


                        String rule12 = "{\"switch\": \"" + srcSwitch + "\", \"name\":\"tmp1-2\", \"cookie\":\"0\", \"priority\":\"5\", "
                                + "\"src-mac\":\"" + dstMac + "\", \"ingress-getPort()\":\"" + srcOutput + "\", "
                                + "\"dst-mac\": \"" + srcMac + "\", \"active\":\"true\",\"vlan-id\":\"-1\", "
                                + "\"actions\":\"output=" + srcIngressPort + "\"}";

                        String rule21 = "{\"switch\": \"" + dstSwitch + "\", \"name\":\"tmp2-1\", \"cookie\":\"0\", \"priority\":\"5\", "
                                + "\"src-mac\":\"" + srcMac + "\", \"ingress-getPort()\":\"" + dstIngressPort + "\", "
                                + "\"dst-mac\": \"" + dstMac + "\", \"active\":\"true\",\"vlan-id\":\"-1\", "
                                + "\"actions\":\"output=" + dstOutput + "\"}";


                        String rule22 = "{\"switch\": \"" + dstSwitch + "\", \"name\":\"tmp2-2\", \"cookie\":\"0\", \"priority\":\"5\", "
                                + "\"src-mac\":\"" + dstMac + "\", \"ingress-getPort()\":\"" + dstOutput + "\", "
                                + "\"dst-mac\": \"" + srcMac + "\", \"active\":\"true\",\"vlan-id\":\"-1\", "
                                + "\"actions\":\"output=" + dstIngressPort + "\"}";




                        List<String> rules = new ArrayList<>();
                        rules.add(rule11);
                        rules.add(rule12);
                        rules.add(rule21);
                        rules.add(rule22);
                        try {
                            new SDNSweep(null).pushFlows(rules);
                        } catch (IOException ex) {
                            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ParserConfigurationException ex) {
                            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (SAXException ex) {
                            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(SDNControllerClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };

            thread.start();
        }
    }
//    private SDNSweep.FloodlightStats[] getFloodlightPortStats(String dpi, int getPort()) throws IOException, InterruptedException {
//        SDNSweep.FloodlightStats stats1 = null;
//        SDNSweep.FloodlightStats stats2 = null;
//        //        List<FloodlightStats> stats1 = getFloodlightPortStats(dpi);
//        //        Thread.sleep((long) interval);
//        //        List<FloodlightStats> stats2 = getFloodlightPortStats(dpi);
//        Map<String, SDNSweep.StatsHolder> map = SDNSweep.getStatsMap();
//        if (map != null) {
//            SDNSweep.StatsHolder h = map.get(dpi+"-"+getPort());
//            if (h != null) {
//                stats1 = h.getStats1();
//                stats2 = h.getStats2();
//            }
//        }
//        SDNSweep.FloodlightStats stat1 = null;
//        for (SDNSweep.FloodlightStats s : stats1) {
//            if (s.portNumber == getPort()) {
//                stat1 = s;
//                break;
//            }
//        }
//
//        SDNSweep.FloodlightStats stat2 = null;
//        for (SDNSweep.FloodlightStats s : stats2) {
//            if (s.portNumber == getPort()) {
//                stat2 = s;
//                break;
//            }
//        }
//        return new SDNSweep.FloodlightStats[]{stats1, stats2};
//    }
//    private String[] getsFlowPort(String v1, String v2) {
//        String[] tuple = new String[2];
//        if (sFlowHostPortMap == null) {
//            sFlowHostPortMap = new HashMap<>();
//        }
//        if (v1.contains(":") && v2.contains(":")) {
//            String switch1IP = getSwitchIPFromDPI(v1);
////            String switch2IP = getSwitchIPFromDPI(v2);
//            if (!sFlowHostPortMap.containsKey(switch1IP)) {
//                List<Flow> flows = getAgentFlows(switch1IP);
//                for (Flow f : flows) {
//                    String[] keys = f.flowKeys.split(",");
//                    String from = keys[0];
//                    String to = keys[1];
//                    if (!isAttached(from, v1) && isAttached(to, v1)) {
////                        Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "Switch: " + switch1IP + " -> " + f.dataSource);
//                        sFlowHostPortMap.put(switch1IP, f.dataSource);
//                        break;
//                    }
//                }
//            }
////            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "Host: " + switch1IP + " getPort(): " + sFlowHostPortMap.get(switch1IP));
//            tuple[0] = switch1IP;
//            tuple[1] = String.valueOf(sFlowHostPortMap.get(switch1IP));
//            return tuple;
//        } else {
//            String switchIP = null;
//            String hostIP = null;
//            if (v1.contains(".")) {
//                switchIP = getSwitchIPFromHostIP(v1);
//                hostIP = v1;
//            } else {
//                switchIP = getSwitchIPFromHostIP(v2);
//                hostIP = v2;
//            }
//
//            if (!sFlowHostPortMap.containsKey(hostIP)) {
//                List<Flow> flows = getAgentFlows(switchIP);
//                for (Flow f : flows) {
//                    String[] keys = f.flowKeys.split(",");
//                    if (keys[0].equals(hostIP)) {
//                        sFlowHostPortMap.put(hostIP, f.dataSource);
//                        break;
//                    }
//                }
//            }
//            Logger.getLogger(SDNControllerClient.class.getName()).log(Level.INFO, "Host: " + hostIP + " is attached to: " + switchIP + " getPort(): " + sFlowHostPortMap.get(hostIP));
//            tuple[0] = switchIP;
//            tuple[1] = String.valueOf(sFlowHostPortMap.get(hostIP));
//            return tuple;
//        }
//    }
//
//    private Map<String, Integer> getifNameOpenFlowPortNumberMap(String dpi) {
//        HashMap<String, Integer> ifNamePortMap = new HashMap<>();
//        List<Switch> switches = getSwitches();
//        for (Switch s : switches) {
//            if (s.dpid.equals(dpi)) {
//                List<Port> ports = s.ports;
//                for (Port p : ports) {
//                    ifNamePortMap.put(p.name, p.portNumber);
//                }
//                break;
//            }
//        }
//        return ifNamePortMap;
//    }
//
//    private String getSwitchIPFromDPI(String dpi) {
//        for (Switch s : getSwitches()) {
//            if (s.dpid.equals(dpi)) {
//                return s.inetAddress.split(":")[0].substring(1);
//            }
//        }
//        return null;
//    }
//
//    private boolean isAttached(String from, String dpi) {
//        for (NetworkEntity ne : getNetworkEntity(from)) {
//            for (AttachmentPoint ap : ne.getAttachmentPoint()) {
//                if (ap.getSwitchDPID().equals(dpi)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//    private List<NetworkEntity> getNetworkEntity(String address) {
//        if (networkEntityCache == null) {
//            networkEntityCache = new HashMap();
//        }
//        if (!networkEntityCache.containsKey(address)) {
//            WebResource webResource = client.resource(uri + ":" + floodlightPort);
//            WebResource res = null;
//            if (address.contains(".")) {
//                // http://145.100.133.131:8080/wm/device/?getIpv4()=192.168.100.1
//                res = webResource.path("wm").path("device/").queryParam("getIpv4()", address);
//            } else {
//                // http://145.100.133.131:8080/wm/device/?getMac()=fe:16:3e:00:26:b1
//                res = webResource.path("wm").path("device/").queryParam("getMac()", address);
//            }
//            List<NetworkEntity> ne = res.get(new GenericType<List<NetworkEntity>>() {
//            });
//            networkEntityCache.put(address, ne);
//        }
//        return networkEntityCache.get(address);
//    }
//
//    private List<NetworkEntity> getNetworkEntity(Set<String> sources) {
//        List<NetworkEntity> entities = new ArrayList<>();
//        for (String e : sources) {
//            entities.addAll(getNetworkEntity(e));
//        }
//        return entities;
//    }
//
//    private List<Link> getSwitchLinks() {
//        if (linkCache == null) {
//            linkCache = new ArrayList<>();
//        }
//        if (linkCache.isEmpty()) {
//            WebResource webResource = client.resource(uri + ":" + floodlightPort);
//            WebResource res = webResource.path("wm").path("topology").path("links").path("json");
//            linkCache = res.get(new GenericType<List<Link>>() {
//            });
//        }
//
//        return linkCache;
//    }
//
//    private List<Switch> getSwitches() {
//        if (switches == null) {
//            WebResource webResource = client.resource(uri + ":" + floodlightPort);
//            WebResource res = webResource.path("wm").path("core").path("controller").path("switches").path("json");
//            switches = res.get(new GenericType<List<Switch>>() {
//            });
//        }
//        return switches;
//    }
//
//    private String getSwitchIPFromHostIP(String address) {
//        if (networkEntitySwitchMap == null) {
//            networkEntitySwitchMap = new HashMap<>();
//        }
//        if (!networkEntitySwitchMap.containsKey(address)) {
//            List<NetworkEntity> ne = getNetworkEntity(address);
//            String dpi = ne.get(0).getAttachmentPoint().get(0).getSwitchDPID();
//            for (Switch switches : getSwitches()) {
//                if (switches.dpid.equals(dpi)) {
//                    String ip = switches.inetAddress.split(":")[0].substring(1);
//                    networkEntitySwitchMap.put(address, ip);
//                    break;
//                }
//            }
//        }
//
//        return networkEntitySwitchMap.get(address);
//    }
//
//    private List<Flow> getAgentFlows(String switchIP) {
//        List<Flow> agentFlows = new ArrayList<>();
//        for (Flow f : getAllFlows()) {
//            if (f.agent.equals(switchIP)) {
//                agentFlows.add(f);
//            }
//        }
//        return agentFlows;
//    }
//
//    private List<Flow> getAllFlows() {
//        WebResource webResource = client.resource(uri + ":" + sflowRTPrt);
//        WebResource res = webResource.path("flows").path("json");
//        return res.get(new GenericType<List<Flow>>() {
//        });
//    }
//
//    private List<Ifpkts> getifoutpktsMetric(String agent, int getPort()) {
//        WebResource webResource = client.resource(uri + ":" + sflowRTPrt);
//        WebResource res = webResource.path("metric").path(agent).path(getPort() + ".ifoutpkts").path("json");
//        return res.get(new GenericType<List<Ifpkts>>() {
//        });
//    }
//
//    private List<FloodlightStats> getFloodlightPortStats(String dpi) throws IOException {
//        //http://145.100.133.130:8080/wm/core/switch/00:00:4e:cd:a6:8d:c9:44/getPort()/json
//        WebResource webResource = client.resource(uri + ":" + floodlightPort);
//        WebResource res = webResource.path("wm").path("core").path("switch").path(dpi).path("getPort()").path("json");
//
//
//        String output = res.get(String.class);
//        String out = output.substring(27, output.length() - 1);
//
//        ObjectMapper mapper = new ObjectMapper();
//        return mapper.readValue(out, mapper.getTypeFactory().constructCollectionType(List.class, FloodlightStats.class));
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class Ifpkts {
//
//        @XmlElement(name = "agent")
//        String agent;
//        @XmlElement(name = "dataSource")
//        int dataSource;
//        @XmlElement(name = "lastUpdate")
//        long lastUpdate;
//        /**
//         * The lastUpdateMax and lastUpdateMin values indicate how long ago (in
//         * milliseconds) the most recent and oldest updates
//         */
//        @XmlElement(name = "lastUpdateMax")
//        long lastUpdateMax;
//        @XmlElement(name = "lastUpdateMin")
//        /**
//         * The metricN field in the query result indicates the number of data
//         * sources that contributed to the summary metrics
//         */
//        long lastUpdateMin;
//        @XmlElement(name = "metricN")
//        int metricN;
//        @XmlElement(name = "metricName")
//        String metricName;
//        @XmlElement(name = "metricValue")
//        double value;
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class Flow {
//
//        @XmlElement(name = "agent")
//        String agent;
//        @XmlElement(name = "dataSource")
//        int dataSource;
//        @XmlElement(name = "end")
//        String end;
//        @XmlElement(name = "flowID")
//        int flowID;
//        @XmlElement(name = "flowKeys")
//        String flowKeys;
//        @XmlElement(name = "name")
//        String name;
//        @XmlElement(name = "start")
//        long start;
//        @XmlElement(name = "value")
//        double value;
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class NetworkEntity {
//
//        @XmlElement(name = "entityClass")
//        String entityClass;
//        @XmlElement(name = "lastSeen")
//        String lastSeen;
//        @XmlElement(name = "getIpv4()")
//        List<String> getIpv4();
//        @XmlElement(name = "vlan")
//        List<String> vlan;
//        @XmlElement(name = "getMac()")
//        List<String> getMac();
//        @XmlElement(name = "getAttachmentPoint()")
//        List<AttachmentPoint> getAttachmentPoint();
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class AttachmentPoint {
//
//        @XmlElement(name = "getPort()")
//        int getPort();
//        @XmlElement(name = "errorStatus")
//        String errorStatus;
//        @XmlElement(name = "getSwitchDPID()")
//        String getSwitchDPID();
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    private static class Link {
//
//        @XmlElement(name = "src-switch")
//        String srcSwitch;
//        @XmlElement(name = "src-getPort()")
//        int getSrcPort();
//        @XmlElement(name = "dst-switch")
//        String dstSwitch;
//        @XmlElement(name = "dst-getPort()")
//        int dstPort;
//        @XmlElement(name = "type")
//        String type;
//        @XmlElement(name = "direction")
//        String direction;
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class Switch {
//
//        @XmlElement(name = "actions")
//        int actions;
//        @XmlElement(name = "attributes")
//        Attributes attributes;
//        @XmlElement(name = "ports")
//        List<Port> ports;
//        @XmlElement(name = "buffers")
//        int buffers;
//        @XmlElement(name = "description")
//        Description description;
//        @XmlElement(name = "capabilities")
//        int capabilities;
//        @XmlElement(name = "inetAddress")
//        String inetAddress;
//        @XmlElement(name = "connectedSince")
//        long connectedSince;
//        @XmlElement(name = "dpid")
//        String dpid;
//        @XmlElement(name = "harole")
//        String harole;
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    private static class Description {
//
//        @XmlElement(name = "software")
//        String software;
//        @XmlElement(name = "hardware")
//        String hardware;
//        @XmlElement(name = "manufacturer")
//        String manufacturer;
//        @XmlElement(name = "serialNum")
//        String serialNum;
//        @XmlElement(name = "datapath")
//        String datapath;
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    private static class Port {
//
//        @XmlElement(name = "portNumber")
//        int portNumber;
//        @XmlElement(name = "hardwareAddress")
//        String hardwareAddress;
//        @XmlElement(name = "name")
//        String name;
//        @XmlElement(name = "config")
//        int config;
//        @XmlElement(name = "state")
//        int state;
//        @XmlElement(name = "currentFeatures")
//        int currentFeatures;
//        @XmlElement(name = "advertisedFeatures")
//        int advertisedFeatures;
//        @XmlElement(name = "supportedFeatures")
//        int supportedFeatures;
//        @XmlElement(name = "peerFeatures")
//        int peerFeatures;
//    }
//
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    private static class Attributes {
//
//        @XmlElement(name = "supportsOfppFlood")
//        boolean supportsOfppFlood;
//        @XmlElement(name = "supportsNxRole")
//        boolean supportsNxRole;
//        @XmlElement(name = "FastWildcards")
//        int fastWildcards;
//        @XmlElement(name = "supportsOfppTable")
//        boolean supportsOfppTable;
//    }
//
////    @XmlRootElement
////    @XmlAccessorType(XmlAccessType.FIELD)
//////    @JsonIgnoreProperties(ignoreUnknown = true)
////    public static class FloodlightStatsWrapper {
////
//////        @XmlElement(name = "00:00:4e:cd:a6:8d:c9:44")
////        @XmlElementWrapper
//////        @XmlElement
////        List<FloodlightStats> stats;
////    }
//    @XmlRootElement
//    @XmlAccessorType(XmlAccessType.FIELD)
//    public static class FloodlightStats {
//
//        @JsonProperty("portNumber")
//        int portNumber;
//        @JsonProperty("receivePackets")
//        long receivePackets;
//        @JsonProperty("transmitPackets")
//        long transmitPackets;
//        @JsonProperty("receiveBytes")
//        long receiveBytes;
//        @JsonProperty("transmitBytes")
//        long transmitBytes;
//        @JsonProperty("receiveDropped")
//        long receiveDropped;
//        @JsonProperty("transmitDropped")
//        long transmitDropped;
//        @JsonProperty("receiveErrors")
//        long receiveErrors;
//        @JsonProperty("transmitErrors")
//        long transmitErrors;
//        @JsonProperty("receiveFrameErrors")
//        long receiveFrameErrors;
//        @JsonProperty("receiveOverrunErrors")
//        long receiveOverrunErrors;
//        @JsonProperty("receiveCRCErrors")
//        long receiveCRCErrors;
//        @JsonProperty("collisions")
//        long collisions;
//    }
}
