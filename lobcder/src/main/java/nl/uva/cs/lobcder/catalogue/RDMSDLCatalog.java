/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.*;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.vlet.data.StringUtil;

/**
 *
 * @author S. Koulouzis
 */
public class RDMSDLCatalog implements IDLCatalogue {

    private static final Object lock = new Object();
    private final PersistenceManagerFactory pmf;

    public RDMSDLCatalog() {
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
    }

    @Override
    public void registerResourceEntry(ILogicalData entry) throws CatalogueException {

        //Check if it exists 
        String strLogicalResourceName = entry.getLDRI().toString();
        PersistenceManager pm = pmf.getPersistenceManager();

        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("strLDRI == strLogicalResourceName");
            q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
            q.setUnique(true);
            ILogicalData loaded = (ILogicalData) q.execute(strLogicalResourceName);

            if (loaded != null && comparePaths(loaded.getLDRI(), entry.getLDRI())) {
                throw new DuplicateResourceException("Cannot register resource " + entry.getLDRI() + " resource exists");
            }

            //If it has a parent node, add this path to the parent node 
            Path parentPath = entry.getLDRI().getParent();
            if (parentPath != null && !StringUtil.isEmpty(parentPath.toString()) && !parentPath.isRoot()) {
                strLogicalResourceName = parentPath.toString();
                q = pm.newQuery(LogicalData.class);
                //restrict to instances which have the field ldri equal to some logicalResourceName
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                q.setUnique(true);
                ILogicalData parentEntry = (ILogicalData) q.execute(strLogicalResourceName);
                if (parentEntry == null) {
                    throw new NonExistingResourceException("Cannot add " + entry.getLDRI().toString() + " child to non existing parent " + parentPath.toString());
                }
                parentEntry.addChild(entry.getLDRI());
            }
            //Persisst entry
            Collection<IStorageSite> storageSites = entry.getStorageSites();
            //work around to remove duplicated storage sites 
            if (storageSites != null && !storageSites.isEmpty()) {
                for (IStorageSite s : storageSites) {
                    String uname = s.getVPHUsername();
                    String epoint = s.getEndpoint();
                    q = pm.newQuery(StorageSite.class);

                    q.setFilter("vphUsername == uname && endpoint == epoint");
                    q.declareParameters(uname.getClass().getName() + " uname, " + epoint.getClass().getName() + " epoint");
//                    Collection<StorageSite> results = (Collection<StorageSite>) q.execute(uname, epoint);
                    Long number = (Long) q.deletePersistentAll(uname, epoint);
                }
            }

            pm.makePersistent(entry);
            tx.commit();
            ILogicalData copy = pm.detachCopy(entry);
            entry = null;
            entry = copy;

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    @Override
    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName) throws Exception {

        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        ILogicalData copy = null;
        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            String strLogicalResourceName = logicalResourceName.toString();
            q.setFilter("strLDRI == strLogicalResourceName");
            q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
            q.setUnique(true);
            ILogicalData loaded = (ILogicalData) q.execute(strLogicalResourceName);
            tx.commit();
            copy = pm.detachCopy(loaded);

        } catch (Exception ex) {
            throw new CatalogueException(ex.getMessage());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
        return copy;
    }

