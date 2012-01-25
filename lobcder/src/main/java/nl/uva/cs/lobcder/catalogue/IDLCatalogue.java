/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import java.util.Properties;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;

/**
 *
 * @author S. Koulouzis
 */
public interface IDLCatalogue {

    public void registerResourceEntry(ILogicalData entry) throws CatalogueException;

    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName)
            throws Exception;

//    public IResourceEntry getResourceEntryByUID(String UID)
//            throws Exception;
    public void unregisterResourceEntry(ILogicalData entry) throws CatalogueException;

    public Boolean resourceEntryExists(ILogicalData entry) throws CatalogueException;

    public Collection<ILogicalData> getTopLevelResourceEntries() throws CatalogueException;

    public void renameEntry(Path oldPath, Path newPath) throws CatalogueException;
    
    public Collection<IStorageSite> getSitesByUname(String vphUname) throws CatalogueException;

    public boolean storageSiteExists(Properties prop)throws CatalogueException;

    public void registerStorageSite(Properties prop)throws CatalogueException;
    
}
