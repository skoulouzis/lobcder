/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.vlet.exception.ResourceNotFoundException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouzis
 */
class WebDataDirResource extends WebDataResource implements FolderResource, CollectionResource {

//    private ILogicalData entry;
//    private final IDLCatalogue catalogue;
    private boolean debug = true;

    public WebDataDirResource(IDLCatalogue catalogue, ILogicalData entry) throws IOException, Exception {
        super(catalogue, entry);
//        this.entry = entry;
        if (!getLogicalData().getType().equals(Constants.LOGICAL_FOLDER)) {
            throw new Exception("The logical data has the wonrg type: " + getLogicalData().getType());
        }
//        this.catalogue = catalogue;
        debug("Init. entry: " + getLogicalData().getLDRI());
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
//            Permissions p = new Permissions(getLogicalData().getMetadata().getPermissionArray());
//            MyPrincipal principal = getPrincipal();
//            if(!p.canWrite(principal)){
//                throw new NotAuthorizedException();
//            }
            debug("createCollection.");
            Path newCollectionPath = Path.path(getLogicalData().getLDRI(), newName);
            LogicalData newFolderEntry = (LogicalData) getCatalogue().getResourceEntryByLDRI(newCollectionPath);
            debug("\t newCollectionPath: " + newCollectionPath);
            if (newFolderEntry == null) {
                newFolderEntry = new LogicalData(newCollectionPath, Constants.LOGICAL_FOLDER);
                Metadata meta = newFolderEntry.getMetadata();
                meta.setCreateDate(System.currentTimeMillis());
                newFolderEntry.setMetadata(meta);

                Collection<IStorageSite> sites = getStorageSites();

                newFolderEntry.setStorageSites(sites);
                getCatalogue().registerResourceEntry(newFolderEntry);
            }


//            Metadata meta = newFolderEntry.getMetadata();
//            meta.setPermissionArray((new Permissions(principal).getRolesPerm()));
//            newFolderEntry.setMetadata(meta);
//            getCatalogue().updateResourceEntry(newFolderEntry);

            WebDataDirResource resource = new WebDataDirResource(getCatalogue(), newFolderEntry);
            return resource;
        } catch (Permissions.Exception e) {
            throw new NotAuthorizedException();
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(this, newName);
            }
        }
        return null;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException {
//        try {
//            Permissions p = new Permissions(getLogicalData().getMetadata().getPermissionArray());
//            MyPrincipal principal = getPrincipal();
//            if (!p.canRead(principal)) {
//                throw new NotAuthorizedException();
//            }
//        } catch (Permissions.Exception e) {
//            throw new NotAuthorizedException();
//        } catch (NotAuthorizedException e){
//            throw e;
//        } catch (Exception e) {
//            throw new NotAuthorizedException();
//        }
        try {
            debug("child.");
            Path childPath = Path.path(getLogicalData().getLDRI(), childName);
            ILogicalData child = getCatalogue().getResourceEntryByLDRI(childPath);

            if (child != null) {
                if (child.getType().equals(Constants.LOGICAL_FOLDER)) {
                    return new WebDataDirResource(getCatalogue(), child);
                }
                if (child.getType().equals(Constants.LOGICAL_FILE)) {
                    return new WebDataFileResource(getCatalogue(), child);
                }
                if (child.getType().equals(Constants.LOGICAL_DATA)) {
                    return new WebDataResource(getCatalogue(), child);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException {
        debug("getChildren.");
        ArrayList<? extends Resource> children = null;
        try {
//            ArrayList<Integer> perm = getLogicalData().getMetadata().getPermissionArray();
//            Permissions p = new Permissions(perm);
//            MyPrincipal principal = getPrincipal();
//            if(!p.canRead(principal)) {
//                throw new NotAuthorizedException(); 
//            }
            if (getLogicalData().getLDRI().isRoot()) {
                children = getTopLevelChildren();
            } else {
                children = new ArrayList<Resource>(getEntriesChildren());
            }

        } catch (Permissions.Exception e) {
            throw new NotAuthorizedException();
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        debug("Returning children: " + children);
        return children;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Resource resource;
        debug("createNew.");
        debug("\t newName: " + newName);
        debug("\t length: " + length);
        debug("\t contentType: " + contentType);

        try {
//            Metadata meta = getLogicalData().getMetadata();
//            ArrayList<Integer> array = meta.getPermissionArray();
//            Permissions p = new Permissions(array);
//            MyPrincipal principal = getPrincipal();
//            if (!p.canWrite(principal)) {
//                throw new NotAuthorizedException();
//            }
            Path newPath = Path.path(getLogicalData().getLDRI(), newName);

            LogicalData newResource = (LogicalData) getCatalogue().getResourceEntryByLDRI(newPath);
            if (newResource != null) {
                resource = updateExistingFile(newResource, length, contentType, inputStream);
            } else {
                resource = createNonExistingFile(newPath, length, contentType, inputStream);
            }


            if (!getLogicalData().getLDRI().isRoot()) {
                ILogicalData reloaded = getCatalogue().getResourceEntryByLDRI(this.getLogicalData().getLDRI());
                setLogicalData(reloaded);
            }

//            meta = ((WebDataResource) resource).getLogicalData().getMetadata();
//            meta.setPermissionArray((new Permissions(principal)).getRolesPerm());
//            ((WebDataResource) resource).getLogicalData().setMetadata(meta);

//            getCatalogue().updateResourceEntry(((WebDataResource) resource).getLogicalData());

            return resource;
        } catch (Permissions.Exception e) {
            throw new NotAuthorizedException();
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.getMessage());
        } finally {
        }
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        try {
            debug("copyTo.");
            debug("\t toCollection: " + toCollection.getName());
            debug("\t name: " + name);
//            Permissions p = new Permissions(getLogicalData().getMetadata().getPermissionArray());
//            MyPrincipal principal = getPrincipal();
//            if (!p.canRead(principal)) {
//                throw new NotAuthorizedException();
//            }
//            Permissions parentPerm = new Permissions(((WebDataResource) toCollection).getLogicalData().getMetadata().getPermissionArray());
//            if (!parentPerm.canWrite(principal)) {
//                throw new NotAuthorizedException();
//            }
            Path toCollectionLDRI = Path.path(toCollection.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);

            LogicalData newFolderEntry = new LogicalData(newLDRI, Constants.LOGICAL_FOLDER);
            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
//            newFolderEntry.getMetadata().setPermissionArray((ArrayList<Integer>) p.getRolesPerm().clone());
            getCatalogue().registerResourceEntry(newFolderEntry);

//        } catch (Permissions.Exception e) {
//            throw new NotAuthorizedException();
//        } catch (NotAuthorizedException e) {
//            throw e;
        } catch (Exception ex) {
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(this, ex.getMessage());
            }
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
//            MyPrincipal principal = getPrincipal();
//            if (getLogicalData().getLDRI().isRoot()) {
//                throw new NotAuthorizedException();
//            } else {
//                Path parentPath = getPath().getParent();
//                if (parentPath == null || parentPath.isRoot()) {
//                    if (!principal.getRoles().contains(Permissions.ROOT_ADMIN)) {
//                        throw new NotAuthorizedException();
//                    }
//                } else {
//                    ArrayList<Integer> pai = getCatalogue().getResourceEntryByLDRI(getPath().getParent()).getMetadata().getPermissionArray();
//                    Permissions p = new Permissions(pai);
//                    if (!p.canWrite(principal)) {
//                        throw new NotAuthorizedException();
//                    }
//                }
//            }
            debug("delete.");
            //TODO: physical data shall be removed AFTER we delete logical structures. All permissions checks are
            //performed on logical level 
            List<? extends Resource> children = getChildren();
            if (children != null) {
                for (Resource r : children) {
                    if (r instanceof DeletableResource) {
                        ((DeletableResource) r).delete();
                    }
                }
            }
            Collection<IStorageSite> sites = getLogicalData().getStorageSites();
            if (sites != null && !sites.isEmpty()) {
                for (IStorageSite s : sites) {
                    s.deleteVNode(getLogicalData().getPDRI());
                }
            }
            getCatalogue().unregisterResourceEntry(getLogicalData());

//        } catch (Permissions.Exception e) {
//            throw new NotAuthorizedException();
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
//        try {
//            Permissions p = new Permissions(getLogicalData().getMetadata().getPermissionArray());
//            MyPrincipal principal = getPrincipal();
//            if (!p.canRead(principal)) {
//                throw new NotAuthorizedException();
//            }
//        } catch (Exception e) {
//            throw new NotAuthorizedException();
//        }

        //List contents. Fix this to work with browsers 
        debug("sendContent.");
//        StringTemplateGroup t = new StringTemplateGroup("page");
//        StringTemplate template = t.getInstanceOf("page");
//         template.setAttribute("path", getPath().toString());
//        t.setAttribute("static", _staticContentPath);
//        t.setAttribute("subject", new SubjectWrapper(getSubject()));
//        t.setAttribute("base", UrlPathWrapper.forEmptyPath());
//         template.write(new AutoIndentWriter(new OutputStreamWriter(out, "UTF-8")));

        for (Resource r : getChildren()) {
            String s = r.getName() + " - " + r.getUniqueId();
            out.write(s.getBytes());
        }
        out.flush();
        out.close();
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        debug("getMaxAgeSeconds.");
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);
        ArrayList<String> mimeTypes;
        if (accepts != null) {
            String[] acceptsTypes = accepts.split(",");
            if (getLogicalData().getMetadata() != null && getLogicalData().getMetadata().getContentTypes() != null) {
                mimeTypes = getLogicalData().getMetadata().getContentTypes();
                for (String accessType : acceptsTypes) {
                    for (String mimeType : mimeTypes) {
                        if (accessType.equals(mimeType)) {
                            return mimeType;
                        }
                    }
                }
                return mimeTypes.get(0);
            }
        }
        return null;
    }

    @Override
    public Long getContentLength() {
        debug("getContentLength.");
        if (getLogicalData().getMetadata() != null) {
            return getLogicalData().getMetadata().getLength();
        }
        return null;
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        try {
//            MyPrincipal principal = getPrincipal();
//            if (getLogicalData().getLDRI().isRoot()) {
//                throw new NotAuthorizedException();
//            } else {
//                if (getPath().getParent().isRoot()) {
//                    if (!principal.getRoles().contains(Permissions.ROOT_ADMIN)) {
//                        throw new NotAuthorizedException();
//                    }
//                } else {
//                    ArrayList<Integer> pai = getCatalogue().getResourceEntryByLDRI(getPath().getParent()).getMetadata().getPermissionArray();
//                    Permissions p = new Permissions(pai);
//                    if (!p.canWrite(principal)) {
//                        throw new NotAuthorizedException();
//                    }
//                }
//            }
//            Permissions parentPerm = new Permissions(((WebDataResource) rDest).getLogicalData().getMetadata().getPermissionArray());
//            if (!parentPerm.canWrite(principal)) {
//                throw new NotAuthorizedException();
//            }
            debug("moveTo.");
            debug("\t rDestgetName: " + rDest.getName() + " name: " + name);
            getCatalogue().renameEntry(getLogicalData().getLDRI(), Path.path(getLogicalData().getLDRI(), name));
//        } catch (Permissions.Exception e) {
//            throw new NotAuthorizedException();
//        } catch (NotAuthorizedException e) {
//            throw e;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(rDest, ex.getMessage());
            }
        }
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        debug("\t entry.getMetadata(): " + getLogicalData().getMetadata());
        debug("\t entry.getMetadata().getCreateDate(): " + getLogicalData().getMetadata().getCreateDate());
        if (getLogicalData().getMetadata() != null && getLogicalData().getMetadata().getCreateDate() != null) {
            debug("getCreateDate. returning");
            return new Date(getLogicalData().getMetadata().getCreateDate());
        }
        debug("getCreateDate. returning");
        return null;
    }

    @Override
    protected void debug(String msg) {
//        if (debug) {
//            System.err.println(this.getClass().getSimpleName() + "." + getLogicalData().getLDRI() + ": " + msg);
//        }
    }

    private ArrayList<? extends Resource> getTopLevelChildren() throws Exception {
        Collection<ILogicalData> topEntries = getCatalogue().getTopLevelResourceEntries();
        ArrayList<Resource> children = new ArrayList<Resource>();
        for (ILogicalData e : topEntries) {
            if (e.getType().equals(Constants.LOGICAL_FOLDER)) {
                children.add(new WebDataDirResource(getCatalogue(), e));
            } else if (e.getType().equals(Constants.LOGICAL_FILE)) {
                children.add(new WebDataFileResource(getCatalogue(), e));
            } else {
                children.add(new WebDataResource(getCatalogue(), e));
            }
        }
        return children;
    }

    private Collection<Resource> getEntriesChildren() throws Exception {
        Collection<String> childrenPaths = getLogicalData().getChildren();

        Collection<Resource> children = new CopyOnWriteArrayList<Resource>();
        ArrayList<String> toBeRemoved = new ArrayList<String>();
        if (childrenPaths != null) {

            for (String p : childrenPaths) {
                debug("Adding children: " + p);
                ILogicalData ch = getCatalogue().getResourceEntryByLDRI(Path.path(p));
                if (ch == null) {
                    //We have some kind of inconsistency. It can happend that someone else calls delete on the child, which removes it from the catalog. In this case we'll belive the catalog and retun null 
//                    throw new NullPointerException("The Collection " + getLogicalData().getLDRI() + " has " + p + " registered as a child but the catalogue has no such entry");
                    //This throws java.util.ConcurrentModificationException because we change the list while iteratting 
//                    getLogicalData().removeChild();
                    toBeRemoved.add(p);
                    continue;
                }
                if (ch.getType().equals(Constants.LOGICAL_FOLDER)) {
                    children.add(new WebDataDirResource(getCatalogue(), ch));
                } else if (ch.getType().equals(Constants.LOGICAL_FILE)) {
                    children.add(new WebDataFileResource(getCatalogue(), ch));
                } else {
                    children.add(new WebDataResource(getCatalogue(), ch));
                }
            }
            getLogicalData().removeChildren(toBeRemoved);
        }
        return children;
    }

    private Resource createNonExistingFile(Path newPath, Long length, String contentType, InputStream inputStream) throws IOException, Exception {


        return asyncCreateNonExistingFile(newPath, length, contentType, inputStream);

//        return syncCreateNonExistingFile(newPath, length, contentType, inputStream);


    }

    private Resource updateExistingFile(LogicalData newResource, Long length, String contentType, InputStream inputStream) throws VlException, IOException, Exception {
        VFSNode node;

        if (!newResource.hasPhysicalData()) {
            node = newResource.createPhysicalData();
        } else {
            node = newResource.getVFSNode();
        }
        if (node != null) {
            OutputStream out = ((VFile) node).getOutputStream();
            IOUtils.copy(inputStream, out);
            if (inputStream != null) {
                inputStream.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        Metadata meta = newResource.getMetadata();
        meta.setLength(length);
        meta.addContentType(contentType);
        meta.setModifiedDate(System.currentTimeMillis());
        newResource.setMetadata(meta);

        getCatalogue().updateResourceEntry(newResource);
//        LogicalData relodedResource = (LogicalData) getCatalogue().getResourceEntryByLDRI(newResource.getLDRI());
        return new WebDataFileResource(getCatalogue(), newResource);
    }

    private Resource asyncCreateNonExistingFile(Path newPath, Long length, String contentType, InputStream inputStream) throws CatalogueException, IOException, InterruptedException, Exception {
        LogicalData newResource = new LogicalData(newPath, Constants.LOGICAL_FILE);
        Collection<IStorageSite> sites = getStorageSites();
        newResource.setStorageSites(sites);

        Worker w1 = new Worker(Worker.CREATE_PHYSICAL_FILE);
        w1.setLogicalData(newResource);
        w1.setInputStream(inputStream);
        Thread t1 = new Thread(w1);
        t1.start();


        Worker w2 = new Worker(Worker.REGISTER_DATA);
        w2.setLogicalData(newResource);
        w2.setContentType(contentType);
        w2.setLength(length);
        w2.setCatalog(getCatalogue());
        Thread t2 = new Thread(w2);
        t2.start();


        t1.join();
        if (w1.getException() != null) {
            throw w1.getException();
        }
        t2.join();
        if (w2.getException() != null) {
            throw w2.getException();
        }
        return new WebDataFileResource(getCatalogue(), newResource);
    }

    private Resource syncCreateNonExistingFile(Path newPath, Long length, String contentType, InputStream inputStream) throws CatalogueException, IOException, Exception {
        LogicalData newResource = new LogicalData(newPath, Constants.LOGICAL_FILE);
        Collection<IStorageSite> sites = getStorageSites();
        newResource.setStorageSites(sites);

        VFSNode node = null;
        try {
            node = newResource.getVFSNode();
        } catch (Exception ex) {
            if (!(ex instanceof ResourceNotFoundException)) {
                throw (ResourceNotFoundException) ex;
            }
        }
        if (node == null) {
            node = newResource.createPhysicalData();
        }
        if (node != null) {
            OutputStream out = ((VFile) node).getOutputStream();
            IOUtils.copy(inputStream, out);
            if (inputStream != null) {
                inputStream.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        Metadata meta = new Metadata();
        meta.setLength(length);
        meta.addContentType(contentType);
        meta.setCreateDate(System.currentTimeMillis());
        meta.setModifiedDate(System.currentTimeMillis());
        newResource.setMetadata(meta);
        getCatalogue().registerResourceEntry(newResource);

        return new WebDataFileResource(getCatalogue(), newResource);
    }
}
