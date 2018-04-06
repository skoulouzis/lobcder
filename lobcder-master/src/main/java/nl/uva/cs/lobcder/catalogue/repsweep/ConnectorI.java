package nl.uva.cs.lobcder.catalogue.repsweep;

import nl.uva.cs.lobcder.catalogue.beans.PdriBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriGroupBean;
import nl.uva.cs.lobcder.catalogue.beans.StorageSiteBean;
import nl.uva.cs.lobcder.resources.PDRI;

import java.util.Collection;
import java.util.Set;

/**
 * Created by dvasunin on 28.02.15.
 */
public interface ConnectorI {
    public Set<PdriGroupBean> selectPdriGroupsToRelocate(Long localCacheId) throws Exception;

    public Set<StorageSiteBean> getRemovingStorage() throws Exception;

    public void reportNewReplica(PdriBean destinationPdriBean, PdriGroupBean pdriGroupBean) throws Exception;

    public void reportDeletedPdri(PdriBean pdriBean) throws Exception;

    public void reportPdriGroupDone(PdriGroupBean pdriGroupBean) throws Exception;

    public void reportPdriGroupRelease(PdriGroupBean pdriGroupBean) throws Exception;

}
