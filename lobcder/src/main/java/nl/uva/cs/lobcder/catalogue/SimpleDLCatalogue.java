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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.*;
import javax.jdo.annotations.Transactional;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.vlet.data.StringUtil;
import org.datanucleus.api.jdo.NucleusJDOHelper;

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
            try {
                tx.begin();
//            debug("persistEntry. DB UID class: " + id.getClass().getName() + " UID: " + id);
                //work around to remove duplicated storage sites 
                if (storageSites != null && !storageSites.isEmpty()) {
                    for (IStorageSite s : storageSites) {
                        Collection<String> uname = s.getVPHUsernames();
                        epoint = s.getEndpoint();
                        Query q = pm.newQuery(StorageSite.class);

                        q.setFilter("vphUsernames == uname && endpoint == epoint");
                        q.declareParameters(uname.getClass().getName() + " uname, " + epoint.getClass().getName() + " epoint");


                        results = (Collection<StorageSite>) q.execute(uname, epoint);
                        for (StorageSite ss : results) {
                            if (s.getVPHUsernames().equals(ss.getVPHUsernames()) && s.getEndpoint().equals(ss.getEndpoint())) {
                                deleteStorageSites.add(ss);
                            }
                        }
                    }
                    pm.deletePersistentAll(deleteStorageSites);
                }
                pm.makePersistent(entry);
                //The null bug
                pm.detachCopy(entry);
                tx.commit();

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                stupidBugLogicData(entry);
                pm.close();
            }
        }
    }

    private ILogicalData queryEntry(Path logicalResourceName) throws CatalogueException {
        String strLogicalResourceName = logicalResourceName.toString();
        ILogicalData copy = null;
        synchronized (lock) {
            PersistenceManager pm = pmf.getPersistenceManager();
            FetchPlan fp = pm.getFetchPlan();
            fp.setGroup(FetchPlan.DEFAULT);
            fp.setMaxFetchDepth(-1);
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(LogicalData.class);

                //restrict to instances which have the field ldri equal to some logicalResourceName
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                q.setUnique(true);
               
                ILogicalData entry = (ILogicalData) q.execute(strLogicalResourceName);
                copy = entry;
                tx.commit();

                String[] loadedFieldNames = NucleusJDOHelper.getLoadedFields(entry, pm);
                if (loadedFieldNames != null) {
                    for (String l : loadedFieldNames) {
                        debug(entry.getLDRI() + " Loaded: " + l);
                    }
                }

                if (entry != null) {
                    boolean isDetached = JDOHelper.isDetached(entry);
                    debug(entry.getLDRI() + " is detaced: " + isDetached);
                    if (!isDetached) {
                        pm.makeTransient(entry);
//                        pm.detachCopy(entry);
                        isDetached = JDOHelper.isDetached(entry);
                        debug(entry.getLDRI() + " is detaced: " + isDetached);
                    }

                    ObjectState state = JDOHelper.getObjectState(entry);
                    debug("State: " + state.name());
                }
//                copy = pm.detachCopy(entry);
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();

            }
        }

        return copy;
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

                Collection<String> childernPathForDeleition = result.getChildren();

                if (childernPathForDeleition != null) {
                    Collection<ILogicalData> childernForDeleition = new ArrayList<ILogicalData>();
                    for (String p : childernPathForDeleition) {
                        strLogicalResourceName = p.toString();
                        q = pm.newQuery(LogicalData.class);
                        q.setFilter("strLDRI == strLogicalResourceName");
                        q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                        q.setUnique(true);

                        LogicalData ch = (LogicalData) q.execute(strLogicalResourceName);
                        childernForDeleition.add(ch);
                    }
                    pm.deletePersistentAll(childernForDeleition);
                }

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

            try {
                tx.begin();
                //This query, will return objects of type DataResourceEntry
                Query q = pm.newQuery(LogicalData.class);
                //restrict to instances which have the field ldri equal to some logicalResourceName
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                q.setUnique(true);

                ILogicalData entry = (ILogicalData) q.execute(strLogicalResourceName);
                ILogicalData copy = pm.detachCopy(entry);
                if (entry == null) {
                    throw new NonExistingResourceException("Cannot add " + child.toString() + " child to non existing parent " + parent.toString());
                }
                copy.addChild(child);
                entry.addChild(child);
                tx.commit();
                stupidBugLogicData(entry);

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
//                Query q = pm.newQuery(extend);

                //restrict to instances which have the field ldri equal to some logicalResourceName
                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");
                q.setUnique(true);

                ILogicalData result = (ILogicalData) q.execute(strLogicalResourceName);
                //Lazy loading ?
                result.getChildren().size();
                ILogicalData copy = pm.detachCopy(result);

                if (copy == null) {
                    throw new NonExistingResourceException("Cannot remove " + child.toString() + " from non existing parent " + parent.toString());
                }

                Path theChild = copy.getChild(child);
                if (theChild == null) {
                    throw new NonExistingResourceException("Cannot remove " + child.toString() + ". Parent " + parent.toString() + " has no such child");
                }
                copy.removeChild(child);
                result.removeChild(child);

                tx.commit();

                stupidBugLogicData(result);



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
        Collection<ILogicalData> results = null;
        synchronized (lock) {
            //TODO Fix all the queris!
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();

                Query q = pm.newQuery("SELECT FROM " + LogicalData.class.getName()
                        + " WHERE ldriLen == 1");

                results = (Collection<ILogicalData>) q.execute();

                tx.commit();

                if (results != null) {
                    //The stupid bug 
                    for (ILogicalData e : results) {
                        stupidBugLogicData(e);
                    }
                }

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }

                pm.close();
            }
        }

