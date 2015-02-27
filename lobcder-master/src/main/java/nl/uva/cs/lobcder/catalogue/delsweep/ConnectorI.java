package nl.uva.cs.lobcder.catalogue.delsweep;

import nl.uva.cs.lobcder.catalogue.beans.PdriGroupBean;

import java.util.Collection;

/**
 * Created by dvasunin on 26.02.15.
 */
public interface ConnectorI {
    public Collection<PdriGroupBean> getPdriGroupsToProcess() throws Exception;
    public void confirmPdriGroup(Long pdriGroupId) throws Exception;
}
