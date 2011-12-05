/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

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
    private final String vphUsername;

    public StorageSiteManager(String vphUsername) {
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
        this.vphUsername = vphUsername;
    }

    public ArrayList<StorageSite> getSites() {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(StorageSite.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("vphUsername == vphUsername");
            Collection<StorageSite> results = (Collection<StorageSite>) q.execute(vphUsername);
            
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
        return null;
    }

    public void registerStorageSite(Properties prop) throws Exception {
        Credential cred = new Credential();
        cred.setStorageSiteUsername(prop.getProperty(Constants.STORAGE_SITE_USERNAME));
        cred.setStorageSitePassword(prop.getProperty(Constants.STORAGE_SITE_PASSWORD));
        cred.setVPHUsernname(prop.getProperty(Constants.VPH_USERNAME));
        String endpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
        
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

    private void debug(String msg) {
        System.err.println(this.getClass().getName() + ": " + msg);
    }
}
