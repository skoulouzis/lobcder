/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.resources.LogicalFolder;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouzis
 */
class WebDataDirResource implements FolderResource, CollectionResource {

    private ILogicalData entry;
    private final IDLCatalogue catalogue;

    public WebDataDirResource(IDLCatalogue catalogue, ILogicalData entry) throws IOException {
        this.entry = entry;
        this.catalogue = catalogue;
        debug("Init. entry: " + entry.getLDRI());
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
            debug("createCollection.");

            Path newCollectionPath = Path.path(entry.getLDRI(), newName);
            debug("\t newCollectionPath: " + newCollectionPath);
            LogicalFolder newFolderEntry = new LogicalFolder(newCollectionPath);
            newFolderEntry.getMetadata().setCreateDate(System.currentTimeMillis());

            Collection<IStorageSite> sites = entry.getStorageSites();
            if (sites == null || sites.isEmpty()) {
                debug("\t Storage Sites for " + this.entry.getLDRI() + " are empty!");
                throw new IOException("Storage Sites for " + this.entry.getLDRI() + " are empty!");
            }

            //Maybe we have a problem with shalow copy
            //copyStorageSites.addAll(entry.getStorageSites());
            ArrayList<IStorageSite> copyStorageSites = new ArrayList<IStorageSite>();
            for (IStorageSite s : sites) {
                copyStorageSites.add(new StorageSite(s.getEndpoint(), s.getCredentials()));
            }

            newFolderEntry.setStorageSites(copyStorageSites);
//            sites = newFolderEntry.getStorageSites();
//            if (sites == null || sites.isEmpty()) {
//                debug("\t Storage Sites for " + newFolderEntry.getLDRI() + " are empty!");
//                throw new IOException("Storage Sites for " + newFolderEntry.getLDRI() + " are empty!");
//            }
            catalogue.registerResourceEntry(newFolderEntry);

            ILogicalData reloaded = catalogue.getResourceEntryByLDRI(newFolderEntry.getLDRI());
            sites = reloaded.getStorageSites();
            if (sites == null || sites.isEmpty()) {
                debug("\t Storage Sites for (reloaded)" + reloaded.getLDRI() + " are empty!");
                //Bad bad horrible patch!
                sites = entry.getStorageSites();
                copyStorageSites = new ArrayList<IStorageSite>();
                for (IStorageSite s : sites) {
                    copyStorageSites.add(new StorageSite(s.getEndpoint(), s.getCredentials()));
                }
                newFolderEntry.setStorageSites(copyStorageSites);
                catalogue.updateResourceEntry(newFolderEntry);
                reloaded = catalogue.getResourceEntryByLDRI(newFolderEntry.getLDRI());
//                throw new IOException("Storage Sites for " + reloaded.getLDRI() + " are empty!");
            }
            WebDataDirResource resource = new WebDataDirResource(catalogue, reloaded);

            reloaded = catalogue.getResourceEntryByLDRI(this.entry.getLDRI());
            this.entry = reloaded;

            return resource;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(this, newName);
            }
        }
        return null;
    }

    @Override
    public Resource child(String childName) {
        try {
            debug("child.");
            Path childPath = Path.path(entry.getLDRI(), childName);
            ILogicalData child = catalogue.getResourceEntryByLDRI(childPath);

            if (child != null) {
                return new WebDataDirResource(catalogue, child);
            }
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() {
        debug("getChildren.");
        ArrayList<? extends Resource> children = null;
        try {
            if (entry.getLDRI().isRoot()) {
                children = getTopLevelChildren();
            } else {
                children = getEntriesChildren();
            }
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        debug("Returning children: " + children);
        return children;
    }

    @Override
    public String getUniqueId() {
        debug("getUniqueId.");
        return entry.getUID();
    }

    @Override
    public String getName() {
        debug("getName.");
        return entry.getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        debug("authenticate.\n"
                + "\t user: " + user
                + "\t password: " + password);
        return user;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        debug("authorise. \n"
                + "\t request.getAbsolutePath(): " + request.getAbsolutePath() + "\n"
                + "\t request.getAbsoluteUrl(): " + request.getAbsoluteUrl() + "\n"
                + "\t request.getAcceptHeader(): " + request.getAcceptHeader() + "\n"
                + "\t request.getFromAddress(): " + request.getFromAddress() + "\n"
                + "\t request.getRemoteAddr(): " + request.getRemoteAddr() + "\n"
                + "\t auth.getCnonce(): " + auth.getCnonce() + "\n"
                + "\t auth.getNc(): " + auth.getNc() + "\n"
                + "\t auth.getNonce(): " + auth.getNonce() + "\n"
                + "\t auth.getPassword(): " + auth.getPassword() + "\n"
                + "\t auth.getQop(): " + auth.getQop() + "\n"
                + "\t auth.getRealm(): " + auth.getRealm() + "\n"
                + "\t auth.getResponseDigest(): " + auth.getResponseDigest() + "\n"
                + "\t auth.getUri(): " + auth.getUri() + "\n"
                + "\t auth.getUser(): " + auth.getUser() + "\n"
                + "\t auth.getTag(): " + auth.getTag());
        return true;
    }

    @Override
    public String getRealm() {
        debug("getRealm.");
        return "relam";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        if (entry.getMetadata() != null && entry.getMetadata().getModifiedDate() != null) {
            return new Date(entry.getMetadata().getModifiedDate());
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        return null;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Resource resource;
        try {
            debug("createNew.");
            debug("\t newName: " + newName);
            debug("\t length: " + length);
            debug("\t contentType: " + contentType);
            Path newPath = Path.path(entry.getLDRI(), newName);

            LogicalFile newResource = (LogicalFile) catalogue.getResourceEntryByLDRI(newPath);
            if (newResource != null) {
                resource = updateExistingFile(newResource, length, contentType, inputStream);
            } else {
                resource = createNonExistingFile(newPath, length, contentType, inputStream);
            }

            ILogicalData reloaded = catalogue.getResourceEntryByLDRI(this.entry.getLDRI());
            this.entry = reloaded;
            return resource;
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
            Path toCollectionLDRI = Path.path(toCollection.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);
            LogicalFolder newFolderEntry = new LogicalFolder(newLDRI);
            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newFolderEntry);

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
            debug("delete.");
            Collection<IStorageSite> sites = entry.getStorageSites();
            if (sites != null && !sites.isEmpty()) {
                for (IStorageSite s : sites) {
                    s.deleteVNode(entry.getLDRI());
                }
            }
            List<? extends Resource> children = getChildren();
            for (Resource r : children) {
                if (r instanceof DeletableResource) {
                    ((DeletableResource) r).delete();
                }
            }
            catalogue.unregisterResourceEntry(entry);
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        //Not sure what it does
        debug("sendContent.");
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
            if (entry.getMetadata() != null) {
                mimeTypes = entry.getMetadata().getContentTypes();
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
        if (entry.getMetadata() != null) {
            return entry.getMetadata().getLength();
        }
        return null;
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        try {
            debug("moveTo.");
            debug("\t rDestgetName: " + rDest.getName() + " name: " + name);
//            if(rDest == null || rDest.getName() == null){
//                debug("----------------Will throw forbidden ");
//                throw new com.bradmcevoy.http.exceptions.BadRequestException(this);
//            }
            catalogue.renameEntry(entry.getLDRI(), Path.path(name));
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
        debug("\t entry.getMetadata(): " + entry.getMetadata());
        debug("\t entry.getMetadata().getCreateDate(): " + entry.getMetadata().getCreateDate());
        if (entry.getMetadata() != null && entry.getMetadata().getCreateDate() != null) {
            debug("getCreateDate. returning");
            return new Date(entry.getMetadata().getCreateDate());
        }
        debug("getCreateDate. returning");
        return null;
    }

    protected void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + "." + entry.getLDRI() + ": " + msg);
    }

    private ArrayList<? extends Resource> getTopLevelChildren() throws Exception {
        Collection<ILogicalData> topEntries = catalogue.getTopLevelResourceEntries();
        ArrayList<Resource> children = new ArrayList<Resource>();
        for (ILogicalData e : topEntries) {
            if (e instanceof LogicalFolder) {
                children.add(new WebDataDirResource(catalogue, e));
            } else if (e instanceof LogicalFile) {
                children.add(new WebDataFileResource(catalogue, e));
            } else {
                children.add(new WebDataResource(catalogue, e));
            }
        }
        return children;
    }

    private ArrayList<? extends Resource> getEntriesChildren() throws Exception {
        Collection<Path> childrenPaths = entry.getChildren();
//        if(childrenPaths == null){
//             entry = catalogue.getResourceEntryByLDRI(this.entry.getLDRI());
//        }
//        childrenPaths = entry.getChildren();
        ArrayList<Resource> children = new ArrayList<Resource>();
        if (childrenPaths != null) {
            for (Path p : childrenPaths) {
                debug("Adding children: " + p);
                ILogicalData ch = catalogue.getResourceEntryByLDRI(p);
                if (ch instanceof LogicalFolder) {
                    children.add(new WebDataDirResource(catalogue, ch));
                } else if (ch instanceof LogicalFile) {
                    children.add(new WebDataFileResource(catalogue, ch));
                } else {
                    children.add(new WebDataResource(catalogue, ch));
                }
            }
        }
        return children;
    }

    Path getPath() {
        return this.entry.getLDRI();
    }

    Collection<IStorageSite> getStorageSites() {
        return this.entry.getStorageSites();
    }

    private Resource createNonExistingFile(Path newPath, Long length, String contentType, InputStream inputStream) throws IOException, Exception {
        LogicalFile newResource = new LogicalFile(newPath);
        //We have to make a copy of the member collection. The same collection 
        //can't be a member of the two different classes, the relationship is 1-N!!!
        ArrayList<IStorageSite> copyStorageSites = new ArrayList<IStorageSite>();
        Collection<IStorageSite> sites = entry.getStorageSites();
//        if (sites == null || sites.isEmpty()) {
//            ILogicalData reloaded = this.catalogue.getResourceEntryByLDRI(entry.getLDRI());
//            sites = reloaded.getStorageSites();
//        }
        if (sites == null || sites.isEmpty()) {
            debug("\t Storage Sites for " + this.entry.getLDRI() + " are empty!");
            throw new IOException("Storage Sites for " + this.entry.getLDRI() + " are empty!");
        }
        //Maybe we have a problem with shalow copy
        //copyStorageSites.addAll(entry.getStorageSites());
        for (IStorageSite s : sites) {
            copyStorageSites.add(new StorageSite(s.getEndpoint(), s.getCredentials()));
        }
        newResource.setStorageSites(copyStorageSites);
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

        Metadata meta = new Metadata();
        meta.setLength(length);
        meta.addContentType(contentType);
        meta.setCreateDate(System.currentTimeMillis());
        newResource.setMetadata(meta);

        catalogue.registerResourceEntry(newResource);
        LogicalFile relodedResource = (LogicalFile) catalogue.getResourceEntryByLDRI(newResource.getLDRI());

        return new WebDataFileResource(catalogue, relodedResource);
    }

    private Resource updateExistingFile(LogicalFile newResource, Long length, String contentType, InputStream inputStream) throws VlException, IOException, Exception {
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

        catalogue.updateResourceEntry(newResource);
        LogicalFile relodedResource = (LogicalFile) catalogue.getResourceEntryByLDRI(newResource.getLDRI());
        return new WebDataFileResource(catalogue, relodedResource);
    }
}
