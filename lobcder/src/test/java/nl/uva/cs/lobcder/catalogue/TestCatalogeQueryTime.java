/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.io.File;
import java.util.ArrayList;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;

/**
 *
 * @author skoulouz
 */
public class TestCatalogeQueryTime {
    
    
    public static void main(String args[]) throws Exception{
        testQueryTime();
        System.exit(0);
    }

    static void testQueryTime() throws Exception {
        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
        
        Path path = Path.path("/r1");
        LogicalData entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        ArrayList<IStorageSite> sites = new ArrayList<IStorageSite>();
        sites.add(new StorageSite("file:///tmp", new Credential("user1".split(","))));
        entry.setStorageSites(sites);
        instance.registerResourceEntry(entry);
        
        path = Path.path("/r1/r2");
        entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        instance.registerResourceEntry(entry);
        
        path = Path.path("/r1/r3");
        entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        instance.registerResourceEntry(entry);
        
        path = Path.path("/r1/r3/r4");
        entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        entry.getMetadata().setCreateDate(System.currentTimeMillis());
        instance.registerResourceEntry(entry);
        
        path = Path.path("/r1/r3/r4/r5");
        entry = new LogicalData(path, Constants.LOGICAL_FOLDER);        
        instance.registerResourceEntry(entry);
        
        path = Path.path("/r1/r3/r4/r5/r6");
        entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        entry.getMetadata().setCreateDate(System.currentTimeMillis());
        instance.registerResourceEntry(entry);
        
        path = Path.path("/r1/r3/r4/r5/r6/r7");
        entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        entry.getMetadata().setCreateDate(System.currentTimeMillis());
        instance.registerResourceEntry(entry);
        
        path = Path.path("/r1/r3/r4/r5/r6/r7/r8");
        entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        entry.getMetadata().setCreateDate(System.currentTimeMillis());
        instance.registerResourceEntry(entry);
        
        
        double N = 100;
        double total=0;
        //Query N times for the entry and measure time
        for(int i=0;i<N;i++){
            double start = System.currentTimeMillis();
            ILogicalData res = instance.getResourceEntryByLDRI(path);
            double end = System.currentTimeMillis();
            total += (end-start);
        }
        
        double mean = total/N;
        System.out.println("Mean query without rename time: "+mean);
        Path newPath = Path.path(path.getParent(), "new");
        instance.renameEntry(path, newPath);
        
        total=0;
        //Query N times for the entry and measure time
        for(int i=0;i<N;i++){
            double start = System.currentTimeMillis();
            ILogicalData res = instance.getResourceEntryByLDRI(path);
            double end = System.currentTimeMillis();
            total += (end-start);
        }
        
        mean = total/N;
        System.out.println("Mean query with rename time: "+mean);
        
        instance.clearLogicalData();
        instance.clearAllSites();
    }
}
