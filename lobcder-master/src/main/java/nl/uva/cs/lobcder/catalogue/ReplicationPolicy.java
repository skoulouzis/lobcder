package nl.uva.cs.lobcder.catalogue;

import java.util.Collection;

/**
 * Created by dvasunin on 20.01.15.
 */
public interface ReplicationPolicy {
    public Collection<Long> getSitesToReplicate();
}
