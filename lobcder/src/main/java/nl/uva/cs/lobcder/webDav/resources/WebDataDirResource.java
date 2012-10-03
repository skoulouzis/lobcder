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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.authdb.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.catalogue.ResourceExistsException;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.vlet.exception.VlException;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataDirResource extends WebDataResource implements FolderResource, CollectionResource, DeletableCollectionResource {

    private boolean debug = true;

    public WebDataDirResource(JDBCatalogue catalogue, ILogicalData entry) throws IOException, Exception {
        super(catalogue, entry);
        if (!getLogicalData().getType().equals(Constants.LOGICAL_FOLDER)) {
            throw new Exception("The logical data has the wonrg type: " + getLogicalData().getType());
        }
        debug("Init. entry: " + getLogicalData().getLDRI());
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        debug("createCollection.");
        Connection connection = null;
        try {
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            Permissions perm = getCatalogue().getPermissions(getLogicalData().getUID(), getLogicalData().getOwner(), connection);
            if (!getPrincipal().canWrite(perm)) {
                throw new NotAuthorizedException();
            }
            Path newCollectionPath = Path.path(getLogicalData().getLDRI(), newName);
            debug("\t newCollectionPath: " + newCollectionPath);
            ILogicalData newFolderEntry = getCatalogue().getResourceEntryByLDRI(newCollectionPath, connection);
            if (newFolderEntry == null) { // collection does not exists, create a new one
                newFolderEntry = new LogicalData(newCollectionPath, Constants.LOGICAL_FOLDER, getCatalogue());
                newFolderEntry.setCreateDate(System.currentTimeMillis());
                newFolderEntry.setModifiedDate(System.currentTimeMillis());
                newFolderEntry.setOwner(getPrincipal().getUserId());
                WebDataDirResource res = new WebDataDirResource(getCatalogue(), newFolderEntry);
                newFolderEntry = getCatalogue().registerOrUpdateResourceEntry(newFolderEntry, connection);
                getCatalogue().setPermissions(newFolderEntry.getUID(), new Permissions(getPrincipal()), connection);
                connection.commit();
                connection.close();
                return res;
            } else {
                throw new ConflictException(this, newName);
            }
        } catch (Exception ex) {
            if (ex instanceof ConflictException) {
                throw (ConflictException) ex;
            } else {
                debug(ex.getMessage());
                throw new BadRequestException(this, ex.getMessage());
            }
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException {
        debug("child.");
        Connection connection = null;
        try {
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            Permissions perm = getCatalogue().getPermissions(getLogicalData().getUID(), getLogicalData().getOwner(), connection);
            if (!getPrincipal().canRead(perm)) {
                throw new NotAuthorizedException();
            }
            ILogicalData child = getCatalogue().getResourceEntryByLDRI(getLogicalData().getLDRI().child(childName), connection);
            connection.commit();
            connection.close();
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
            return null;
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException {
        debug(getLogicalData().getLDRI().toPath() + " collection: getChildren.");
        Connection connection = null;
        try {
            List<WebDataResource> children = new LinkedList<WebDataResource>();
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            Permissions perm = getCatalogue().getPermissions(getLogicalData().getUID(), getLogicalData().getOwner(), connection);
            if (!getPrincipal().canRead(perm)) {
                throw new NotAuthorizedException();
            }
            Collection<ILogicalData> childrenLD = getCatalogue().getChildren(getLogicalData().getLDRI().toPath(), connection);
            connection.commit();
            connection.close();
            if (childrenLD != null) {
                for (ILogicalData childLD : childrenLD) {
                    if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                        children.add(new WebDataDirResource(getCatalogue(), childLD));
                    }
                    if (childLD.getType().equals(Constants.LOGICAL_FILE)) {
                        children.add(new WebDataFileResource(getCatalogue(), childLD));
                    }
                    if (childLD.getType().equals(Constants.LOGICAL_DATA)) {
                        children.add(new WebDataResource(getCatalogue(), childLD));
                    }
                }
            }
            return children;
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        debug("createNew.");
        debug("\t newName: " + newName);
        debug("\t length: " + length);
        debug("\t contentType: " + contentType);
        Connection connection = null;
        try {
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            Path newPath = Path.path(getLogicalData().getLDRI(), newName);
            ILogicalData newResource = getCatalogue().getResourceEntryByLDRI(newPath, connection);
            if (newResource != null) { // Resource exists, update
                Permissions p = getCatalogue().getPermissions(newResource.getUID(), newResource.getOwner(), connection);
//                if (!getPrincipal().canWrite(p)) {
//                    throw new NotAuthorizedException();
//                }
                newResource.setLength(length);
                newResource.setModifiedDate(System.currentTimeMillis());
                newResource.addContentType(contentType);
                getCatalogue().registerOrUpdateResourceEntry(newResource, connection);
                PDRI pdri = createPDRI(length);
                pdri.putData(inputStream);
                newResource = getCatalogue().registerPdriForNewEntry(newResource, pdri, connection);
            } else { // Resource does not exists, create a new one
                // new need write prmissions for current collection
                Permissions p = getCatalogue().getPermissions(getLogicalData().getUID(), getLogicalData().getOwner(), connection);
                if (!getPrincipal().canWrite(p)) {
                    throw new NotAuthorizedException();
                }
                newResource = new LogicalData(newPath, Constants.LOGICAL_FILE, getCatalogue());
                newResource.setLength(length);
                newResource.setCreateDate(System.currentTimeMillis());
                newResource.setModifiedDate(System.currentTimeMillis());
                newResource.addContentType(contentType);
                //Spiros: add the owner 
                newResource.setOwner(getPrincipal().getUserId());
                getCatalogue().registerOrUpdateResourceEntry(newResource, connection);
                getCatalogue().setPermissions(newResource.getUID(), new Permissions(getPrincipal()), connection);
                PDRI pdri = createPDRI(length);
                pdri.putData(inputStream);
                getCatalogue().registerPdriForNewEntry(newResource, pdri, connection);
            }
            connection.commit();
            connection.close();
            return new WebDataFileResource(getCatalogue(), newResource);
        } catch (NotAuthorizedException e) {
            debug("NotAuthorizedException");
            throw e;
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new BadRequestException(this, ex.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        throw new ConflictException("Not implemented");
        /*
         * try {
         *
         * WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
         * debug("copyTo."); debug("\t toCollection: " +
         * toWDDR.getLogicalData().getLDRI().toPath()); debug("\t name: " +
         * name); isReadable(); toWDDR.isWritable(); Permissions p = new
         * Permissions(getPrincipal());
         * getCatalogue().copyEntry(getLogicalData().getUID(), p.getRolesPerm(),
         * toWDDR, name); // } catch (NotAuthorizedException ex) { // throw ex;
         * // } catch (BadRequestException ex) { // throw ex; } catch (Exception
         * ex) { ex.printStackTrace(); if (ex.getMessage().contains("resource
         * exists")) { throw new ConflictException(this, ex.getMessage()); }
         * Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE,
         * null, ex); }
         */
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Connection connection = null;
        try {
            debug(getLogicalData().getLDRI().toPath() + " folder delete.");
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            Path parentPath = getLogicalData().getLDRI().getParent();
            ILogicalData parentLD = getCatalogue().getResourceEntryByLDRI(parentPath, connection);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions p = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), connection);
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException();
            }
            getCatalogue().removeResourceEntry(getLogicalData(), getPrincipal(), connection);
            connection.commit();
            connection.close();
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            }
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
//        StringTemplateGroup t = new StringTemplateGroup("page");
//        StringTemplate template = t.getInstanceOf("page");
//         template.setAttribute("path", getPath().toString());
//        t.setAttribute("static", _staticContentPath);
//        t.setAttribute("subject", new SubjectWrapper(getSubject()));
//        t.setAttribute("base", UrlPathWrapper.forEmptyPath());
//         template.write(new AutoIndentWriter(new OutputStreamWriter(out, "UTF-8")));

        debug("sendContent. " +  contentType);
        Connection connection = null;
        try {
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            Permissions perm = getCatalogue().getPermissions(getLogicalData().getUID(), getLogicalData().getOwner(), connection);
            if (!getPrincipal().canRead(perm)) {
                throw new NotAuthorizedException();
            }
            Collection<ILogicalData> childrenLD = getCatalogue().getChildren(getLogicalData().getLDRI().toPath(), connection);
            connection.commit();
            connection.close();
            for (ILogicalData ld : childrenLD) {               
                if(!ld.getName().isEmpty()) {                
                    String s = ld.getName() + '\n';
                    System.err.print(s);
                    out.write(s.getBytes());
                }
            }
            out.flush();
            out.close();        
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        debug("getMaxAgeSeconds.");
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);
        return "text/html";
       /* List<String> mimeTypes;
        if (accepts != null) {
            String[] acceptsTypes = accepts.split(",");
            if (getLogicalData().getContentTypes() != null) {
                mimeTypes = getLogicalData().getContentTypes();
                for (String accessType : acceptsTypes) {
                    for (String mimeType : mimeTypes) {
                        if (accessType.equals(mimeType)) {
                            System.err.println("\t\t#################Contenttype: " + mimeType);
                            return mimeType;
                        }
                    }
                }
                System.err.println("\t\t#################Contenttype: " + mimeTypes.get(0));
                return mimeTypes.get(0);
            }
        } 
        System.err.println("\t\t#################Contenttype: null");
        return null;
        * 
        */
    }

    @Override
    public Long getContentLength() {
        debug("getContentLength.");
        return null;//getLogicalData().getLength();
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        debug("moveTo: collection " + getLogicalData().getLDRI().toPath());
        WebDataDirResource rdst = (WebDataDirResource) rDest;
        debug("\t rDestgetName: " + rdst.getLogicalData().getLDRI().toPath() + " name: " + name);
        Connection connection = null;
        try {
            Path parentPath = getLogicalData().getLDRI().getParent();
            if (parentPath == null) {
                throw new NotAuthorizedException();
            }
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            ILogicalData parentLD = getCatalogue().getResourceEntryByLDRI(getLogicalData().getLDRI().getParent(), connection);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions p = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), connection);
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException();
            }
            p = getCatalogue().getPermissions(rdst.getLogicalData().getUID(), rdst.getLogicalData().getOwner(), connection);
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException();
            }
            getCatalogue().moveEntry(getLogicalData(), rdst.getLogicalData(), name, connection);
            connection.commit();
            connection.close();
        } catch (ResourceExistsException ex) {
            throw new ConflictException(rDest, ex.getMessage());
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        debug("\t entry.getCreateDate(): " + getLogicalData().getCreateDate());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + getLogicalData().getLDRI() + ": " + msg);
        }
    }

    @Override
    public boolean isLockedOutRecursive(Request rqst) {
        return false;
    }
}
