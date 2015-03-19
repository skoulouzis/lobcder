package nl.uva.cs.lobcder.catalogue.repsweep;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.beans.ItemBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriGroupBean;
import nl.uva.cs.lobcder.catalogue.beans.StorageSiteBean;
import nl.uva.cs.lobcder.catalogue.repsweep.policy.ReplicationPolicy;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.DeleteHelper;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * Created by dvasunin on 28.02.15.
 */

@Log
public class ReplicateSweep implements Runnable {

    private final ConnectorI connector;
    private final Long localCacheId;
    private final ReplicationPolicy replicationPolicy;

    public ReplicateSweep(ConnectorI connector) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.connector = connector;
        localCacheId = PropertiesHelper.getLocalCacheId();
        replicationPolicy = Class.forName(PropertiesHelper.getReplicationPolicy()).asSubclass(ReplicationPolicy.class).newInstance();
    }

    @Override
    public void run() {
        try{
            Collection<PdriGroupBean> pdriGroupBeanCollection = connector.selectPdriGroupsToRelocate(localCacheId);
            if(pdriGroupBeanCollection != null) {
                for (PdriGroupBean pdriGroupBean : pdriGroupBeanCollection) {
                    try {
                        Set<StorageSiteBean> preferences = new HashSet<>();
                        if (pdriGroupBean.getItem() != null) {
                            for (ItemBean itemBean : pdriGroupBean.getItem()) {
                                Collection<StorageSiteBean> itemPreferences = itemBean.getPreference();
                                if (itemPreferences != null) preferences.addAll(itemPreferences);
                            }
                        }
                        Set<StorageSiteBean> toReplicate = new HashSet<>(preferences);
                        Set<PdriBean> wantRemove = new HashSet<>();
                        Set<StorageSiteBean> removingStorage = connector.getRemovingStorage();
                        Set<PdriBean> noCache = new HashSet<>();
                        Set<PdriBean> remoteCache = new HashSet<>();
                        if (pdriGroupBean.getPdri() != null) {
                            for (PdriBean pdriBean : pdriGroupBean.getPdri()) {
                                if (isLocalCachePdri(pdriBean)) {
                                    wantRemove.add(pdriBean);
                                } else {
                                    if (pdriBean.getStorage().getCache()) {
                                        remoteCache.add(pdriBean);
                                    } else {
                                        if (preferences.isEmpty()) {
                                            if (removingStorage.contains(pdriBean.getStorage())) {
                                                wantRemove.add(pdriBean);
                                            } else {
                                                noCache.add(pdriBean);
                                            }
                                        } else {
                                            if (preferences.contains(pdriBean.getStorage())) {
                                                toReplicate.remove(pdriBean.getStorage());
                                            } else {
                                                wantRemove.add(pdriBean);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (preferences.isEmpty()) {
                            if (noCache.isEmpty()) {
                                replicate(pdriGroupBean, replicationPolicy.getSitesToReplicate());
                            }
                        } else {
                            replicate(pdriGroupBean, toReplicate);
                        }
                        removePdris(wantRemove);
                        if (remoteCache.isEmpty()) {
                            connector.reportPdriGroupDone(pdriGroupBean);
                        } else {
                            connector.reportPdriGroupRelease(pdriGroupBean);
                        }
                    } catch (Exception e) {
                        log.log(Level.SEVERE, null, e);
                        connector.reportPdriGroupRelease(pdriGroupBean);
                    }
                }
            }
        } catch(Exception e) {
            log.log(Level.SEVERE, null, e);
        }
    }


    private void removePdris(Set<PdriBean> removePdriBeans) throws Exception{
        for(PdriBean pdriBean : removePdriBeans) {
            DeleteHelper.delete(pdriBean);
            connector.reportDeletedPdri(pdriBean);
        }
    }

    private boolean isLocalCachePdri(PdriBean pdriBean) {
        return pdriBean.getStorage().getId().equals(localCacheId);
    }

    private PdriBean getSourcePdriForGroup(PdriGroupBean source) {
        Set<PdriBean> noCache = new HashSet<>();
        for (PdriBean pdriBean : source.getPdri()) {
            if(isLocalCachePdri(pdriBean)) return pdriBean;
            if(!pdriBean.getStorage().getCache()) noCache.add(pdriBean);
        }

        PdriBean[] arr = noCache.toArray(new PdriBean[noCache.size()]);
        return arr[new Random().nextInt(arr.length)];
    }

    private String generateFileName(PdriBean pdriBean) {
        return pdriBean.getName();
    }

    private void replicate(PdriGroupBean source, Set<StorageSiteBean> toReplicate) throws Exception{
        PDRI destinationPdri = null;
        PdriBean sourcePdriBean = getSourcePdriForGroup(source);
        PDRI sourcePdri =  PDRIFactory.getFactory().createInstance(sourcePdriBean);
        try {
            for (StorageSiteBean storageSiteBean : toReplicate) {
                BigInteger pdriKey = DesEncrypter.generateKey();
                PdriBean destinationPdriBean= new PdriBean(
                        null,
                        generateFileName(sourcePdriBean),
                        pdriKey,
                        storageSiteBean
                );
                destinationPdri = PDRIFactory.getFactory().createInstance(destinationPdriBean);
                if (!destinationPdri.exists(destinationPdri.getFileName())) {
                    destinationPdri.replicate(sourcePdri);
                }
                connector.reportNewReplica(destinationPdriBean, source);
                destinationPdri = null;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, null, e);
            if(destinationPdri != null) {
                destinationPdri.delete();
            }
            throw e;
        }
    }

}