    @Override
    public void unregisterResourceEntry(ILogicalData entry) throws CatalogueException {
        //first remove this node from it's parent
        Path entriesParent = entry.getLDRI().getParent();
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            if (entriesParent != null && !StringUtil.isEmpty(entriesParent.toString()) && !entry.getLDRI().isRoot()) {
                String strLogicalResourceName = entriesParent.toString();

                Query q = pm.newQuery(LogicalData.class);

                //restrict to instances which have the field ldri equal to some logicalResourceName
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                q.setUnique(true);
                ILogicalData parentEntry = (ILogicalData) q.execute(strLogicalResourceName);
                if (parentEntry == null) {
                    throw new NonExistingResourceException("Cannot remove " + entry.getLDRI().toString() + " from non existing parent " + entriesParent.toString());
                }

                Path theChild = parentEntry.getChild(entry.getLDRI());
                if (theChild == null) {
                    throw new NonExistingResourceException("Cannot remove " + entry.getLDRI().toString() + ". Parent " + entriesParent.toString() + " has no such child");
                }
                parentEntry.removeChild(entry.getLDRI());
            }
            //Then remove it's children. Query for nodes that have that parent.. and the parent 
            Query q = pm.newQuery(LogicalData.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            String parentsName = entry.getLDRI().toString();

            q.setFilter("strLDRI == parentsName");
            q.declareParameters(parentsName.getClass().getName() + " parentsName");
            q.setUnique(true);
            LogicalData result = (LogicalData) q.execute(parentsName);
            Path path = result.getLDRI();
            if (path.isRoot()) {
                q = pm.newQuery(LogicalData.class);
                Long number = (Long) q.deletePersistentAll();
            } else {
                String name = "/" + path.getName();
                Integer pos = Integer.valueOf(path.toString().indexOf(name));
                q = pm.newQuery(LogicalData.class);
                q.setFilter("strLDRI.indexOf(name)==pos");
                q.declareParameters(name.getClass().getName() + " name, " + pos.getClass().getName() + " pos");
                Long number = (Long) q.deletePersistentAll(name, pos);
            }
            tx.commit();

        } catch (Exception ex) {
            throw new CatalogueException(ex.getMessage());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    @Override
    public Boolean resourceEntryExists(ILogicalData entry) throws CatalogueException {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        ILogicalData copy = null;
        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            String strLogicalResourceName = entry.getLDRI().toString();
            q.setFilter("strLDRI == strLogicalResourceName");
            q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
            q.setUnique(true);
            ILogicalData loaded = (ILogicalData) q.execute(strLogicalResourceName);
            tx.commit();
            copy = pm.detachCopy(loaded);
        } catch (Exception ex) {
            throw new CatalogueException(ex.getMessage());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
        return copy != null ? true : false;
    }

    @Override
    public Collection<ILogicalData> getTopLevelResourceEntries() throws CatalogueException {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<ILogicalData> copy = null;
        try {
            tx.begin();

            Query q = pm.newQuery("SELECT FROM " + LogicalData.class.getName()
                    + " WHERE ldriLen == 1");
            Collection<ILogicalData> results = (Collection<ILogicalData>) q.execute();
            tx.commit();
            copy = pm.detachCopyAll(results);

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }

        return copy;
    }

    @Override
    public void renameEntry(Path oldPath, Path newPath) throws CatalogueException {
        //Check if oldPath exists 
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        ILogicalData copy = null;
        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            String strLogicalResourceName = oldPath.toString();
            q.setFilter("strLDRI == strLogicalResourceName");
            q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
            q.setUnique(true);
            ILogicalData toBeRenamed = (ILogicalData) q.execute(strLogicalResourceName);

            if (toBeRenamed == null) {
                throw new ResourceExistsException("Rename Entry: cannot rename resource " + oldPath + " resource doesn't exists");
            }

            //Remove this node from it's parent 
            Path parent = oldPath.getParent();
            if (parent != null && !StringUtil.isEmpty(parent.toString())) {
                q = pm.newQuery(LogicalData.class);
                String parentsName = parent.toString();
                q.setFilter("strLDRI == parentsName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " parentsName");
                q.setUnique(true);

                ILogicalData parentEntry = (ILogicalData) q.execute(parentsName);

                if (parentEntry == null) {
                    throw new NonExistingResourceException("Cannot remove " + oldPath.toString() + " from non existing parent " + parent.toString());
                }

                Path theChild = parentEntry.getChild(oldPath);
                if (theChild == null) {
                    throw new NonExistingResourceException("Cannot remove " + oldPath.toString() + ". Parent " + parent.toString() + " has no such child");
                }
                parentEntry.removeChild(oldPath);
                parentEntry.addChild(newPath);
            }
            toBeRenamed.setLDRI(newPath);
            Collection<String> children = toBeRenamed.getChildren();
            if (children != null) {
                for (String ch : children) {
                    String newChildName = ch.replace(oldPath.toString(), newPath.toString());
                    toBeRenamed.removeChild(Path.path(ch));
                    toBeRenamed.addChild(Path.path(newChildName));

                    q = pm.newQuery(LogicalData.class);
                    q.setFilter("strLDRI == ch");
                    q.declareParameters(strLogicalResourceName.getClass().getName() + " ch");
                    q.setUnique(true);
                    ILogicalData childEntry = (ILogicalData) q.execute(ch);
                    childEntry.setLDRI(Path.path(newChildName));
//                    debug("Old Name: " + ch + " new name: " + newChildName);
                }
            }

            tx.commit();
        } catch (Exception ex) {
            throw new CatalogueException(ex.getMessage());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }

    }

    @Override
    public Collection<IStorageSite> getSitesByUname(String vphUname) throws CatalogueException {
        Collection<IStorageSite> copy = null;
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            Query q = pm.newQuery(StorageSite.class);

            q.setFilter("vphUsername == vphUname");
            q.declareParameters(vphUname.getClass().getName() + " vphUname");
            Collection<IStorageSite> results = (Collection<IStorageSite>) q.execute(vphUname);
            copy = pm.detachCopyAll(results);
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
        return copy;
    }

    @Override
    public boolean storageSiteExists(Properties prop) throws CatalogueException {
        String uname = prop.getProperty(nl.uva.cs.lobcder.webDav.resources.Constants.VPH_USERNAME);
        String ePoint = prop.getProperty(nl.uva.cs.lobcder.webDav.resources.Constants.STORAGE_SITE_ENDPOINT);
        Collection<StorageSite> ss;
        StorageSite storageSite = null;
        PersistenceManager pm = pmf.getPersistenceManagerProxy();
        Transaction tx = pm.currentTransaction();

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(StorageSite.class);

            q.setFilter("endpoint == ePoint && vphUsername == uname");
            q.declareParameters(ePoint.getClass().getName() + " ePoint, " + uname.getClass().getName() + " uname");
            q.setUnique(true);
            storageSite = (StorageSite) q.execute(ePoint, uname);
            tx.commit();
            StorageSite copy = pm.detachCopy(storageSite);
            storageSite = copy;
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }

        return storageSite != null ? true : false;
    }

    @Override
    public void registerStorageSite(Properties prop) throws CatalogueException {
        Credential cred = new Credential(prop.getProperty(nl.uva.cs.lobcder.webDav.resources.Constants.VPH_USERNAME));
        cred.setStorageSiteUsername(prop.getProperty(nl.uva.cs.lobcder.webDav.resources.Constants.STORAGE_SITE_USERNAME));
        cred.setStorageSitePassword(prop.getProperty(nl.uva.cs.lobcder.webDav.resources.Constants.STORAGE_SITE_PASSWORD));
        String endpoint = prop.getProperty(nl.uva.cs.lobcder.webDav.resources.Constants.STORAGE_SITE_ENDPOINT);

        PersistenceManager pm = null;
        Transaction tx = null;

        try {
            debug("Adding endpoint: " + endpoint);
            StorageSite site = new StorageSite(endpoint, cred);

            pm = pmf.getPersistenceManager();
            tx = pm.currentTransaction();

            tx.begin();

            Query q = pm.newQuery(StorageSite.class);
            String ePoint = site.getEndpoint();
            String uname = site.getVPHUsername();
            q.setFilter("endpoint == ePoint && vphUsername == uname");
            q.declareParameters(ePoint.getClass().getName() + " ePoint, " + uname.getClass().getName() + " uname");
            q.setUnique(true);
            StorageSite storageSite = (StorageSite) q.execute(ePoint, uname);
            if (storageSite == null) {
                pm.makePersistent(site);
//                StorageSite copy = pm.detachCopy(site);
//                site = copy;
            }
            tx.commit();
            q.cancelAll();

        } catch (Exception ex) {
            throw new CatalogueException(ex.getMessage());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    @Override
    public void updateResourceEntry(ILogicalData newResource) throws CatalogueException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws CatalogueException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void clearAllSites() {
        PersistenceManager pm = pmf.getPersistenceManagerProxy();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            Query q = pm.newQuery(StorageSite.class);

            long num = q.deletePersistentAll();
//                pm.deletePersistentAll(results);
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    Collection<StorageSite> getAllSites() {
        PersistenceManager pm = pmf.getPersistenceManagerProxy();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            Query q = pm.newQuery(StorageSite.class);
            Collection<StorageSite> results = (Collection<StorageSite>) q.execute();
            tx.commit();

            return pm.detachCopyAll(results);

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getName() + ": " + msg);
    }

    private boolean comparePaths(Path path1, Path path2) {
        if (path1.toString().equals(path2.toString())) {
            return true;
        }
        return false;
    }
}
