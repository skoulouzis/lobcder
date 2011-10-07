/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import java.util.List;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;

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

    public void renameEntry(IDataResourceEntry entry, Path newName);
}
