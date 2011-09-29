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
    private PersistenceManager pm = null;

    public SimpleDRCatalogue() {
        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
        pm = pmf.getPersistenceManager();

    }

    @Override
    public void registerResourceEntry(IDataResourceEntry entry) throws Exception {
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

    @Override
    public IDataResourceEntry getResourceEntryByLDRI(Path logicalResourceName) throws Exception {

        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            //Find query !!
                Query q = pm.newQuery("SELECT FROM " + DataResourceEntry.class.getName());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }

        return null;
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

    private IDataResourceEntry loadEntry(Path logicalResourceName) {
        return new DataResourceEntry(logicalResourceName);
    }

    void printFSTree() {
    }
}
