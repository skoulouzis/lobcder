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
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;

public class SimpleDLCatalogue implements IDLCatalogue {

    private static boolean debug = true;
    private final PersistenceManagerFactory pmf;

    public SimpleDLCatalogue() {
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");

    }

    @Override
    public void registerResourceEntry(ILogicalData entry) throws Exception {
        ILogicalData loaded = queryEntry(entry.getLDRI());
        if (loaded != null && comparePaths(loaded.getLDRI(), entry.getLDRI())) {
            throw new Exception("registerResourceEntry: cannot register resource " + entry.getLDRI() + " resource exists");
        }

        Path parentPath = entry.getLDRI().getParent();
        if (!parentPath.isRoot()) {
            addChild(parentPath, entry.getLDRI());
        }
        persistEntry(entry);

        return;
    }

    @Override
    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName) throws Exception {
        debug("Quering " + logicalResourceName);
        return queryEntry(logicalResourceName);
    }

//    @Override
//    public IResourceEntry getResourceEntryByUID(String uid) throws Exception {
//        return loadEntryByUID(uid);
//    }
    @Override
    public void unregisterResourceEntry(ILogicalData entry) throws Exception {
        deleteEntry(entry.getLDRI());
    }

    @Override
    public Boolean resourceEntryExists(ILogicalData entry) throws Exception {
        return queryEntry(entry.getLDRI()) != null ? true : false;
    }

    @Override
    public Collection<ILogicalData> getTopLevelResourceEntries() throws Exception {
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
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<LogicalData> results;
        ILogicalData entry = null;

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == logicalResourceName.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + logicalResourceName.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path logicalResourceName");
            results = (Collection<LogicalData>) q.execute(logicalResourceName);
            if (!results.isEmpty()) {
//                debug("queryEntry. Num of res: " + results.size());

                //TODO fix query 
                for (LogicalData e : results) {
//                    debug("queryEntry. LDRI: " + e.getLDRI() + " UID: " + e.getUID());
                    if (comparePaths(e.getLDRI(), logicalResourceName)) {
//                        debug("Returning: " + e.getLDRI() + " " + e.getUID());
                        entry = e;
                        break;
                    }
                }
//                Object id = pm.getObjectId(entry);
//                debug("queryEntry. DB UID class: " + id.getClass().getName() + " UID: " + id);
            }

            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }
        return entry;
    }

    private void deleteEntry(Path logicalResourceName) throws Exception {
        debug("deleteEntry: " + logicalResourceName);

        //first remove this node from it's parent 
        if (!logicalResourceName.isRoot()) {
            removeChild(logicalResourceName.getParent(), logicalResourceName);
        }

        //Next the node 
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            Query q = pm.newQuery(LogicalData.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == logicalResourceName.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + logicalResourceName.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path logicalResourceName");
            Collection<LogicalData> results = (Collection<LogicalData>) q.execute(logicalResourceName);
            if (!results.isEmpty()) {

                for (LogicalData e : results) {
                    if (comparePaths(e.getLDRI(), logicalResourceName)) {
                        if (e.hasChildren()) {
                            throw new Exception("deleteEntry: cannot remove " + e.getLDRI() + " Is a collection");
                        }
                        pm.deletePersistent(e);
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

    private void addChild(Path parent, Path child) {
        debug("Will add to " + parent + " " + child);
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<LogicalData> results;
        ILogicalData entry = null;

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == parent.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + parent.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path parent");
            results = (Collection<LogicalData>) q.execute(parent);
            if (!results.isEmpty()) {
                for (LogicalData e : results) {
                    if (comparePaths(e.getLDRI(), parent)) {
                        e.addChild(child);
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

    private void removeChild(Path parent, Path child) {
        debug("Will remove from " + parent + " " + child);
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<LogicalData> results;

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == parent.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + parent.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path parent");
            results = (Collection<LogicalData>) q.execute(parent);
            if (!results.isEmpty()) {
                for (LogicalData e : results) {
                    if (comparePaths(e.getLDRI(), parent)) {
                        e.removeChild(child);
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
    public void renameEntry(Path oldPath, Path newPath) throws Exception {
        debug("renameEntry.");
        debug("\t entry: " + oldPath + " newName: " + newPath);

        
        ILogicalData loaded = queryEntry(oldPath);

        if (loaded == null) {
            throw new Exception("renameEntry: cannot rename resource " + oldPath + " resource doesn't exists");
        }

        removeChild(oldPath.getParent(), oldPath);
        

        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<LogicalData> results;
        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(LogicalData.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == oldPath.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + oldPath.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path oldLdri");
            results = (Collection<LogicalData>) q.execute(oldPath);
            if (!results.isEmpty()) {
                for (LogicalData e : results) {
                    if (comparePaths(e.getLDRI(), oldPath)) {
                        e.setLDRI(newPath);
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

        addChild(newPath.getParent(), newPath);
    }

    private boolean comparePaths(Path path1, Path path2) {
        if (path1.toString().equals(path2.toString())) {
            return true;
        }
        return false;
    }
}
