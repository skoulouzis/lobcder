/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.MyStorageSite;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.webDav.resources.WebDataDirResource;

/**
 *
 * @author S. Koulouzis
 */
public interface IDLCatalogue {

    public ILogicalData registerPdriForNewEntry(Long logicalDataUID, PDRI pdri) throws Exception;

    public Collection<PDRI> getPdriByGroupId(Long GroupId);

    public void registerResourceEntry(WebDataDirResource parent, ILogicalData entry) throws CatalogueException;

    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName)
            throws Exception;

    public ILogicalData getResourceEntryByUID(Long UID) throws Exception;

    public void removeResourceEntry(Path entry) throws Exception;

    public Collection<ILogicalData> getChildren(WebDataDirResource parent);

    public void moveEntry(Long entryId, WebDataDirResource newParent, String newName) throws Exception;

    public void copyEntry(Long entryId, List<Integer> perm, WebDataDirResource newParent, String newName) throws Exception;

    public Collection<MyStorageSite> getStorageSitesByUser(MyPrincipal user) throws CatalogueException;

    public void updateResourceEntry(ILogicalData newResource) throws Exception;

    public Runnable deleteSweep();

    public void removeResourceEntryBulk(Path ldrI);

    public void registerStorageSite(MyStorageSite ss) throws CatalogueException;
}
