/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;

/**
 *
 * @author S. Koulouzis
 */
public class Network {

    public static String getIP(String host) {
        if (host == null) {
            return host;
        }
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException ex) {
            return host;
        }
    }

    public static String replaceIP(String host) throws MalformedURLException {
        if (host == null) {
            return host;
        }
        URL hostURL;
        host = Network.getIP(host);
        Map<String, String> map = PropertiesHelper.getIPMap();
        if (map == null || map.isEmpty()) {
            return host;
        }
        try {
            hostURL = new URL(host);
        } catch (MalformedURLException ex) {
            if (map.containsKey(host)) {
                String mapedIP = map.get(host);
                return mapedIP;
            }
            return host;
        }

        if (map.containsKey(hostURL.getHost())) {
            String mapedIP = map.get(hostURL.getHost());
            return new URL(hostURL.getProtocol(), mapedIP, hostURL.getPort(), hostURL.getFile()).toString();
        }
        return host;
    }
}
