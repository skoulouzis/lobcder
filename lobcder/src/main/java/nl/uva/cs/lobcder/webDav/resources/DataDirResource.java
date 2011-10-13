/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
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
import nl.uva.cs.lobcder.catalogue.IDRCatalogue;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import nl.uva.cs.lobcder.resources.ResourceFileEntry;
import nl.uva.cs.lobcder.resources.ResourceFolderEntry;

/**
 *
 * @author S. Koulouzis
 */
class DataDirResource implements FolderResource, CollectionResource {

    private final IDataResourceEntry entry;
    private final IDRCatalogue catalogue;

    public DataDirResource(IDRCatalogue catalogue, IDataResourceEntry entry) {
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
            ResourceFolderEntry newFolderEntry = new ResourceFolderEntry(newCollectionPath);
            newFolderEntry.getMetadata().setCreateDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newFolderEntry);
            debug("\t newCollection: " + newFolderEntry.getLDRI() + " getLDRI().getName():" + newFolderEntry.getLDRI().getName());

            return new DataDirResource(catalogue, newFolderEntry);
        } catch (Exception ex) {
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if(ex.getMessage().contains("resource exists")){
                throw  new ConflictException(this, newName);
            }
        }
        return null;
    }

    @Override
    public Resource child(String childName) {
        try {
            debug("child.");
            Path childPath = Path.path(entry.getLDRI(), childName);
            IDataResourceEntry child = catalogue.getResourceEntryByLDRI(childPath);

            if (child != null) {
                return new DataDirResource(catalogue, child);
            }
        } catch (Exception ex) {
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
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
        debug("authenticate.");
        return user;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        debug("authorise.");
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
        try {
            debug("createNew.");
            debug("\t newName: " + newName);
            debug("\t length: " + length);
            debug("\t contentType: " + contentType);
            ResourceFileEntry newResource = new ResourceFileEntry(Path.path(entry.getLDRI(), newName));
            newResource.getMetadata().setLength(length);
            newResource.getMetadata().addMimeType(contentType);
            newResource.getMetadata().setCreateDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newResource);
            DataFileResource file = new DataFileResource(catalogue, newResource);
            debug("returning createNew. " + file.getName());
            return file;
        } catch (Exception ex) {
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        debug("returning createNew. null");
        return null;
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        try {
            debug("copyTo.");
            debug("\t toCollection: " + toCollection.getName());
            debug("\t name: " + name);
            Path toCollectionLDRI = Path.path(toCollection.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);
            ResourceFolderEntry newFolderEntry = new ResourceFolderEntry(newLDRI);
            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newFolderEntry);

        } catch (Exception ex) {
            if(ex.getMessage().contains("resource exists")){
                throw  new ConflictException(this, ex.getMessage());
            }
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        debug("delete.");
        try {
            catalogue.unregisterResourceEntry(entry);
        } catch (Exception ex) {
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            debug("Exception: " + ex.getMessage());
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        //Not sure what is does
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
                mimeTypes = entry.getMetadata().getMimeTypes();
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
            catalogue.renameEntry(entry.getLDRI(), Path.path(name));
        } catch (Exception ex) {
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if(ex.getMessage().contains("resource exists")){
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
        Collection<IDataResourceEntry> topEntries = catalogue.getTopLevelResourceEntries();
        ArrayList<Resource> children = new ArrayList<Resource>();
        for (IDataResourceEntry e : topEntries) {
            if (e instanceof ResourceFolderEntry) {
                children.add(new DataDirResource(catalogue, e));
            } else if (e instanceof ResourceFileEntry) {
                children.add(new DataFileResource(catalogue, e));
            } else {
                children.add(new DataResource(catalogue, e));
            }
        }
        return children;
    }

    private ArrayList<? extends Resource> getEntriesChildren() throws Exception {
        ArrayList<Path> childrenPaths = entry.getChildren();
        ArrayList<Resource> children = new ArrayList<Resource>();
        if (childrenPaths != null) {
            for (Path p : childrenPaths) {
                debug("Adding children: " + p);
                IDataResourceEntry ch = catalogue.getResourceEntryByLDRI(p);
                if (ch instanceof ResourceFolderEntry) {
                    children.add(new DataDirResource(catalogue, ch));
                } else if (ch instanceof ResourceFileEntry) {
                    children.add(new DataFileResource(catalogue, ch));
                } else {
                    children.add(new DataResource(catalogue, ch));
                }
            }
        }

        return children;
    }
}
