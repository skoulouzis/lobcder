/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import java.util.List;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import nl.uva.cs.lobcder.webDav.resources.DataFileResource;

/**
 *
 * @author S. Koulouzis
 */
public interface IDRCatalogue {

    public void registerResourceEntry(IDataResourceEntry entry) throws Exception;

    public IDataResourceEntry getResourceEntryByLDRI(Path logicalResourceName)
            throws Exception;

//    public IResourceEntry getResourceEntryByUID(String UID)
//            throws Exception;
    public void unregisterResourceEntry(IDataResourceEntry entry) throws Exception;

    public Boolean resourceEntryExists(IDataResourceEntry entry) throws Exception;

    public Collection<IDataResourceEntry> getTopLevelResourceEntries() throws Exception;

    public void renameEntry(Path oldPath, Path newPath) throws Exception;
    
}
