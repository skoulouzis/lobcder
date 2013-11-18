/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import nl.uva.cs.lobcder.webDav.resources.WebDataFileResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author S. Koulouzis
 */
public class PropertiesHelper {

    public static final String propertiesPath = "/lobcder.properties";

    public static List<String> getWorkers() {
        ArrayList<String> workers = null;
        BufferedReader br = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("/workers");
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            workers = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                workers.add(line);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return workers;
    }

    public static List<String> getNonRedirectableUserAgents() {
        ArrayList<String> agants = null;
        BufferedReader br = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("/user-agents");
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            agants = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                agants.add(line);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return agants;
    }

    public static String getWorkerToken() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return properties.getProperty("worker.token");
    }

    public static boolean doAggressiveReplication() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return Boolean.valueOf(properties.getProperty("replication.aggressive", "false"));
    }

    public static boolean doRedirectGets() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return Boolean.valueOf(properties.getProperty("get.redirect", "false"));
    }

    public static boolean doRemoteAuth() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return Boolean.valueOf(properties.getProperty("auth.use.remote", "true"));
    }

    public static String getMetadataReposetoryURL() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return properties.getProperty("metadata.reposetory.url", "http://vphshare.atosresearch.eu/metadata-retrieval/rest/metadata");
    }

    public static Boolean useMetadataReposetory() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return Boolean.valueOf(properties.getProperty("use.metadata.reposetory", "true"));
    }

    public static String getAuthRemoteURL() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return properties.getProperty("auth.remote.url", "https://jump.vph-share.eu/validatetkt/?ticket=");
    }

    public static int getDefaultRowLimit() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        return Integer.valueOf(properties.getProperty("default.rowlimit", "500"));
    }

    public static Map<String, String> getIPMap() {
        HashMap<String, String> ipMap = new HashMap<>();
        BufferedReader br = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("/ip-map");
            br = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = br.readLine()) != null) {
                String[] keyValue = line.split(" ");
                ipMap.put(keyValue[0], keyValue[1]);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ipMap;
    }
}
