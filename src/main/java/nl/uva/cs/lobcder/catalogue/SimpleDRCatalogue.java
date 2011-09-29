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
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import nl.uva.cs.lobcder.resources.DataResourceEntry;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import org.datanucleus.query.typesafe.TypesafeQuery;

public class SimpleDRCatalogue implements IDRCatalogue {

    private static boolean debug = true;
    private final PersistenceManagerFactory pmf;

    public SimpleDRCatalogue() {
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");

    }

    @Override
    public void registerResourceEntry(IDataResourceEntry entry) throws Exception {
        IDataResourceEntry loaded = queryEntry(entry.getLDRI());

        if (loaded!=null && loaded.getLDRI().getName().equals(entry.getLDRI().getName())) {
            throw new Exception("mkdir: cannot register resource " + entry.getLDRI() + " resource exists");
        }
        persistEntry(entry);
    }

    @Override
    public IDataResourceEntry getResourceEntryByLDRI(Path logicalResourceName) throws Exception {
        return queryEntry(logicalResourceName);
    }

//    @Override
//    public IResourceEntry getResourceEntryByUID(String uid) throws Exception {
//        return loadEntryByUID(uid);
//    }
    @Override
    public void unregisterResourceEntry(IDataResourceEntry entry) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean resourceEntryExists(IDataResourceEntry entry) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IDataResourceEntry getRoot() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<IDataResourceEntry> getTopLevelResourceEntries() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
        }
    }

    void printFSTree() {
    }

    private void persistEntry(IDataResourceEntry entry) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            pm.makePersistent(entry);
            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
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
            if(!results.isEmpty()){
                entry = results.iterator().next();
            }
            
            //            for (DataResourceEntry e : results) {
            //                debug("LDRI: " + e.getLDRI() + " UID: " + e.getUID());
            //            }

        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }
        return entry;
    }
}
