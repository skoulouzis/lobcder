/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

/**
 *
 * @author S. Koulouzis
 */
import com.bradmcevoy.common.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.resources.LogicalFolder;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.webDav.resources.Constants;
import nl.uva.vlet.data.StringUtil;

public class SimpleDLCatalogue implements IDLCatalogue {

    private static boolean debug = true;
    private final PersistenceManagerFactory pmf;
    private static final Object lock = new Object();

    public SimpleDLCatalogue() {
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
    }

    @Override
    public void registerResourceEntry(ILogicalData entry) throws CatalogueException {
        ILogicalData loaded = queryEntry(entry.getLDRI());
        if (loaded != null && comparePaths(loaded.getLDRI(), entry.getLDRI())) {
            throw new DuplicateResourceException("Cannot register resource " + entry.getLDRI() + " resource exists");
        }

        Path parentPath = entry.getLDRI().getParent();
        if (parentPath != null && !StringUtil.isEmpty(parentPath.toString()) && !parentPath.isRoot()) {
            addChild(parentPath, entry.getLDRI());
        }
        persistEntry(entry);
    }

    @Override
    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName) throws CatalogueException {
        debug("Quering " + logicalResourceName);
        return queryEntry(logicalResourceName);
    }

//    @Override
//    public IResourceEntry getResourceEntryByUID(String uid) throws Exception {
//        return loadEntryByUID(uid);
//    }
    @Override
    public void unregisterResourceEntry(ILogicalData entry) throws CatalogueException {
        //        Collection<StorageSite> sites = entry.getStorageSites();
        //        if (sites != null && sites.isEmpty()) {
        //            StorageSiteManager sm = new StorageSiteManager();
        //            sm.deleteStorgaeSites(entry.getStorageSites());
        //        }
        deleteEntry(entry.getLDRI());
    }

    @Override
    public Boolean resourceEntryExists(ILogicalData entry) throws CatalogueException {
        return queryEntry(entry.getLDRI()) != null ? true : false;
    }

    @Override
    public Collection<ILogicalData> getTopLevelResourceEntries() throws CatalogueException {
        return queryTopLevelResources();
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
        }
    }

    private void persistEntry(ILogicalData entry) {
        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            Collection<IStorageSite> storageSites = entry.getStorageSites();
            Collection<StorageSite> results;
            Collection<StorageSite> deleteStorageSites = new ArrayList<StorageSite>();
            String epoint;
            String uname;
            try {
                tx.begin();
//            debug("persistEntry. DB UID class: " + id.getClass().getName() + " UID: " + id);
                //work around to remove duplicated storage sites 
                if (storageSites != null && !storageSites.isEmpty()) {
                    for (IStorageSite s : storageSites) {
                        uname = s.getVPHUsername();
                        epoint = s.getEndpoint();
                        Query q = pm.newQuery(StorageSite.class);

                        q.setFilter("vphUsername == uname && endpoint == epoint");
                        q.declareParameters(uname.getClass().getName() + " uname, " + epoint.getClass().getName() + " epoint");


                        results = (Collection<StorageSite>) q.execute(uname, epoint);
                        for (StorageSite ss : results) {
                            if (s.getVPHUsername().equals(ss.getVPHUsername()) && s.getEndpoint().equals(ss.getEndpoint())) {
                                deleteStorageSites.add(ss);
                            }
                        }
                    }
                    pm.deletePersistentAll(deleteStorageSites);
                }
                pm.makePersistent(entry);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                //!?!?!?! if this is not here, the entry's LDRI gets to null???
                entry.getLDRI();
//                entry.getChildren();
//                entry.getStorageSites();
//                entry.getUID();
                pm.close();
            }
        }
    }

    private ILogicalData queryEntry(Path logicalResourceName) throws CatalogueException {
        String strLogicalResourceName = logicalResourceName.toString();
        ILogicalData entry;
        synchronized (lock) {
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
                entry = (ILogicalData) q.execute(strLogicalResourceName);
                if (entry != null) {
                    debug("Got back: " + entry.getLDRI());
                }
                if (entry != null && entry.getLDRI() == null) {
                    throw new CatalogueException("entry " + logicalResourceName + " has null LDRI");
                }
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }

        if (entry != null) {
            debug("Got back: " + entry.getLDRI());
        }

        return entry;
    }

    private void deleteEntry(Path logicalResourceName) throws ResourceExistsException, NonExistingResourceException {
        debug("deleteEntry: " + logicalResourceName);
        String strLogicalResourceName = logicalResourceName.toString();

        //first remove this node from it's parent
        Path parent = logicalResourceName.getParent();

        if (parent != null && !StringUtil.isEmpty(parent.toString()) && !logicalResourceName.isRoot()) {
            removeChild(parent, logicalResourceName);
        }

        synchronized (lock) {
            //Next the node 
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();

                Query q = pm.newQuery(LogicalData.class);
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");

                q.setUnique(true);
                LogicalData result = (LogicalData) q.execute(strLogicalResourceName);
                pm.deletePersistent(result);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }

    }

    private void addChild(Path parent, Path child) throws NonExistingResourceException {
        debug("Will add to " + parent + " " + child);
        String strLogicalResourceName = parent.toString();

        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            ILogicalData entry = null;

            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(LogicalData.class);
                //restrict to instances which have the field ldri equal to some logicalResourceName
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                q.setUnique(true);
                entry = (ILogicalData) q.execute(strLogicalResourceName);
                if (entry == null) {
                    throw new NonExistingResourceException("Cannot add " + child.toString() + " child to non existing parent " + parent.toString());
                }
                entry.addChild(child);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }

    }

    private void removeChild(Path parent, Path child) throws NonExistingResourceException {
        debug("Will remove from " + parent + " " + child);
        String strLogicalResourceName = parent.toString();

        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();

            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(LogicalData.class);
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");

                q.setUnique(true);
                LogicalData result = (LogicalData) q.execute(strLogicalResourceName);
                if (result == null) {
                    throw new NonExistingResourceException("Cannot remove " + child.toString() + " from non existing parent " + parent.toString());
                }

                result.removeChild(child);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }

    }

    private Collection<ILogicalData> queryTopLevelResources() {
        Collection topLevel = new ArrayList<ILogicalData>();
        synchronized (lock) {
            //TODO Fix all the queris!
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            Collection<ILogicalData> results;
            Path p = Path.path("/");
            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(LogicalData.class);

                //restrict to instances which have the field ldri equal to some logicalResourceName
                q.setFilter("ldri.getLength == p.getLength");
                //We then import the type of our logicalResourceName parameter
                q.declareImports("import " + p.getClass().getName());
                //and the parameter itself
                q.declareParameters("Path p");
                results = (Collection<ILogicalData>) q.execute(p);

                for (ILogicalData e : results) {
                    if (e.getLDRI().getLength() == 1) {
                        topLevel.add(e);
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

        return topLevel;
    }

    @Override
    public void renameEntry(Path oldPath, Path newPath) throws CatalogueException {
        debug("renameEntry.");
        debug("\t entry: " + oldPath + " newName: " + newPath);
        String strLogicalResourceName = oldPath.toPath();

        ILogicalData loaded = queryEntry(oldPath);

        if (loaded == null) {
            throw new ResourceExistsException("Rename Entry: cannot rename resource " + oldPath + " resource doesn't exists");
        }

        Path parent = oldPath.getParent();
        if (parent != null && !StringUtil.isEmpty(parent.toString())) {
            removeChild(parent, oldPath);
        }

        Map<Path, Path> renamedChildrenMap = new HashMap<Path, Path>();
        PersistenceManager pm;
        Transaction tx;
        synchronized (lock) {
            pm = pmf.getPersistenceManager();
            tx = pm.currentTransaction();
            String[] parts;
            String addition;
            String strNewChildPath = "";
            Path newChildPath;


            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(LogicalData.class);

                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");

                q.setUnique(true);
                ILogicalData entry = (ILogicalData) q.execute(strLogicalResourceName);
                entry.setLDRI(newPath);
                Collection<Path> children = entry.getChildren();
                if (children != null && !children.isEmpty()) {
                    for (Path p : children) {
//                debug("Path: " + p);
                        parts = p.getParts();

                        for (String part : parts) {
//                    debug("Part: " + part);
                            if (part.equals(oldPath.getName())) {
                                addition = newPath.getName();
                            } else {
                                addition = part;
                            }
                            strNewChildPath += "/" + addition;

                        }
                        newChildPath = Path.path(newPath, strNewChildPath);
//                debug("newChildPath: " + newChildPath);
                        entry.removeChild(p);
                        entry.addChild(newChildPath);
                        renamedChildrenMap.put(p, newChildPath);
                        strNewChildPath = "";
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


        if (renamedChildrenMap != null && !renamedChildrenMap.isEmpty()) {
            Set<Path> keySet = renamedChildrenMap.keySet();
            for (Path p : keySet) {
                strLogicalResourceName = p.toString();
                synchronized (lock) {
                    pm = pmf.getPersistenceManager();
                    tx = pm.currentTransaction();

                    try {
                        tx.begin();
                        //This query, will return objects of type DataResourceEntry
                        Query q = pm.newQuery(LogicalData.class);
                        q.setFilter("strLDRI == strLogicalResourceName");
                        q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");

                        q.setUnique(true);
                        ILogicalData childEntry = (ILogicalData) q.execute(strLogicalResourceName);
                        if (childEntry == null) {
//                        pm.makePersistent(new LogicalData(renamedChildrenMap.get(p)));
                        } else {
                            childEntry.setLDRI(renamedChildrenMap.get(p));
                        }
                        tx.commit();
                    } finally {
                        if (tx.isActive()) {
                            tx.rollback();
                        }
                        pm.close();
                    }
                }

            }
        }

        Path newParent = newPath.getParent();
        if (newParent != null && !StringUtil.isEmpty(newParent.toString())) {
            addChild(newPath.getParent(), newPath);
        }
    }

    private boolean comparePaths(Path path1, Path path2) {
        if (path1.toString().equals(path2.toString())) {
            return true;
        }
        return false;
    }

    public Collection<LogicalData> getAllLogicalData() {
        Collection c;
        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();
                Query q = pm.newQuery(LogicalData.class);
                c = (Collection) q.execute();
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }
        return c;
    }

    @Override
    public Collection<IStorageSite> getSitesByUname(String vphUname) {
        Collection<IStorageSite> results;
        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManagerProxy();
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();
                Query q = pm.newQuery(StorageSite.class);

                q.setFilter("vphUsername == vphUname");
                q.declareParameters(vphUname.getClass().getName() + " vphUname");

                results = (Collection<IStorageSite>) q.execute(vphUname);

                if (!results.isEmpty()) {
                    for (IStorageSite s : results) {
                        debug("getSites. endpoint: " + s.getEndpoint() + " uname: " + s.getVPHUsername());
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
        return results;
    }

    public void clearAllSites() {
        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManagerProxy();
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
    }

    public Collection<StorageSite> getAllSites() {
        synchronized (lock) {
            //Next the node 
            PersistenceManager pm = pmf.getPersistenceManagerProxy();
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();

                Query q = pm.newQuery(StorageSite.class);
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
    }

    @Override
    public void registerStorageSite(Properties prop) throws CatalogueException {
        Credential cred = new Credential(prop.getProperty(Constants.VPH_USERNAME));
        cred.setStorageSiteUsername(prop.getProperty(Constants.STORAGE_SITE_USERNAME));
        cred.setStorageSitePassword(prop.getProperty(Constants.STORAGE_SITE_PASSWORD));
        String endpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);

        if (!storageSiteExists(prop)) {
            try {
                debug("Adding endpoint: " + endpoint);

                StorageSite site = new StorageSite(endpoint, cred);
                synchronized (lock) {
                    PersistenceManager pm = pmf.getPersistenceManagerProxy();
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
            } catch (Exception ex) {
                throw new CatalogueException(ex.getMessage());
            }
        }

    }

    @Override
    public boolean storageSiteExists(Properties prop) throws CatalogueException {
        String uname = prop.getProperty(Constants.VPH_USERNAME);
        String ssUname = Constants.STORAGE_SITE_USERNAME;
        String ePoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);


        Collection<StorageSite> ss;
        StorageSite storageSite = null;
        Object r;
        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManagerProxy();
            Transaction tx = pm.currentTransaction();
            int hit = 0;

            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(StorageSite.class);

                q.setFilter("endpoint == ePoint && vphUsername == uname");
                q.declareParameters(ePoint.getClass().getName() + " ePoint, " + uname.getClass().getName() + " uname");
                ss = (Collection<StorageSite>) q.execute(ePoint, uname);
                for (StorageSite s : ss) {
                    if (s.getEndpoint().equals(ePoint) && s.getVPHUsername().equals(uname)) {
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
        }
        if (storageSite == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void updateResourceEntry(ILogicalData entry) throws CatalogueException {
        Collection<Path> children = entry.getChildren();
        Path ldri = entry.getLDRI();
        Metadata meta = entry.getMetadata();
        Collection<IStorageSite> sites = entry.getStorageSites();

        ILogicalData updated;
        try {
            if (entry instanceof LogicalFile) {
                updated = new LogicalFile(ldri);
            } else if (entry instanceof LogicalFolder) {
                updated = new LogicalFolder(ldri);
            } else {
                updated = new LogicalData(ldri);
            }
            updated.setChildren(children);
            updated.setMetadata(meta);
            Credential cred;
            if (sites != null && !sites.isEmpty()) {
                Collection<IStorageSite> copySites = new ArrayList<IStorageSite>();
                for (IStorageSite s : sites) {
                    cred = new Credential(s.getVPHUsername());
                    cred.setStorageSitePassword(s.getCredentials().getStorageSitePassword());
                    cred.setStorageSiteUsername(s.getCredentials().getStorageSiteUsername());
                    copySites.add(new StorageSite(s.getEndpoint(), cred));
                }
                updated.setStorageSites(copySites);
            }
        } catch (Exception ex) {
            throw new CatalogueException(ex.getMessage());
        }

        unregisterResourceEntry(entry);
        registerResourceEntry(updated);
    }

    @Override
    public void close() throws CatalogueException {
        this.pmf.getPersistenceManager().close();
    }
}
