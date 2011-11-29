/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import java.util.List;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.webDav.resources.WebDataFileResource;

/**
 *
 * @author S. Koulouzis
 */
public interface IDRCatalogue {

    public void registerResourceEntry(ILogicalData entry) throws Exception;

    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName)
            throws Exception;

//    public IResourceEntry getResourceEntryByUID(String UID)
//            throws Exception;
    public void unregisterResourceEntry(ILogicalData entry) throws Exception;

    public Boolean resourceEntryExists(ILogicalData entry) throws Exception;

    public Collection<ILogicalData> getTopLevelResourceEntries() throws Exception;

    public void renameEntry(Path oldPath, Path newPath) throws Exception;
    
}
