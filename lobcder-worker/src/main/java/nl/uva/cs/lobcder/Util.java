/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author S. Koulouzis
 */
class Util {

    public static Properties getProperties()
            throws IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Properties prop;
        try (InputStream in = classLoader.getResourceAsStream("/lobcder-worker.properties")) {
            prop = new Properties();
            prop.load(in);
        }
        return prop;
    }

    public static String getIP(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException ex) {
            return hostName;
        }
    }

    public static List<String> getAllIPs() throws UnknownHostException, SocketException {
//        InetAddress localhost = InetAddress.getLocalHost();
//        InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());
        List<String> ips = new ArrayList<>();
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress next = enumIpAddr.nextElement();
                ips.add(next.getHostAddress());
            }
        }
        return ips;
    }

    public static int getBufferSize() throws IOException {
        return Integer.valueOf(getProperties().getProperty(("buffer.size"), "4194304"));
    }

    static String getRestURL() throws IOException {
        return getProperties().getProperty(("rest.url"), "http://localhost:8080/lobcder/rest/");
    }

    static String getRestPassword() throws IOException {
        return getProperties().getProperty(("rest.password"));
    }

    static Boolean isResponseBufferSize() throws IOException {
        return Boolean.valueOf(getProperties().getProperty(("response.set.buffer.size"), "false"));
    }

    static Boolean isCircularBuffer() throws IOException {
        return Boolean.valueOf(getProperties().getProperty(("use.circular.buffer"), "true"));
    }
}