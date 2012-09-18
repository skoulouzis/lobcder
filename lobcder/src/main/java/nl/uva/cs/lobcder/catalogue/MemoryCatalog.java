/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.webDav.resources.WebDataDirResource;
import nl.uva.vlet.exception.VlException;

/**
 *
 * @author dvasunin
 */
public class MemoryCatalog implements IDLCatalogue {

    final Map<Long, ILogicalData> uidToLD = new HashMap<Long, ILogicalData>();
    final Map<String, ILogicalData> pathToLD = new HashMap<String, ILogicalData>();
    final Map<String, HashSet<ILogicalData>> parentPathToChildren = new HashMap<String, HashSet<ILogicalData>>() {

        @Override
        public HashSet<ILogicalData> get(Object key) {
            HashSet<ILogicalData> res = super.get(key);
            if (res == null) {
                res = new HashSet<ILogicalData>();
                super.put((String) key, res);
            }
            return res;
        }
    };
    final Map<Long, PDRIGroup> pdriGroupIdToPdriGroup = new HashMap<Long, PDRIGroup>();
    final Map<Long, PDRI> pdriIdToPdri = new HashMap<Long, PDRI>();
    private final Map<String, List<MyStorageSite>> userToStorageSites = new HashMap<String, List<MyStorageSite>>();

    public MemoryCatalog() {
        Path ldri = Path.path("/");
        LogicalData root = new LogicalData(ldri, Constants.LOGICAL_FOLDER);
        ArrayList<Integer> permArr = new ArrayList<Integer>();
        permArr.add(0);
        permArr.add(Permissions.OWNER_ROLE | Permissions.READWRITE);
        permArr.add(Permissions.REST_ROLE | Permissions.READ);
        permArr.add(Permissions.ROOT_ADMIN | Permissions.READWRITE);
        Metadata meta = root.getMetadata();
        meta.setPermissionArray(permArr);
        meta.setCreateDate(System.currentTimeMillis());
        meta.setModifiedDate(meta.getCreateDate());
        root.setMetadata(meta);
        uidToLD.put(root.getUID(), root);
        pathToLD.put("", root);

        TimerTask gcTask = new TimerTask() {

            Runnable sweep = deleteSweep();

            @Override
            public void run() {
                sweep.run();
            }
        };
        new Timer(true).schedule(gcTask, 10000, 10000); //once in 10 sec
    }

    @Override
    public ILogicalData registerPdriForNewEntry(Long logicalDataUID, PDRI pdri) throws Exception {
        synchronized (this) {
            ILogicalData ld = uidToLD.get(logicalDataUID);
            if (ld != null) {
                PDRIGroup prdiGroup;
                Long pdriGroupId = ld.getPdriGroupId();
                if (pdriGroupId != null) {
                    prdiGroup = pdriGroupIdToPdriGroup.get(pdriGroupId);
                    if (prdiGroup != null) {
                        PDRIGroup.Accessor.setRefCount(prdiGroup, PDRIGroup.Accessor.getRefCount(prdiGroup) - 1);
                    }
                }
                prdiGroup = new PDRIGroup();
                prdiGroup.addPdriRef(pdri.getPdriId());
                PDRIGroup.Accessor.setRefCount(prdiGroup, PDRIGroup.Accessor.getRefCount(prdiGroup) + 1);
                ld.setPdriGroupId(prdiGroup.getGroupId());
                pdriIdToPdri.put(pdri.getPdriId(), pdri);
                pdriGroupIdToPdriGroup.put(prdiGroup.getGroupId(), prdiGroup);
            }
            return (ILogicalData) ld.clone();
        }
    }

    @Override
    public Collection<PDRI> getPdriByGroupId(Long GroupId) {
        ArrayList<PDRI> res = new ArrayList<PDRI>();
        synchronized (this) {
            for (Long pdriId : pdriGroupIdToPdriGroup.get(GroupId).getPdriIds()) {
                res.add(pdriIdToPdri.get(pdriId));
            }
        }
        return res;
    }

