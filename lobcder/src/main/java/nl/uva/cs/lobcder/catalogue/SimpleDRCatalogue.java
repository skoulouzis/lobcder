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
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import nl.uva.cs.lobcder.resources.DataResourceEntry;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;

public class SimpleDRCatalogue implements IDRCatalogue {

    private static boolean debug = true;
    private final PersistenceManagerFactory pmf;

    public SimpleDRCatalogue() {
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");

    }

    @Override
    public void registerResourceEntry(IDataResourceEntry entry) throws Exception {
        IDataResourceEntry loaded = queryEntry(entry.getLDRI());
        if (loaded != null && loaded.getLDRI().getName().equals(entry.getLDRI().getName())) {
            throw new Exception("registerResourceEntry: cannot register resource " + entry.getLDRI() + " resource exists");
        }
        
        Path parentPath = entry.getLDRI().getParent();
        if (!parentPath.isRoot()) {
            addChild(parentPath, entry.getLDRI());
        }
        persistEntry(entry);

        return ;
    }

    @Override
    public IDataResourceEntry getResourceEntryByLDRI(Path logicalResourceName) throws Exception {
        debug("Quering " + logicalResourceName);
        return queryEntry(logicalResourceName);
    }

//    @Override
//    public IResourceEntry getResourceEntryByUID(String uid) throws Exception {
//        return loadEntryByUID(uid);
//    }
    @Override
    public void unregisterResourceEntry(IDataResourceEntry entry) throws Exception {
        deleteEntry(entry.getLDRI());
    }

    @Override
    public Boolean resourceEntryExists(IDataResourceEntry entry) throws Exception {
        return queryEntry(entry.getLDRI()) != null ? true : false;
    }

    @Override
    public Collection<IDataResourceEntry> getTopLevelResourceEntries() throws Exception {
        return queryTopLevelResources();
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
        }
    }

    private void persistEntry(IDataResourceEntry entry) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            pm.makePersistent(entry);
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

    private IDataResourceEntry queryEntry(Path logicalResourceName) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<DataResourceEntry> results;
        IDataResourceEntry entry = null;

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(DataResourceEntry.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == logicalResourceName.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + logicalResourceName.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path logicalResourceName");
            results = (Collection<DataResourceEntry>) q.execute(logicalResourceName);
            if (!results.isEmpty()) {
//                debug("queryEntry. Num of res: " + results.size());

                //TODO fix query 
                for (DataResourceEntry e : results) {
//                    debug("queryEntry. LDRI: " + e.getLDRI() + " UID: " + e.getUID());

                    if (e.getLDRI().getName().equals(logicalResourceName.getName())) {
//                        debug("Returning: "+e.getLDRI()+" "+e.getUID());
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

            Query q = pm.newQuery(DataResourceEntry.class);
            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == logicalResourceName.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + logicalResourceName.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path logicalResourceName");
            Collection<DataResourceEntry> results = (Collection<DataResourceEntry>) q.execute(logicalResourceName);
            if (!results.isEmpty()) {

                for (DataResourceEntry e : results) {
                    if (e.getLDRI().getName().equals(logicalResourceName.getName())) {
                        if (e.hasChildren()) {
                            throw new Exception("deleteEntry: cannot remove " + e.getLDRI() + " Is a collection");
                        }
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

    private void addChild(Path parent, Path child) {
        debug("Will add to " + parent + " " + child);
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<DataResourceEntry> results;
        IDataResourceEntry entry = null;

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(DataResourceEntry.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == parent.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + parent.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path parent");
            results = (Collection<DataResourceEntry>) q.execute(parent);
            if (!results.isEmpty()) {
                for (DataResourceEntry e : results) {
                    if (e.getLDRI().getName().equals(parent.getName())) {
                        e.addChild(child);
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
        Collection<DataResourceEntry> results;

        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(DataResourceEntry.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getName == parent.getName");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + parent.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path parent");
            results = (Collection<DataResourceEntry>) q.execute(parent);
            if (!results.isEmpty()) {
                for (DataResourceEntry e : results) {
                    if (e.getLDRI().getName().equals(parent.getName())) {
                        e.removeChild(child);
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

    private Collection<IDataResourceEntry> queryTopLevelResources() {

        //TODO Fix all the queris!
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        Collection<IDataResourceEntry> results;
        Path p = Path.path("/");
        Collection topLevel = new ArrayList<IDataResourceEntry>();


        try {
            tx.begin();
            //This query, will return objects of type DataResourceEntry
            Query q = pm.newQuery(DataResourceEntry.class);

            //restrict to instances which have the field ldri equal to some logicalResourceName
            q.setFilter("ldri.getLength == p.getLength");
            //We then import the type of our logicalResourceName parameter
            q.declareImports("import " + p.getClass().getName());
            //and the parameter itself
            q.declareParameters("Path p");
            results = (Collection<IDataResourceEntry>) q.execute(p);

            for (IDataResourceEntry e : results) {
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
}
