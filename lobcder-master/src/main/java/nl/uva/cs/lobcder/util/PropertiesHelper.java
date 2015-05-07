/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import nl.uva.cs.lobcder.webDav.resources.WebDataFileResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author S. Koulouzis
 */
public class PropertiesHelper {

    public static final String propertiesPath = "lobcder.properties";
//    public static final String cachePropertiesPath = "cache.properties";

    public static enum PREDICTION_TYPE {

        method,
        resource,
        state
    }

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

    public static String getLobComponentToken() throws IOException {
        return getProperties().getProperty("worker.token");
    }

    public static String getReplicationPolicy() throws IOException {
        return getProperties().getProperty("replication.policy", "nl.uva.cs.lobcder.replication.policy.FirstSiteReplicationPolicy");
    }

    public static boolean doRedirectGets() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("redirect.get", "false"));
    }

//    public static boolean doRemoteAuth() throws IOException {
//        return Boolean.valueOf(getProperties().getProperty("auth.use.remote", "true"));
//    }
    public static String getMetadataRepositoryURL() throws IOException {
        return getProperties().getProperty("metadata.repository.url", "http://vphshare.atosresearch.eu/metadata-extended/rest/metadata");
    }

    public static String getMetadataRepositoryDevURL() throws IOException {
        return getProperties().getProperty("metadata.repository.test.url", "http://vphshare.atosresearch.eu/metadata-extended-test/rest/metadata");
    }

    public static Boolean useMetadataRepository() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("metadata.repository.use", "true"));
    }

    public static Boolean useBulckMetadataRepository() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("metadata.repository.bulck", "true"));
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
        return new HashSet<>(Arrays.asList(getProperties().getProperty("allowed.origins").split(",")));
    }

    public static String getSDNControllerURL() throws IOException {
        return getProperties().getProperty("sdn.controller.url");
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

    public static String getSchedulingAlg() throws IOException {
        return getProperties().getProperty("worker.selection.algorithm");
    }

    public static Boolean useSDN() throws IOException {
        return Boolean.valueOf(getProperties().getProperty("sdn.controller.use", "false"));
    }

    public static String getPredictorAlgorithm() throws IOException {
        return getProperties().getProperty("predictor.algorithm", "FirstSuccessor");
    }

    public static Integer getStableSuccessorN() throws IOException {
        return Integer.valueOf(getProperties().getProperty("stable.successor.N", "3"));
    }

    public static Integer RecentPopularityJ() throws IOException {
        return Integer.valueOf(getProperties().getProperty("recent.popularity.j", "5"));
    }

    public static Integer RecentPopularityK() throws IOException {
        return Integer.valueOf(getProperties().getProperty("recent.popularity.k", "3"));
    }

    public static Integer PredecessorPositionLen() throws IOException {
        return Integer.valueOf(getProperties().getProperty("predecessor.position.len", "3"));
    }

    public static Integer getFirstSuccessorrN() throws IOException {
        return Integer.valueOf(getProperties().getProperty("first.successor.N", "1"));
    }

    public static int KNN() throws IOException {
        return Integer.valueOf(getProperties().getProperty("knn", "10"));
    }

    public static PREDICTION_TYPE getPredictionType() throws IOException {
        return PREDICTION_TYPE.valueOf(getProperties().getProperty("predictor.type", "state"));
    }

    public static String getBackendWorkingFolderName() throws IOException {
        return getProperties().getProperty("backend.working.folder.name", "LOBCDER-REPLICA-vTEST");
    }

    public static Long getTmpDirUid() {
        Long result = null;
        try {
            String property = getProperties().getProperty("tmp.dir.uid", null);
            result = (property == null) ? null : Long.valueOf(property);
        } catch (Throwable th) {
        }
        return result;
    }

    public static int getNumberOfSites() throws IOException {
        return Integer.valueOf(getProperties().getProperty("replication.number.of.sites", "2"));
    }

    public static long getSweepersInterval() throws IOException {
        return Long.valueOf(getProperties().getProperty("sweepers.interval", "100"));
    }

    public static File getGeoDB() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//        URL url = classLoader.getResource("GeoLiteCityv6.dat");
//                URL url = classLoader.getResource("GeoIP.dat"); 

        URL url = classLoader.getResource(getProperties().getProperty("geolocation.db.file", "GeoLiteCity.dat"));
        String dbPath = url.getPath();
        return new File(dbPath);
    }

    public static long getCacheFreeSpaceLimit() throws IOException {
        return Long.valueOf(getProperties().getProperty("cache.size.limit", "2000000000"));
    }

    public static Long getLocalCacheId() throws IOException {
        return Long.valueOf(getProperties().getProperty("localcache.id"));
    }

    public static Integer getMaximumNumberOfRequests() throws IOException {
        return Integer.valueOf(getProperties().getProperty("maximum.num.requests", "15"));
    }
}
