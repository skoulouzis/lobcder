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
    }

    static void testQueryTime() throws Exception {
        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
        Path path = Path.path("/someResource");
        LogicalData entry = new LogicalData(path, Constants.LOGICAL_FOLDER);
        ArrayList<IStorageSite> sites = new ArrayList<IStorageSite>();
        sites.add(new StorageSite("file:///tmp", new Credential("user1".split(","))));
        entry.setStorageSites(sites);
        instance.registerResourceEntry(entry);
        int N = 100;
        long total=0;
        //Query N times for the entry and measure time
        for(int i=0;i<N;i++){
            long start = System.currentTimeMillis();
            ILogicalData res = instance.getResourceEntryByLDRI(path);
            long end = System.currentTimeMillis();
            total += (end-start);
        }
        
        System.out.println("Mean query time: "+(total/N));
        
        instance.unregisterResourceEntry(entry);
    }
}
