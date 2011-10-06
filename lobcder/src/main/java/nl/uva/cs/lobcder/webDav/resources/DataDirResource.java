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
import nl.uva.cs.lobcder.resources.DataResourceEntry;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import nl.uva.cs.lobcder.resources.ResourceFileEntry;
import nl.uva.cs.lobcder.resources.ResourceFolderEntry;

/**
 *
 * @author S. Koulouzis
 */
class DataDirResource implements FolderResource {

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
            ResourceFolderEntry newCollection = new ResourceFolderEntry(newCollectionPath);
            catalogue.registerResourceEntry(newCollection);
            debug("\t newCollection: " + newCollection.getLDRI() + " getLDRI().getName():" + newCollection.getLDRI().getName());

            return new DataDirResource(catalogue, newCollection);
        } catch (Exception ex) {
            Logger.getLogger(DataDirResource.class.getName()).log(Level.SEVERE, null, ex);
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
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        return null;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        debug("createNew.");
        return null;
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        debug("copyTo.");
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        debug("delete.");
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        debug("sendContent.");
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        debug("getMaxAgeSeconds.");
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType.");
        return null;
    }

    @Override
    public Long getContentLength() {
        debug("getContentLength.");
        return null;
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        debug("moveTo.");
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
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
        return children;
    }
}
