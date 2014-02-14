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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author S. Koulouzis
 */
public class PropertiesHelper {

    public static final String propertiesPath = "lobcder.properties";

    private static Properties getProperties() throws IOException {
        InputStream in = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            in = classLoader.getResourceAsStream(propertiesPath);
            Properties properties = new Properties();
            properties.load(in);

            return properties;
        } catch (IOException ex) {
            Logger.getLogger(PropertiesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return null;
    }

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
        return getProperties().getProperty("worker.token");
    }

    public static boolean doAggressiveReplication() throws IOException {

        return Boolean.valueOf(getProperties().getProperty("replication.aggressive", "false"));
    }

    public static boolean doRedirectGets() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("get.redirect", "false"));
    }

//    public static boolean doRemoteAuth() throws IOException {
//        return Boolean.valueOf(getProperties().getProperty("auth.use.remote", "true"));
//    }

    public static String getMetadataReposetoryURL() throws IOException {
        return getProperties().getProperty("metadata.reposetory.url", "http://vphshare.atosresearch.eu/metadata-retrieval/rest/metadata");
    }

    public static Boolean useMetadataReposetory() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("use.metadata.repository", "true"));
    }

//    public static String getAuthRemoteURL() throws IOException {
//        return getProperties().getProperty("auth.remote.url", "https://jump.vph-share.eu/validatetkt/?ticket=");
//    }

    public static int getDefaultRowLimit() throws IOException {
        return Integer.valueOf(getProperties().getProperty("default.rowlimit", "500"));
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
            in.close();
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

    public static boolean doPrediction() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("do.prediction", "false"));
    }

    public static boolean doRequestLoging() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("do.request.loging", "true"));
    }

    public static Set<String> getAllowedOrigins() throws IOException {
        return new HashSet<>(Arrays.asList (getProperties().getProperty("allowed.origins").split(",")));
    }

    public static String getFloodLightURL() throws IOException {
        return getProperties().getProperty("floodlight.url");
    }

    public static HashMap<Integer, String> getPortWorkerMap() {
         HashMap<Integer, String> ipMap = new HashMap<>();
        BufferedReader br = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("/port-map");
            br = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = br.readLine()) != null) {
                String[] keyValue = line.split(" ");
                ipMap.put(Integer.valueOf(keyValue[0]), keyValue[1]);
            }
            br.close();
            in.close();
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
