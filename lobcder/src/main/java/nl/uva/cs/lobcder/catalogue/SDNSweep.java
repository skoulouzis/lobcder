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
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class SDNSweep implements Runnable {

    private final DataSource datasource;
    private final String uri;
    private final int floodlightPort = 8080;
    private final int sflowRTPrt = 8008;
    private final Client client;
    private static Map<String, NetworkEntity> networkEntitesCache;
    @Getter
    private static List<Link> switchLinks;
    private List<Switch> switches;
    private long interval = 1000;
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

    public SDNSweep(DataSource datasource) throws IOException {
        this.datasource = datasource;
        uri = PropertiesHelper.getSDNControllerURL();
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);
    }

    private void init() {
        getAllNetworkEntites();
        getAllSwitchLinks();
        getAllSwitches();
    }

    public static NetworkEntity getNetworkEntity(String dest) {
        return networkEntitesCache.get(dest);
    }

    @Override
    public void run() {
        try {
            init();
            updateMtrics();
        } catch (IOException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateMtrics() throws IOException, InterruptedException {
        for (Switch sw : getAllSwitches()) {
            List<FloodlightStats> stats1 = getFloodlightPortStats(sw.dpid);
            Thread.sleep(interval);
            List<FloodlightStats> stats2 = getFloodlightPortStats(sw.dpid);

            for (int i = 0; i < stats1.size(); i++) {
//                for(String mn  : METRIC_NAMES){
//                    String key = mn+"-"+sw.dpid + "-" + stats1.get(i).portNumber;
//                }

                Double val = collisionsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                Double oldValue = ((val == null) ? 1.0 : val);
                Double newValue = Double.valueOf(stats1.get(i).collisions - stats2.get(i).collisions);
                newValue = (newValue + oldValue) / 2.0;
                collisionsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);

                val = receiveBytesMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).receiveBytes - stats2.get(i).receiveBytes);
                val = ((newValue > oldValue) ? newValue : oldValue);
                receiveBytesMap.put(sw.dpid + "-" + stats1.get(i).portNumber, val);




                val = receiveCRCErrorsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).receiveCRCErrors - stats2.get(i).receiveCRCErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveCRCErrorsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);


                val = receiveDroppedMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).receiveDropped - stats2.get(i).receiveDropped);
                newValue = (newValue + oldValue) / 2.0;
                receiveDroppedMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);



                val = receiveErrorsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).receiveErrors - stats2.get(i).receiveErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveErrorsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);


                val = receiveFrameErrorsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).receiveFrameErrors - stats2.get(i).receiveFrameErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveErrorsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);

                val = receiveOverrunErrorsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).receiveOverrunErrors - stats2.get(i).receiveOverrunErrors);
                newValue = (newValue + oldValue) / 2.0;
                receiveOverrunErrorsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);



                val = receivePacketsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).receivePackets - stats2.get(i).receivePackets);
                val = ((newValue > oldValue) ? newValue : oldValue);
                receivePacketsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, val);



                val = transmitBytesMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).transmitBytes - stats2.get(i).transmitBytes);
                val = ((newValue > oldValue) ? newValue : oldValue);
                transmitBytesMap.put(sw.dpid + "-" + stats1.get(i).portNumber, val);



                val = transmitDroppedMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).transmitDropped - stats2.get(i).transmitDropped);
                newValue = (newValue + oldValue) / 2.0;
                transmitDroppedMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);


                val = transmitErrorsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).transmitErrors - stats2.get(i).transmitErrors);
                newValue = (newValue + oldValue) / 2.0;
                transmitErrorsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, newValue);

                val = transmitPacketsMap.get(sw.dpid + "-" + stats1.get(i).portNumber);
                oldValue = ((val == null) ? 1.0 : val);
                newValue = Double.valueOf(stats1.get(i).transmitPackets - stats2.get(i).transmitPackets);
                val = ((newValue > oldValue) ? newValue : oldValue);
                transmitPacketsMap.put(sw.dpid + "-" + stats1.get(i).portNumber, val);

            }
        }
    }

    private List<FloodlightStats> getFloodlightPortStats(String dpi) throws IOException {
        //http://145.100.133.130:8080/wm/core/switch/00:00:4e:cd:a6:8d:c9:44/port/json
        WebResource webResource = client.resource(uri + ":" + floodlightPort);
        WebResource res = webResource.path("wm").path("core").path("switch").path(dpi).path("port").path("json");
        String output = res.get(String.class);
        String out = output.substring(27, output.length() - 1);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(out, mapper.getTypeFactory().constructCollectionType(List.class, FloodlightStats.class));
    }

    private Collection<NetworkEntity> getAllNetworkEntites() {
        if (networkEntitesCache == null) {
            networkEntitesCache = new HashMap<>();
        }
        if (networkEntitesCache.isEmpty()) {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = null;
            // http://145.100.133.131:8080/wm/device/?ipv4=192.168.100.1
            res = webResource.path("wm").path("device/");
            List<NetworkEntity> neList = res.get(new GenericType<List<NetworkEntity>>() {
            });

            for (NetworkEntity ne : neList) {
                if (!networkEntitesCache.containsKey(ne.ipv4.get(0))) {
                    networkEntitesCache.put(ne.ipv4.get(0), ne);
                }
            }
        }
        return networkEntitesCache.values();
    }

    private List<Link> getAllSwitchLinks() {
        if (switchLinks == null) {
            switchLinks = new ArrayList<>();
        }
        if (switchLinks.isEmpty()) {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = webResource.path("wm").path("topology").path("links").path("json");
            switchLinks = res.get(new GenericType<List<Link>>() {
            });
        }
        return switchLinks;
    }

    private List<Switch> getAllSwitches() {
        if (switches == null) {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = webResource.path("wm").path("core").path("controller").path("switches").path("json");
            switches = res.get(new GenericType<List<Switch>>() {
            });
        }
        return switches;
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
    public static class Flow {

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

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NetworkEntity {

        @XmlElement(name = "entityClass")
        public String entityClass;
        @XmlElement(name = "lastSeen")
        public String lastSeen;
        @XmlElement(name = "ipv4")
        public List<String> ipv4;
        @XmlElement(name = "vlan")
        public List<String> vlan;
        @XmlElement(name = "mac")
        public List<String> mac;
        @XmlElement(name = "attachmentPoint")
        public List<AttachmentPoint> attachmentPoint;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AttachmentPoint {

        @XmlElement(name = "port")
        public int port;
        @XmlElement(name = "errorStatus")
        public String errorStatus;
        @XmlElement(name = "switchDPID")
        public String switchDPID;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Link {

        @XmlElement(name = "src-switch")
        public String srcSwitch;
        @XmlElement(name = "src-port")
        public int srcPort;
        @XmlElement(name = "dst-switch")
        public String dstSwitch;
        @XmlElement(name = "dst-port")
        public int dstPort;
        @XmlElement(name = "type")
        public String type;
        @XmlElement(name = "direction")
        public String direction;
    }

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
        public int portNumber;
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

    public class StatsHolder {

        @Getter
        private final FloodlightStats stats1;
        @Getter
        private final FloodlightStats stats2;

        public StatsHolder(FloodlightStats stats1, FloodlightStats stats2) {
            this.stats1 = stats1;
            this.stats2 = stats2;
        }
    }
}