//        return topLevel;
        return results;
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

        //Remove this node from it's parent 
        Path parent = oldPath.getParent();
        if (parent != null && !StringUtil.isEmpty(parent.toString())) {
//            ILogicalData p = queryEntry(parent);
            removeChild(parent, oldPath);
        }

        Map<Path, Path> renamedChildrenMap = new HashMap<Path, Path>();
        PersistenceManager pm;
        Transaction tx;
        ILogicalData entry;
        synchronized (lock) {
            pm = pmf.getPersistenceManager();
            tx = pm.currentTransaction();
            String[] parts;
            String addition;
            String strNewChildPath = "";
            Path newChildPath;

            //Rename the node and hold it's children aside 
            try {
                tx.begin();
                //This query, will return objects of type LogicalData
                Query q = pm.newQuery(LogicalData.class);

                q.setFilter("strLDRI == strLogicalResourceName");
                q.declareParameters(strLogicalResourceName.getClass().getName() + " strLogicalResourceName");

                q.setUnique(true);

                entry = (ILogicalData) q.execute(strLogicalResourceName);
                entry.setLDRI(newPath);
                Collection<String> children = entry.getChildren();
                if (children != null && !children.isEmpty()) {
                    for (String p : children) {
                        debug("Path: " + p);
                        parts = Path.path(p).getParts();

                        for (String part : parts) {
                            debug("Part: " + part);
                            if (part.equals(oldPath.getName())) {
                                addition = newPath.getName();
                            } else {
                                addition = part;
                            }
                            strNewChildPath += "/" + addition;
                        }
                        newChildPath = Path.path(newPath, strNewChildPath);
                        debug("newChildPath: " + newChildPath);
                        entry.removeChild(Path.path(p));
                        entry.addChild(newChildPath);
                        renamedChildrenMap.put(Path.path(p), newChildPath);
                        strNewChildPath = "";
                    }
                }
                tx.commit();
                stupidBugLogicData(entry);
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }

        //Rename children of type LogicalData. The children of the prev step are just of type Path
        if (renamedChildrenMap != null && !renamedChildrenMap.isEmpty()) {
            Set<Path> keySet = renamedChildrenMap.keySet();
            //Query the old children names 
            for (Path p : keySet) {
                strLogicalResourceName = p.toString();
                synchronized (lock) {
                    pm = pmf.getPersistenceManager();
                    tx = pm.currentTransaction();

                    try {
                        tx.begin();

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

        //Add the renamed entry to it's new parent 
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
        Collection<IStorageSite> results = null;
        Collection<IStorageSite> copy = null;
        synchronized (lock) {
//            PersistenceManager pm = pmf.getPersistenceManagerProxy();
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            try {
                tx.begin();
                Query q = pm.newQuery(StorageSite.class);

                q.setFilter("vphUsernames == vphUname");
                q.declareParameters(vphUname.getClass().getName() + " vphUname");
                
                results = (AbstractCollection<IStorageSite>) q.execute(vphUname);
                copy = pm.detachCopyAll(results);
                tx.commit();
                
                
                //Stupid bug!
                if (!results.isEmpty()) {
                    Iterator<IStorageSite> iter = results.iterator();
                    stupidBugStorageSite(iter.next());
                }

            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                pm.close();
            }
        }
        return copy;
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
        String[] vphUsers = prop.getProperty(nl.uva.cs.lobcder.util.Constants.VPH_USERNAMES).split(",");
        Credential cred = new Credential(vphUsers);
        cred.setStorageSiteUsername(prop.getProperty(Constants.STORAGE_SITE_USERNAME));
        cred.setStorageSitePassword(prop.getProperty(Constants.STORAGE_SITE_PASSWORD));
        String endpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
        if (!storageSiteExists(prop)) {
            try {
                debug("Adding endpoint: " + endpoint);
                StorageSite site = new StorageSite(endpoint, cred);
                synchronized (lock) {
                    PersistenceManager pm = pmf.getPersistenceManager();
                    Transaction tx = pm.currentTransaction();
                    try {
                        tx.begin();
                        pm.makePersistent(site);
                        pm.detachCopy(site);
                        //stupidBugStorageSite(site);
                        tx.commit();

                    } finally {
                        if (tx.isActive()) {
                            tx.rollback();
                        }
                        stupidBugStorageSite(site);
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
        String uname = prop.getProperty(Constants.VPH_USERNAMES);
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

                q.setFilter("endpoint == ePoint && vphUsernames == uname");
                q.declareParameters(ePoint.getClass().getName() + " ePoint, " + uname.getClass().getName() + " uname");
                ss = (Collection<StorageSite>) q.execute(ePoint, uname);
                for (StorageSite s : ss) {
                    if (s.getEndpoint().equals(ePoint) && s.getVPHUsernames().equals(uname)) {
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
        Collection<String> children = entry.getChildren();
        Path ldri = entry.getLDRI();
        Metadata meta = entry.getMetadata();
        Collection<IStorageSite> sites = entry.getStorageSites();

        ILogicalData updated;
        try {
            if (entry.getType().equals(Constants.LOGICAL_FILE)) {
                updated = new LogicalData(ldri, Constants.LOGICAL_FILE);
            } else if (entry.getType().equals(Constants.LOGICAL_FOLDER)) {
                updated = new LogicalData(ldri, Constants.LOGICAL_FOLDER);
            } else {
                updated = new LogicalData(ldri, Constants.LOGICAL_DATA);
            }
            updated.setChildren(children);
            updated.setMetadata(meta);
            Credential cred;
            if (sites != null && !sites.isEmpty()) {
                AbstractCollection<IStorageSite> copySites = new ArrayList<IStorageSite>();
                for (IStorageSite s : sites) {
                    String[] names =new String[s.getVPHUsernames().size()];
                    names = s.getVPHUsernames().toArray(names);
                    cred = new Credential(names);
                    cred.setStorageSitePassword(s.getCredentials().getStorageSitePassword());
                    cred.setStorageSiteUsername(s.getCredentials().getStorageSiteUsername());
                    copySites.add(new StorageSite(s.getEndpoint(), cred));
                }
                updated.setStorageSites(copySites);
            }
            updated.setPDRI(entry.getPDRI());
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

    private void stupidBugLogicData(ILogicalData entry) {
        if (entry != null) {
//            //Bug! If we don't do this the ldri becomes null
            //avoid lazy loading problem
            Path ldri = entry.getLDRI();
            String type = entry.getType();
            entry.getPDRI();
            //When you execute your query, the implementation *probably* doesn't 
            //actually retrieve anything. Instead the objects are actually 
            //retrieved when you iterate through the list.  If you first close 
            //the EntityManager, then iterate through the list, this does not 
            //work. If you call size() before closing EntityManager things work, 
            //because the size() needs to retrieve all the results.
            Collection<String> rChildren = entry.getChildren();
            if (rChildren != null) {
                rChildren.size();
            }
            ArrayList<String> types = entry.getMetadata().getContentTypes();
////                    debug("Got back: " + ldri);
            Collection<IStorageSite> ss = entry.getStorageSites();
            for (IStorageSite s : ss) {
                stupidBugStorageSite(s);
            }
        }
    }

    private void stupidBugStorageSite(IStorageSite site) {
        String ep = site.getEndpoint();
//        debug("Endpoint: " + ep);
        Credential cred = site.getCredentials();
//        debug("Credentials: " + cred);
//        site.getUID();
        site.getInfo();
        site.getLogicalPaths();
    }
}
