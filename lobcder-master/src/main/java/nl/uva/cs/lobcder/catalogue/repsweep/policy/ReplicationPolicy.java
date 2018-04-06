package nl.uva.cs.lobcder.catalogue.repsweep.policy;

import nl.uva.cs.lobcder.catalogue.beans.StorageSiteBean;

import java.util.Collection;
import java.util.Set;

/**
 * Created by dvasunin on 06.03.15.
 */
public interface ReplicationPolicy {
    public Set<StorageSiteBean> getSitesToReplicate() throws Exception;
}
