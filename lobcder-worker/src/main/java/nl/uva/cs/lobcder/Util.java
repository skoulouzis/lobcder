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
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 *
 * @author S. Koulouzis
 */
@Log
class Util extends PropertiesHelper {

    static enum ChacheEvictionAlgorithm {

        LRU, MRU, RR, LFU, MFU
    }

    public static Properties getProperties()
            throws IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Properties prop;
        try (InputStream in = classLoader.getResourceAsStream("/lobcder.properties")) {
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
                if (next != null) {
                    String ip = next.getHostAddress();
                    if (ip != null) {
                        ips.add(ip);
                    }
                }
//                Logger.getLogger(Util.class.getName()).log(Level.FINE, "ip: {0}", ip);
            }
        }
        ips.add(InetAddress.getLocalHost().getHostName());
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

    static double getRateOfChangeLim() throws IOException {
        return Double.valueOf(getProperties().getProperty(("rate.of.change"), "2"));
    }

    static boolean doQosCopy() throws IOException {
        return Boolean.valueOf(getProperties().getProperty(("do.qos.copy"), "false"));
    }

    static int getNumOfWarnings() throws IOException {
        return Integer.valueOf(getProperties().getProperty(("num.of.warnings"), "3"));
    }

    static double getProgressThresshold() throws IOException {
        return Double.valueOf(getProperties().getProperty(("progress.thresshold"), "10"));
    }

    static double getProgressThressholdCoefficient() throws IOException {
        return Double.valueOf(getProperties().getProperty(("progress.thresshold.coefficient"), "-0.0024"));
    }

    static boolean dropConnection() throws IOException {
        return Boolean.valueOf(getProperties().getProperty(("drop.connection"), "false"));
    }

    static boolean getOptimizeFlow() throws IOException {
        return Boolean.valueOf(getProperties().getProperty(("optimize.flow"), "true"));
    }

    static boolean sendStats() throws IOException {
        return Boolean.valueOf(getProperties().getProperty(("stats.send"), "false"));
    }

    static ChacheEvictionAlgorithm getCacheEvictionPolicy() throws IOException {
        return ChacheEvictionAlgorithm.valueOf(getProperties().getProperty(("cache.eviction.alg"), "LRU"));
    }

    public static long getCacheFreeSpaceLimit() throws IOException {
        return Long.valueOf(getProperties().getProperty("cache.size.limit", "2000000000"));
    }
}