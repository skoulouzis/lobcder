/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.io.File;
import java.util.*;
import javax.jdo.*;
import javax.jdo.identity.StringIdentity;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.PropertiesLoader;
import nl.uva.vlet.data.StringUtil;

/**
 *
 * @author S. Koulouzis
 */
public class RDMSDLCatalog implements IDLCatalogue {

    private static final Object lock = new Object();
    private static PersistenceManagerFactory pmf;
    private final File propFile;
    private final Map<Path, ILogicalData> cahce = new HashMap<Path, ILogicalData>();

    public RDMSDLCatalog(File propFile) {
//        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
        this.propFile = propFile;
    }

    @Override
    public void registerResourceEntry(ILogicalData entry) throws CatalogueException {
        ILogicalData loaded = cahce.get(entry.getLDRI());
        if (loaded != null && comparePaths(loaded.getLDRI(), entry.getLDRI())) {
            throw new DuplicateResourceException("Cannot register resource " + entry.getLDRI() + " resource exists");
        }
        synchronized (lock) {
            //Check if it exists 
            String strLogicalResourceName = entry.getLDRI().toString();
            PersistenceManager pm = getPmf().getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            try {
                tx.begin();
                loaded = getEntryById(entry.getLDRI(), pm);
                Query q;
                if (loaded == null) {
                    //This query, will return objects of type DataResourceEntry
                    q = pm.newQuery(LogicalData.class);
                    q.setFilter("strLDRI == strLogicalResourceName");
                    q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                    q.setUnique(true);
                    loaded = (ILogicalData) q.execute(strLogicalResourceName);
                }
                if (loaded != null && comparePaths(loaded.getLDRI(), entry.getLDRI())) {
                    throw new DuplicateResourceException("Cannot register resource " + entry.getLDRI() + " resource exists");
                }

                //If it has a parent node, add this path to the parent node 
                Path parentPath = entry.getLDRI().getParent();
                if (parentPath != null && !StringUtil.isEmpty(parentPath.toString()) && !parentPath.isRoot()) {
                    strLogicalResourceName = parentPath.toString();
                    ILogicalData parentEntry = getEntryById(parentPath, pm);
                    if (parentEntry == null) {
                        q = pm.newQuery(LogicalData.class);
                        //restrict to instances which have the field ldri equal to some logicalResourceName
                        q.setFilter("strLDRI == strLogicalResourceName");
                        q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                        q.setUnique(true);
                        parentEntry = (ILogicalData) q.execute(strLogicalResourceName);
                    }
                    if (parentEntry == null) {
                        throw new NonExistingResourceException("Cannot add " + entry.getLDRI().toString() + " child to non existing parent " + parentPath.toString());
                    }
                    parentEntry.addChild(entry.getLDRI());
                    cahce.put(parentEntry.getLDRI(), parentEntry);
//                    pm.detachCopy(parentEntry);

                }
//                //Persisst entry
                pm.makePersistent(entry);
//                ILogicalData copy = pm.detachCopy(entry);
                tx.commit();
//                entry = null;
//                entry = copy;
                cahce.put(entry.getLDRI(), entry);
            } catch (Exception ex) {
                if (ex instanceof NonExistingResourceException) {
                    throw (NonExistingResourceException) ex;
                }
                throw new CatalogueException(ex.getMessage());
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }
    }

    @Override
    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName) throws Exception {
        synchronized (lock) {
            ILogicalData copy = cahce.get(logicalResourceName);
            if (copy != null) {
                return copy;
            }
            PersistenceManager pm = getPmf().getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            String strLogicalResourceName = logicalResourceName.toString();
            try {
                tx.begin();

                ILogicalData loaded = getEntryById(logicalResourceName, pm);
                copy = loaded;
                if (loaded == null) {
                    //This query, will return objects of type DataResourceEntry
                    Query q = pm.newQuery(LogicalData.class);
//                restrict to instances which have the field ldri equal to some logicalResourceName
                    q.setFilter("strLDRI == strLogicalResourceName");
                    q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                    q.setUnique(true);
                    loaded = (ILogicalData) q.execute(strLogicalResourceName);
                    copy = pm.detachCopy(loaded);
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
            if (copy != null) {
                cahce.put(copy.getLDRI(), copy);
            }
            return copy;
        }
    }

    @Override
    public void unregisterResourceEntry(ILogicalData entry) throws CatalogueException {
        synchronized (lock) {
            debug("Unregister " + entry.getLDRI());
            //first remove this node from it's parent
            Path entriesParent = entry.getLDRI().getParent();
            PersistenceManager pm = getPmf().getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            try {
                tx.begin();
                if (entriesParent != null && !StringUtil.isEmpty(entriesParent.toString()) && !entry.getLDRI().isRoot()) {
                    String strLogicalResourceName = entriesParent.toString();
                    ILogicalData parentEntry = getEntryById(entriesParent, pm);

                    if (parentEntry == null) {
                        Query q = pm.newQuery(LogicalData.class);
                        q.setFilter("strLDRI == strLogicalResourceName");
                        q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                        q.setUnique(true);
                        parentEntry = (ILogicalData) q.execute(strLogicalResourceName);
                    }
                    if (parentEntry == null) {
                        throw new NonExistingResourceException("Cannot remove " + entry.getLDRI().toString() + " from non existing parent " + entriesParent.toString());
                    }
                    Path theChild = parentEntry.getChild(entry.getLDRI());
                    if (theChild == null) {
                        throw new NonExistingResourceException("Cannot remove " + entry.getLDRI().toString() + ". Parent " + entriesParent.toString() + " has no such child");
                    }
                    parentEntry.removeChild(entry.getLDRI());
                    pm.detachCopy(parentEntry);
                }
                //Then remove it's children. Query for nodes that have that parent.. and the parent
                ILogicalData result = getEntryById(entry.getLDRI(), pm);
                Query q;
                String parentsName = entry.getLDRI().toString();
                if (result == null) {
                    q = pm.newQuery(LogicalData.class);
                    q.setFilter("strLDRI == parentsName");
                    q.declareParameters(parentsName.getClass().getName() + " parentsName");
                    q.setUnique(true);
                    result = (LogicalData) q.execute(parentsName);
                }
                ILogicalData copy = pm.detachCopy(result);
                //Maybe it's already gone 
                if (copy != null) {
                    //Delete its storage sites, since every time we create a new 
                    //entry we add a new StorageSite, even a copy
                    Collection<IStorageSite> storageSites = copy.getStorageSites();
                    //Make sure we dont clear all the storage sites. If the storage 
                    //site dosn't belong to any othe entry,  then  delete
                    //SELECT FOMRM LOGICALDATA WHERE storageSite.contains(storageSite)
                    Collection<LogicalData> res = null;
                    Collection<IStorageSite> toBeDeleted = new ArrayList<IStorageSite>();
                    Collection<IStorageSite> toBeUpdated = new ArrayList<IStorageSite>();

                    q = pm.newQuery(StorageSite.class);
                    Collection<StorageSite> sites = (Collection<StorageSite>) q.execute();

                    for (IStorageSite s : storageSites) {
                        q = pm.newQuery(LogicalData.class);
                        q.setFilter("storageSites.contains(s) && strLDRI != parentsName");
                        q.declareParameters(s.getClass().getName() + " s, " + parentsName.getClass().getName() + " parentsName");
                        res = (Collection<LogicalData>) q.execute(s, parentsName);
                        //Only if no one else is using it delete it
                        if (res == null || res.isEmpty() && sites.size() > PropertiesLoader.getNumOfStorageSites()) {
                            toBeDeleted.add(s);
                        } else {
                            s.removeLogicalPath(copy.getPDRI());
                            toBeUpdated.add(s);
                        }
                    }
                    
                    pm.deletePersistentAll(toBeDeleted);
                    pm.makePersistentAll(toBeUpdated);
//                    pm.detachCopyAll(toBeUpdated);
                } else {
                    String path = entry.getLDRI().toString();
                    debug("Entry " + entry.getLDRI() + " is not in catalog");
                    //We need to remove this ldri from the storage site that has it cached 
                    q = pm.newQuery(StorageSite.class);
                    q.setFilter("logicalPaths.contains(path)");
                    q.declareParameters(path.getClass().getName() + " path");
                    Collection<IStorageSite> results = (Collection<IStorageSite>) q.execute(path);
                    for (IStorageSite s : results) {
                        s.removeLogicalPath(entry.getLDRI());
                    }
                }
                Path path = entry.getLDRI();
                if (path.isRoot()) {
                    q = pm.newQuery(LogicalData.class);
                    Long number = (Long) q.deletePersistentAll();
                    cahce.clear();
                } else {
                    String name = "/" + path.getName();
                    String nameWithSlash = name + "/";
                    String strPath = path.toString();
                    Integer start = Integer.valueOf(path.toString().indexOf(name));
                    Integer end = Integer.valueOf(path.toString().indexOf(name)) + name.length() + 1;
                    q = pm.newQuery(LogicalData.class);
//                    
                    q.setFilter("strLDRI.indexOf(name)==start && strLDRI.substring(start,end)==nameWithSlash || strLDRI==strPath");
                    q.declareParameters(name.getClass().getName() + " name, "
                            + start.getClass().getName() + " start, "
                            + end.getClass().getName() + " end, "
                            + nameWithSlash.getClass().getName() + " nameWithSlash, "
                            + strPath.getClass().getName() + " strPath");
//                    Long number = q.deletePersistentAll(name, start, end, nameWithSlash, strPath);
                    Collection<ILogicalData> res = (Collection<ILogicalData>) q.executeWithArray(new Object[]{name, start, end, nameWithSlash, strPath});
                    for (ILogicalData ld : res) {
                        cahce.remove(ld.getLDRI());
                    }
                    pm.deletePersistentAll(res);
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
    }

    @Override
    public Boolean resourceEntryExists(ILogicalData entry) throws CatalogueException {
        synchronized (lock) {
            ILogicalData copy = cahce.get(entry.getLDRI());
            if (copy != null) {
                return true;
            }
            PersistenceManager pm = getPmf().getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);

            try {
                tx.begin();
                ILogicalData loaded = getEntryById(entry.getLDRI(), pm);
                if (loaded == null) {
                    //This query, will return objects of type DataResourceEntry
                    Query q = pm.newQuery(LogicalData.class);
                    //restrict to instances which have the field ldri equal to some logicalResourceName
                    String strLogicalResourceName = entry.getLDRI().toString();
                    q.setFilter("strLDRI == strLogicalResourceName");
                    q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                    q.setUnique(true);
                    loaded = (ILogicalData) q.execute(strLogicalResourceName);
                }

                copy = pm.detachCopy(loaded);
                tx.commit();
            } catch (Exception ex) {
                throw new CatalogueException(ex.getMessage());
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
            if (copy == null) {
                cahce.remove(entry.getLDRI());
            }
            return copy != null ? true : false;
        }
    }

    @Override
    public Collection<ILogicalData> getTopLevelResourceEntries() throws CatalogueException {
        synchronized (lock) {
            PersistenceManager pm = getPmf().getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            Collection<ILogicalData> copy = null;
            try {
                tx.begin();

                Query q = pm.newQuery("SELECT FROM " + LogicalData.class.getName()
                        + " WHERE ldriLen == 1");
                Collection<ILogicalData> results = (Collection<ILogicalData>) q.execute();
                copy = pm.detachCopyAll(results);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }

                pm.close();
            }
            for (ILogicalData ld : copy) {
                cahce.put(ld.getLDRI(), ld);
            }
            return copy;
        }
    }

    @Override
    public void renameEntry(Path oldPath, Path newPath) throws CatalogueException {
        synchronized (lock) {
            //Check if oldPath exists 
            PersistenceManager pm = getPmf().getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            try {
                tx.begin();
                ILogicalData toBeRenamed = getEntryById(oldPath, pm);
                Query q;
                String strLogicalResourceName = oldPath.toString();
                if (toBeRenamed == null) {
                    //This query, will return objects of type DataResourceEntry
                    q = pm.newQuery(LogicalData.class);
                    //restrict to instances which have the field ldri equal to some logicalResourceName
                    q.setFilter("strLDRI == strLogicalResourceName");
                    q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                    q.setUnique(true);
                    toBeRenamed = (ILogicalData) q.execute(strLogicalResourceName);
                }
                if (toBeRenamed == null) {
                    throw new ResourceExistsException("Rename Entry: cannot rename resource " + oldPath + " resource doesn't exists");
                }
                //Remove this node from it's parent 
                Path parent = oldPath.getParent();
                if (parent != null && !StringUtil.isEmpty(parent.toString())) {
                    ILogicalData parentEntry = getEntryById(parent, pm);
                    if (parentEntry == null) {
                        q = pm.newQuery(LogicalData.class);
                        String parentsName = parent.toString();
                        q.setFilter("strLDRI == parentsName");
                        q.declareParameters(strLogicalResourceName.getClass().getName() + " parentsName");
                        q.setUnique(true);
                        parentEntry = (ILogicalData) q.execute(parentsName);
                    }
                    if (parentEntry == null) {
                        throw new NonExistingResourceException("Cannot remove " + oldPath.toString() + " from non existing parent " + parent.toString());
                    }
                    Path theChild = parentEntry.getChild(oldPath);
                    if (theChild == null) {
                        throw new NonExistingResourceException("Cannot remove " + oldPath.toString() + ". Parent " + parent.toString() + " has no such child");
                    }
//                    pm.detachCopy(parentEntry);
                    parentEntry.removeChild(oldPath);
                    parentEntry.addChild(newPath);
                    cahce.put(parentEntry.getLDRI(), parentEntry);
                }
                cahce.remove(toBeRenamed.getLDRI());
                toBeRenamed.setLDRI(newPath);

                Collection<String> children = toBeRenamed.getChildren();
                if (children != null) {
                    for (String ch : children) {
                        String newChildName = ch.replace(oldPath.toString(), newPath.toString());
                        toBeRenamed.removeChild(Path.path(ch));
                        toBeRenamed.addChild(Path.path(newChildName));

                        ILogicalData childEntry = getEntryById(Path.path(ch), pm);
                        if (childEntry == null) {
                            q = pm.newQuery(LogicalData.class);
                            q.setFilter("strLDRI == ch");
                            q.declareParameters(strLogicalResourceName.getClass().getName() + " ch");
                            q.setUnique(true);
                            childEntry = (ILogicalData) q.execute(ch);
                        }
                        cahce.remove(childEntry.getLDRI());
                        childEntry.setLDRI(Path.path(newChildName));
                        cahce.put(childEntry.getLDRI(), childEntry);
//                        pm.detachCopy(childEntry);
//                    debug("Old Name: " + ch + " new name: " + newChildName);
                    }
                }
                pm.makePersistent(toBeRenamed);
                Path ldri = toBeRenamed.getLDRI();
                cahce.put(ldri, toBeRenamed);
                //Add this to new Parent
                Path newParent = newPath.getParent();
                if (newParent != null && !StringUtil.isEmpty(newParent.toString())) {
                    String path = newParent.toString();
                    ILogicalData newParentEntry = getEntryById(newParent, pm);
                    if (newParentEntry == null) {
                        q = pm.newQuery(LogicalData.class);
                        q.setFilter("strLDRI == path");
                        q.declareParameters(path.getClass().getName() + " path");
                        q.setUnique(true);
                        newParentEntry = (ILogicalData) q.execute(path);
                    }
                    newParentEntry.addChild(newPath);
                    cahce.put(newParentEntry.getLDRI(), newParentEntry);
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
    }

    @Override
    public Collection<IStorageSite> getSitesByUname(String vphUname) throws CatalogueException {
        Collection<IStorageSite> copy = null;
        synchronized (lock) {
            PersistenceManager pm = getPmf().getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            try {
                tx.begin();
                Query q = pm.newQuery(StorageSite.class);

                q.setFilter("vphUsernames.contains(vphUname)");
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
    }

    @Override
    public boolean storageSiteExists(Properties prop) throws CatalogueException {
        String unameCSV = prop.getProperty(nl.uva.cs.lobcder.util.Constants.VPH_USERNAMES);
        String ePoint = prop.getProperty(nl.uva.cs.lobcder.util.Constants.STORAGE_SITE_ENDPOINT);
        Collection<StorageSite> copy;
        synchronized (lock) {
            PersistenceManager pm = getPmf().getPersistenceManagerProxy();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(StorageSite.class);

                q.setFilter("endpoint == ePoint && vphUsernamesCSV == unameCSV");
                q.declareParameters(ePoint.getClass().getName() + " ePoint, " + unameCSV.getClass().getName() + " unameCSV");
                //                q.setUnique(true);
                Collection<StorageSite> StorageSites = (Collection<StorageSite>) q.execute(ePoint, unameCSV);
                copy = pm.detachCopyAll(StorageSites);
                tx.commit();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }

                pm.close();
            }
            if (copy == null || copy.isEmpty()) {
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public void registerStorageSite(Properties prop) throws CatalogueException {
        String[] vphUsers = prop.getProperty(nl.uva.cs.lobcder.util.Constants.VPH_USERNAMES).split(",");
        Credential cred = new Credential(vphUsers);
        cred.setStorageSiteUsername(prop.getProperty(nl.uva.cs.lobcder.util.Constants.STORAGE_SITE_USERNAME));
        cred.setStorageSitePassword(prop.getProperty(nl.uva.cs.lobcder.util.Constants.STORAGE_SITE_PASSWORD));
        String endpoint = prop.getProperty(nl.uva.cs.lobcder.util.Constants.STORAGE_SITE_ENDPOINT);

        PersistenceManager pm = null;
        Transaction tx = null;
        synchronized (lock) {

            try {
                debug("Adding endpoint: " + endpoint);
                StorageSite site = new StorageSite(endpoint, cred);

                pm = getPmf().getPersistenceManager();
                tx = pm.currentTransaction();
                tx.setSerializeRead(Boolean.TRUE);
                tx.begin();

                Query q = pm.newQuery(StorageSite.class);
                String ePoint = site.getEndpoint();
                Collection<String> unameCSV = site.getVPHUsernames();

                q.setFilter(
                        "endpoint == ePoint && vphUsernamesCSV == unameCSV");
                q.declareParameters(ePoint.getClass().getName() + " ePoint, " + unameCSV.getClass().getName() + " unameCSV");
                q.setUnique(
                        true);
                StorageSite storageSite = (StorageSite) q.execute(ePoint, unameCSV);

                pm.detachCopy(storageSite);
                if (storageSite
                        == null) {
                    pm.makePersistent(site);
                    StorageSite copy = pm.detachCopy(site);
                    site = copy;
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
    }

    @Override
    public void updateResourceEntry(ILogicalData newResource) throws CatalogueException {

        Transaction tx = null;
        PersistenceManager pm = null;
        synchronized (lock) {
            try {
                pm = getPmf().getPersistenceManager();
                tx = pm.currentTransaction();
                tx.setSerializeRead(Boolean.TRUE);
                tx.begin();
                //Batch updates
//            Query query = pm.newQuery("UPDATE " + newResource.getClass().getName() + "SET this.ldri=newLDRI WHERE strLDRI == strLogicalResourceName");
//            Long number = (Long) query.execute();
                pm.makePersistent(newResource);
                ILogicalData copy = pm.detachCopy(newResource);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
            cahce.put(newResource.getLDRI(), newResource);
        }
    }

    @Override
    public void close() throws CatalogueException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void clearAllSites() {
        synchronized (lock) {
            PersistenceManager pm = getPmf().getPersistenceManagerProxy();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
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
    }

    public Collection<StorageSite> getAllSites() {
        synchronized (lock) {
            PersistenceManager pm = getPmf().getPersistenceManagerProxy();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
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

    /**
     * @return the pmf
     */
    private PersistenceManagerFactory getPmf() {
        if (pmf == null) {
            pmf = JDOHelper.getPersistenceManagerFactory(propFile);//Path.path(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties").toString());
        }
        return pmf;
    }

    void clearLogicalData() {
        synchronized (lock) {
            PersistenceManager pm = getPmf().getPersistenceManagerProxy();
            Transaction tx = pm.currentTransaction();
            tx.setSerializeRead(Boolean.TRUE);
            try {
                tx.begin();
                Query q = pm.newQuery(LogicalData.class);

                long num = q.deletePersistentAll();
//                pm.deletePersistentAll(results);

                tx.commit();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
            cahce.clear();
        }
    }

    private ILogicalData getEntryById(Path logicalResourceName, PersistenceManager pm) {

        StringIdentity id = new StringIdentity(LogicalData.class, logicalResourceName.toString());
        ILogicalData loaded;
        try {
            loaded = (ILogicalData) pm.getObjectById(id);
            if (!loaded.getLDRI().toString().equals(logicalResourceName.toString())) {
                loaded = null;
            }
        } catch (JDOObjectNotFoundException ex) {
            loaded = null;
        }
        return loaded;
    }
}
