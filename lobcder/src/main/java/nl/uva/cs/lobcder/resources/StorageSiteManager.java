/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import com.sun.org.apache.xml.internal.utils.StopParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import nl.uva.cs.lobcder.webdav.Constants.Constants;

/**
 *
 * @author S. Koulouzis
 */
public class StorageSiteManager {

    private final PersistenceManagerFactory pmf;

    public StorageSiteManager() {
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
    }

    public Collection<StorageSite> getSitesByUname(String vphUsername) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<StorageSite> results;
        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(StorageSite.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("vphUsername == vphUsername");
            results = (Collection<StorageSite>) q.execute(vphUsername);

            if (!results.isEmpty()) {
                for (StorageSite s : results) {
                    debug("getSites. endpoint: " + s.getEndpoint());
                }
            }

            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }
        return results;
    }

    public void registerStorageSite(Properties prop) throws Exception {
        Credential cred = new Credential(prop.getProperty(Constants.VPH_USERNAME));
        cred.setStorageSiteUsername(prop.getProperty(Constants.STORAGE_SITE_USERNAME));
        cred.setStorageSitePassword(prop.getProperty(Constants.STORAGE_SITE_PASSWORD));
        String endpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);

        if (!storageSiteExists(prop)) {
            debug("Adding endpoint: " + endpoint);

            StorageSite site = new StorageSite(endpoint, cred);

            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();

                pm.makePersistent(site);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                site.getEndpoint();
                pm.close();
            }

        }

    }

    private void debug(String msg) {
        System.err.println(this.getClass().getName() + ": " + msg);
    }

    public Collection<StorageSite> getSitesByLPath(Path lDRI) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<StorageSite> results;
        ArrayList<StorageSite> resultsWithPaths = new ArrayList<StorageSite>();


        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(StorageSite.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
//            q.setFilter("logicalPaths.contains(lDRI.getName())");
            results = (Collection<StorageSite>) q.execute(lDRI.getName());
            if (!results.isEmpty()) {
                for (StorageSite s : results) {
                    if (s.getLogicalPaths().contains(lDRI.getName())) {
                        resultsWithPaths.add(s);
                    }
                    debug("getSites. endpoint: " + s.getEndpoint());
                }
            }

            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }
        return resultsWithPaths;
    }

    void cleaSite(String endpoint) {

        //Next the node 
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            Query q = pm.newQuery(StorageSite.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("endpoint == endpoint");
            Collection<StorageSite> results = (Collection<StorageSite>) q.execute(endpoint);
            if (!results.isEmpty()) {
                for (StorageSite s : results) {
                    if (endpoint.equals(s.getEndpoint())) {
                        pm.deletePersistent(s);
                        break;
                    }
                }
            }
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    public Collection<StorageSite> getAllSites() {
        //Next the node 
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            Query q = pm.newQuery(StorageSite.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("endpoint == endpoint");
            Collection<StorageSite> results = (Collection<StorageSite>) q.execute();
            tx.commit();

            return results;

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    public void clearAllSites() {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            Query q = pm.newQuery(StorageSite.class);
            Collection<StorageSite> results = (Collection<StorageSite>) q.execute();
            pm.deletePersistentAll(results);
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    public void deleteStorgaeSites(Collection<StorageSite> storageSites) {
        for (StorageSite s : storageSites) {
            deleteStorageSite(s);
        }
    }

    private void deleteStorageSite(StorageSite s) {
//        debug("delete Endpint:  >>>>>>>>>>>>"+s.getEndpoint());
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            Query q = pm.newQuery(StorageSite.class);
            q.setFilter("endpoint == s.getEndpoint");
            Collection<StorageSite> results = (Collection<StorageSite>) q.execute(s);
            if (!results.isEmpty()) {
                for (StorageSite e : results) {
                    if (e.getEndpoint().equals(s.getEndpoint())) {
                        pm.deletePersistent(e);
                    }
                }
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    public boolean storageSiteExists(Properties prop) throws Exception {
        String uname = prop.getProperty(Constants.VPH_USERNAME);
        String ssUname = Constants.STORAGE_SITE_USERNAME;
        String endpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);


        Collection<StorageSite> ss;
        StorageSite storageSite = null;
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        int hit = 0;

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(StorageSite.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("endpoint == endpoint && vphUsername == uname");
            q.declareParameters(endpoint.getClass().getName() + " endpoint, " + uname.getClass().getName() + " uname");

            ss = (Collection<StorageSite>) q.execute(endpoint, uname);

            for (StorageSite s : ss) {
                if (s.getEndpoint().equals(endpoint) && s.getVPHUsername().equals(uname)) {
                    hit++;
                    storageSite = s;
                }
            }
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }

        if (storageSite == null) {
            return false;
        } else {
            return true;
        }
    }

    void deleteStorgaeSite(Properties prop) throws Exception {
                Credential cred = new Credential(prop.getProperty(Constants.VPH_USERNAME));
        cred.setStorageSiteUsername(prop.getProperty(Constants.STORAGE_SITE_USERNAME));
        cred.setStorageSitePassword(prop.getProperty(Constants.STORAGE_SITE_PASSWORD));
        String endpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
        
        deleteStorageSite(new StorageSite(endpoint, cred));
    }
}