    @Override
    public void registerResourceEntry(WebDataDirResource parent, ILogicalData myentry) throws CatalogueException {
        ILogicalData entry = (ILogicalData) myentry.clone();
        synchronized (this) {
            try {
                if (getResourceEntryByLDRI(entry.getLDRI()) != null) {
                    throw new CatalogueException(entry.getLDRI().toPath() + " exists. Cannot register new");
                }
            } catch (Exception ex) {
                throw new CatalogueException(ex.getMessage());
            }
            entry.setLDRI(parent.getLogicalData().getLDRI().toPath(), entry.getName());
            uidToLD.put(entry.getUID(), entry);
            pathToLD.put(entry.getLDRI().toPath(), entry);
            parentPathToChildren.get(entry.getParent()).add(entry);
        }
    }

    @Override
    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName) throws Exception {
        ILogicalData res;
        synchronized (this) {
            res = (ILogicalData) pathToLD.get(logicalResourceName.toPath());
            if (res != null) {
                res = (ILogicalData) res.clone();
            }
            return res;
        }
    }

    @Override
    public ILogicalData getResourceEntryByUID(Long UID) throws Exception {
        ILogicalData res;
        synchronized (this) {
            return (ILogicalData) uidToLD.get(UID).clone();
        }
    }

    @Override
    public void removeResourceEntry(Path childToRemove) throws Exception {
        String myPath = childToRemove.toPath();
        synchronized (this) {
            ILogicalData toRemove = pathToLD.remove(myPath);
            uidToLD.remove(toRemove.getUID());
            parentPathToChildren.get(childToRemove.getParent().toPath()).remove(toRemove);
            if (toRemove.getPdriGroupId() != null) {
                PDRIGroup pgrig = pdriGroupIdToPdriGroup.get(toRemove.getPdriGroupId());
                if (pgrig != null) {
                    PDRIGroup.Accessor.setRefCount(pgrig, PDRIGroup.Accessor.getRefCount(pgrig) - 1);
                }
            }
        }
    }

    @Override
    public Collection<ILogicalData> getChildren(WebDataDirResource parent) {
        Collection<ILogicalData> result = new ArrayList<ILogicalData>();
        synchronized (this) {
            for (ILogicalData ld : parentPathToChildren.get(parent.getLogicalData().getLDRI().toPath())) {
                result.add((ILogicalData) ld.clone());
            }
        }
        return result;
    }

    @Override
    public void moveEntry(Long entryId, WebDataDirResource newParent, String newName) throws Exception {
        String newParentForEntryStr = newParent.getLogicalData().getLDRI().toPath();
        if (newParent.child(newName) != null) {
            throw new ResourceExistsException(newParent.getLogicalData().getLDRI().child(newName).toPath() + " exists, cannot move");
        }
        synchronized (this) {
            ILogicalData entry = uidToLD.get(entryId);
            final String entryCurrentPathStr = entry.getLDRI().toPath();

            pathToLD.remove(entryCurrentPathStr); // put to variable an entry from HashMap - must be the same in all collections
            parentPathToChildren.get(entry.getParent()).remove(entry); // entry is removed from both maps
            entry.setLDRI(newParentForEntryStr, newName); // set new LDRI for entry
            pathToLD.put(entry.getLDRI().toPath(), entry);
            parentPathToChildren.get(newParentForEntryStr).add(entry);// put the entry to maps
            final String newEntryPathStr = entry.getLDRI().toPath();// all children shall change their prefixes to this string
            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) { //if entry is a folder replace paths for the children (all in depth)
                for (ILogicalData ld : uidToLD.values()) {
                    String parentPathStr = ld.getParent();
                    if (parentPathStr.startsWith(entryCurrentPathStr)) {
                        String oldPath = ld.getLDRI().toPath();
                        String oldParent = ld.getParent();
                        String newParentPath = parentPathStr.replaceFirst(entryCurrentPathStr, newEntryPathStr);
                        ld.setLDRI(newParentPath, ld.getName()); // change LDRI for a child
                        ILogicalData myld = pathToLD.remove(oldPath);//remove child from maps
                        parentPathToChildren.get(oldParent).remove(myld);
                        pathToLD.put(ld.getLDRI().toPath(), ld); // register new paths in maps
                        parentPathToChildren.get(ld.getParent()).add(ld);
                    }
                }
            }
        }
    }

    @Override
    public void copyEntry(Long entryId, List<Integer> perm, WebDataDirResource newParent, String newName) throws Exception {
        String newParentForEntryStr = newParent.getLogicalData().getLDRI().toPath();
        if (newParent.child(newName) != null) {
            System.err.println("QQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQ");
            throw new ResourceExistsException(newParent.getLogicalData().getLDRI().child(newName).toPath() + " exists, cannot copy");
        }
        synchronized (this) {
            ILogicalData entry = uidToLD.get(entryId);
            final String entryCurrentPathStr = entry.getLDRI().toPath();
            //entry = pathToLD.remove(entryCurrentPathStr); // put to variable an entry from HashMap - must be the same in all collections
            //parentPathToChildren.get(entry.getParent()).remove(entry); // entry is removed from both maps
            ILogicalData newEntry = new LogicalData(entry.getLDRI(), entry.getType());
            newEntry.setMetadata((Metadata) entry.getMetadata().clone());
            newEntry.getMetadata().setCreateDate(System.currentTimeMillis());
            newEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            newEntry.getMetadata().setPermissionArray(perm);
            newEntry.setLDRI(newParentForEntryStr, newName); // set new LDRI for entry
            newEntry.setPdriGroupId(entry.getPdriGroupId());
            PDRIGroup pdriGroup = pdriGroupIdToPdriGroup.get(newEntry.getPdriGroupId());
            if (pdriGroup != null) {
                PDRIGroup.Accessor.setRefCount(pdriGroup, PDRIGroup.Accessor.getRefCount(pdriGroup) + 1);
            }
            pathToLD.put(newEntry.getLDRI().toPath(), newEntry);
            parentPathToChildren.get(newParentForEntryStr).add(newEntry);// put the entry to maps
            uidToLD.put(newEntry.getUID(), newEntry);
            final String newEntryPathStr = newEntry.getLDRI().toPath();// all children shall change their prefixes to this string
            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) { //if entry is a folder replace paths for the children (all in depth)
                LinkedList<ILogicalData> copiedData = new LinkedList<ILogicalData>();
                for (ILogicalData ld : uidToLD.values()) {
                    String parentPathStr = ld.getParent();
                    if (parentPathStr.startsWith(entryCurrentPathStr)) {
                        String oldPath = ld.getLDRI().toPath();
                        String oldParent = ld.getParent();
                        String newParentPath = parentPathStr.replaceFirst(entryCurrentPathStr, newEntryPathStr);
                        ILogicalData newE = new LogicalData(ld.getLDRI(), ld.getType());
                        newE.setMetadata((Metadata) ld.getMetadata().clone());
                        newE.getMetadata().setCreateDate(System.currentTimeMillis());
                        newE.getMetadata().setModifiedDate(System.currentTimeMillis());
                        newE.getMetadata().setPermissionArray(perm);
                        newE.setLDRI(newParentPath, newE.getName()); // change LDRI for a child
                        newE.setPdriGroupId(ld.getPdriGroupId());
                        pdriGroup = pdriGroupIdToPdriGroup.get(newE.getPdriGroupId());
                        if (pdriGroup != null) {
                            PDRIGroup.Accessor.setRefCount(pdriGroup, PDRIGroup.Accessor.getRefCount(pdriGroup) + 1);
                        }
                        copiedData.add(newE);
                    }
                }
                for (ILogicalData ld : copiedData) {
                    uidToLD.put(ld.getUID(), ld);
                    pathToLD.put(ld.getLDRI().toPath(), ld); // register new paths in maps
                    parentPathToChildren.get(ld.getParent()).add(ld);
                }
            }
        }
    }

    @Override
    public Collection<MyStorageSite> getStorageSitesByUser(MyPrincipal user) throws CatalogueException {
        //for testing to work, register the /tmp as a storage site.
//        List<MyStorageSite> sites = userToStorageSites.get(user.getToken());
//        if(sites==null || sites.isEmpty()){
//            MyStorageSite ss = new MyStorageSite();
//            ss.addAllowedUser(user);
//            ss.setResourceURI("file:///tmp/");
//            registerStorageSite(ss);
//        }
        return userToStorageSites.get(user.getToken());
    }

    @Override
    public void registerStorageSite(MyStorageSite ss) throws CatalogueException {
//        try {
//            List<MyPrincipal> users = ss.getAllowedUsers();
//            for (MyPrincipal p : users) {
//                List<MyStorageSite> sites = userToStorageSites.get(p);
//                if(sites==null){
//                    sites = new ArrayList<MyStorageSite>();
//                }
//                sites.add(ss);
//                userToStorageSites.put(p.getToken(), sites);
//            }
//        } catch (MyPrincipal.Exception ex) {
//            throw new CatalogueException(ex.getMessage());
//        }
    }

    @Override
    public void updateResourceEntry(ILogicalData newResource) throws Exception {
        synchronized (this) {
            if (getResourceEntryByUID(newResource.getUID()) == null) {
                throw new CatalogueException("Cannot update non-existing ILogicalData: " + newResource.getLDRI().toPath());
            }
            ILogicalData entry = (ILogicalData) newResource.clone();
            uidToLD.put(entry.getUID(), entry);
            pathToLD.put(entry.getLDRI().toPath(), entry);
            Set<ILogicalData> myset = parentPathToChildren.get(entry.getParent());
            myset.remove(entry);
            myset.add(entry);
        }
    }

    @Override
    public Runnable deleteSweep() {
        final MemoryCatalog self = this;
        return new Runnable() {

            @Override
            public void run() {
                Collection<PDRI> toRemove = new LinkedList<PDRI>();
                synchronized (self) {
                    Iterator<Map.Entry<Long, PDRIGroup>> it = pdriGroupIdToPdriGroup.entrySet().iterator();
                    while (it.hasNext()) {
                        PDRIGroup pdriGroup = it.next().getValue();
                        if (PDRIGroup.Accessor.getRefCount(pdriGroup) == 0) {
                            for (Long pdriId : pdriGroup.getPdriIds()) {
                                toRemove.add(pdriIdToPdri.remove(pdriId));
                            }
                            it.remove();
                        }
                    }
                }
                for (PDRI pdri : toRemove) {
                    try {
                        pdri.delete();
                    } catch (IOException ex) {
                        Logger.getLogger(MemoryCatalog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
    }

    @Override
    public void removeResourceEntryBulk(Path ldrI) {
        System.err.println("################################### removeResourceEntryBulk" + ldrI.toPath());
        synchronized (this) {
            Iterator<Map.Entry<Long, ILogicalData>> it = uidToLD.entrySet().iterator();
            while (it.hasNext()) {
                ILogicalData ld = it.next().getValue();
                if (ld.getLDRI().toPath().startsWith(ldrI.toPath())) {
                    if (ld.getPdriGroupId() != null) {
                        PDRIGroup pgrig = pdriGroupIdToPdriGroup.get(ld.getPdriGroupId());
                        if (pgrig != null) {
                            PDRIGroup.Accessor.setRefCount(pgrig, PDRIGroup.Accessor.getRefCount(pgrig) - 1);
                        }
                    }
                    it.remove();
                    parentPathToChildren.get(ld.getParent()).remove(ld);
                    pathToLD.remove(ld.getLDRI().toPath());
                }
            }
        }
    }

    public void removeResourceEntryBulk1(Path currentPath, MyPrincipal principal) throws Exception {
        System.err.println("################################### removeResourceEntryBulk1" + currentPath.toPath());
        synchronized (this) {

            ILogicalData currentLD = pathToLD.get(currentPath.toPath());
            if (currentLD != null) {
                Permissions perm = new Permissions(currentLD.getMetadata().getPermissionArray());
                if (perm.canRead(principal) && perm.canWrite(principal)) {
                    //process files
                    Iterator<ILogicalData> it = parentPathToChildren.get(currentLD.getLDRI().toPath()).iterator();
                    while (it.hasNext()) {
                        ILogicalData ld = it.next();
                        if (ld.getType().equals(Constants.LOGICAL_FILE)) {
                            PDRIGroup pgrig = pdriGroupIdToPdriGroup.get(ld.getPdriGroupId());
                            if (pgrig != null) {
                                PDRIGroup.Accessor.setRefCount(pgrig, PDRIGroup.Accessor.getRefCount(pgrig) - 1);
                            }
                            uidToLD.remove(ld.getUID());
                            pathToLD.remove(ld.getLDRI().toPath());
                            it.remove();
                        }
                    }
                    //process folders
                    it = parentPathToChildren.get(currentLD.getLDRI().toPath()).iterator();
                    while (it.hasNext()) {
                        ILogicalData ld = it.next();
                        if (ld.getType().equals(Constants.LOGICAL_FOLDER)) {
                            removeResourceEntryBulk1(ld.getLDRI(), principal);
                            uidToLD.remove(ld.getUID());
                            pathToLD.remove(ld.getLDRI().toPath());
                            it.remove();
                        }
                    }
                } else {
                    throw new NotAuthorizedException();
                }
            }
        }
    }
}
