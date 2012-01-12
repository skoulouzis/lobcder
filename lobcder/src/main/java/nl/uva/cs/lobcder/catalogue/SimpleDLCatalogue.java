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
import java.util.List;
import javax.jdo.Extent;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.resources.StorageSiteManager;
import nl.uva.vlet.data.StringUtil;

public class SimpleDLCatalogue implements IDLCatalogue {

    private static boolean debug = true;
    private final PersistenceManagerFactory pmf;

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
        Collection<StorageSite> sites = entry.getStorageSites();
        if (sites != null && sites.isEmpty()) {
            StorageSiteManager sm = new StorageSiteManager();
            sm.deleteStorgaeSites(entry.getStorageSites());
        }
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
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            pm.makePersistent(entry);
//            pm.makePersistent(entry.getMetadata());
//            Object id = pm.getObjectId(entry);
//            debug("persistEntry. DB UID class: " + id.getClass().getName() + " UID: " + id);
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            //!?!?!?! if this is not here, the entry's LDRI gets to null???
            entry.getLDRI();
            pm.close();
        }
    }

    private ILogicalData queryEntry(Path logicalResourceName) {
        String strLogicalResourceName = logicalResourceName.toString();
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
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
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

    private void addChild(Path parent, Path child) throws NonExistingResourceException {
        debug("Will add to " + parent + " " + child);
        String strLogicalResourceName = parent.toString();

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

    private void removeChild(Path parent, Path child) throws NonExistingResourceException {
        debug("Will remove from " + parent + " " + child);
        String strLogicalResourceName = parent.toString();
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

    private Collection<ILogicalData> queryTopLevelResources() {

        //TODO Fix all the queris!
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<ILogicalData> results;
        Path p = Path.path("/");
        Collection topLevel = new ArrayList<ILogicalData>();


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




        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        String[] parts;
        String addition;
        String strNewChildPath = "";
        Path newChildPath;
        LogicalData child;
        String strChildLRN;
        ILogicalData loadedChild;

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
            entry.removeChildren(children);

            FIX ME: RENAME CHILDREN 
                    
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
                strNewChildPath = "";

                entry.addChild(newChildPath);
                entry.removeChild(p);

                strChildLRN = p.toString();
                Query q2 = pm.newQuery(LogicalData.class);
                q2.setFilter("strLDRI == strChildLRN");
                q2.declareParameters(strLogicalResourceName.getClass().getName() + " strChildLRN");
                q2.setUnique(true);
                loadedChild = (ILogicalData) q2.execute(strChildLRN);
                if (loadedChild != null) {
                    loadedChild.setLDRI(newChildPath);
                } else {
                    child = new LogicalData(newChildPath);
                    pm.makePersistent(child);
                }

            }

            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
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
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection c;
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
        return c;
    }
}
