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
    private static List<Link> linkCache;
    private List<Switch> switches;
    private long interval = 1000;
    @Getter
    private static Map<String, StatsHolder> statsMap;

    public SDNSweep(DataSource datasource) throws IOException {
        this.datasource = datasource;
        uri = PropertiesHelper.getSDNControllerURL();
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);

        init();
    }

    private void init() {
        getAllNetworkEntites();
        getSwitchLinks();
        getAllSwitches();
    }

    @Override
    public void run() {
        try {
            updateMtrics();
        } catch (IOException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SDNSweep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateMtrics() throws IOException, InterruptedException {
        if (statsMap == null) {
            statsMap = new HashMap<>();
        }
        for (Switch sw : getAllSwitches()) {
            List<FloodlightStats> stats1 = getFloodlightPortStats(sw.dpid);
            Thread.sleep(interval);
            List<FloodlightStats> stats2 = getFloodlightPortStats(sw.dpid);
            statsMap.put(sw.dpid, new StatsHolder(stats1, stats2));
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
                if (networkEntitesCache.containsKey(ne.ipv4.get(0))) {
                    networkEntitesCache.put(ne.ipv4.get(0), ne);
                }
            }
        }
        return networkEntitesCache.values();
    }

    private List<Link> getSwitchLinks() {
        if (linkCache == null) {
            linkCache = new ArrayList<>();
        }
        if (linkCache.isEmpty()) {
            WebResource webResource = client.resource(uri + ":" + floodlightPort);
            WebResource res = webResource.path("wm").path("topology").path("links").path("json");
            linkCache = res.get(new GenericType<List<Link>>() {
            });
        }
        return linkCache;
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
        String agent;
        @XmlElement(name = "dataSource")
        int dataSource;
        @XmlElement(name = "lastUpdate")
        long lastUpdate;
        /**
         * The lastUpdateMax and lastUpdateMin values indicate how long ago (in
         * milliseconds) the most recent and oldest updates
         */
        @XmlElement(name = "lastUpdateMax")
        long lastUpdateMax;
        @XmlElement(name = "lastUpdateMin")
        /**
         * The metricN field in the query result indicates the number of data
         * sources that contributed to the summary metrics
         */
        long lastUpdateMin;
        @XmlElement(name = "metricN")
        int metricN;
        @XmlElement(name = "metricName")
        String metricName;
        @XmlElement(name = "metricValue")
        double value;
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

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FloodlightStats {

        @JsonProperty("portNumber")
        int portNumber;
        @JsonProperty("receivePackets")
        long receivePackets;
        @JsonProperty("transmitPackets")
        long transmitPackets;
        @JsonProperty("receiveBytes")
        long receiveBytes;
        @JsonProperty("transmitBytes")
        long transmitBytes;
        @JsonProperty("receiveDropped")
        long receiveDropped;
        @JsonProperty("transmitDropped")
        long transmitDropped;
        @JsonProperty("receiveErrors")
        long receiveErrors;
        @JsonProperty("transmitErrors")
        long transmitErrors;
        @JsonProperty("receiveFrameErrors")
        long receiveFrameErrors;
        @JsonProperty("receiveOverrunErrors")
        long receiveOverrunErrors;
        @JsonProperty("receiveCRCErrors")
        long receiveCRCErrors;
        @JsonProperty("collisions")
        long collisions;
    }

    private class StatsHolder {

        @Getter
        private final List<FloodlightStats> stats1;
        @Getter
        private final List<FloodlightStats> stats2;

        public StatsHolder(List<FloodlightStats> stats1, List<FloodlightStats> stats2) {
            this.stats1 = stats1;
            this.stats2 = stats2;
        }
    }
}
