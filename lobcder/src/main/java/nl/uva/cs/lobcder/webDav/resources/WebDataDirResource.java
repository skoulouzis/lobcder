/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.FolderResource;
import io.milton.resource.Resource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.util.Constants;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author S. Koulouzis
 */

@Log
public class WebDataDirResource extends WebDataResource implements FolderResource, CollectionResource, DeletableCollectionResource {

    public WebDataDirResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull AuthI auth1,  AuthI auth2) {
        super(logicalData, path, catalogue, auth1, auth2);
        WebDataDirResource.log.fine("Init. WebDataDirResource:  " + getPath());
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        try {
            switch (method) {
                case MKCOL:
                    return getPrincipal().canWrite(getPermissions());
                default:
                    return super.authorise(request, method, auth);
            }
        } catch (Throwable th) {
            WebDataDirResource.log.log(Level.SEVERE, "Exception in authorize for a resource " + getPath(), th);
            return false;
        }
    }


    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        WebDataDirResource.log.fine("createCollection " + newName + " in " + getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Path newCollectionPath = Path.path(getPath(), newName);
                Long newFolderEntryId = getCatalogue().getLogicalDataUidByParentRefAndName(getLogicalData().getUid(), newName, connection);
                if (newFolderEntryId != null) {
                    throw new ConflictException(this, newName);
                } else {// collection does not exists, create a new one
                    LogicalData newFolderEntry = new LogicalData(); //newCollectionPath, Constants.LOGICAL_FOLDER,
                    newFolderEntry.setType(Constants.LOGICAL_FOLDER);
                    newFolderEntry.setParentRef(getLogicalData().getUid());
                    newFolderEntry.setName(newName);
                    newFolderEntry.setCreateDate(System.currentTimeMillis());
                    newFolderEntry.setModifiedDate(System.currentTimeMillis());
                    newFolderEntry.setOwner(getPrincipal().getUserId());
                    WebDataDirResource res = new WebDataDirResource(newFolderEntry, newCollectionPath, getCatalogue(), auth1, auth2);
                    getCatalogue().setPermissions(
                            getCatalogue().registerDirLogicalData(newFolderEntry, connection).getUid(),
                            new Permissions(getPrincipal()), connection);
                    connection.commit();
                    return res;
                }
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, 1);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException {
        WebDataDirResource.log.fine("child(" + childName + ") for " + getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                LogicalData childLD = getCatalogue().getLogicalDataByParentRefAndName(getLogicalData().getUid(), childName, connection);
                connection.commit();
                if (childLD != null) {
                    if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                        return new WebDataDirResource(childLD, Path.path(getPath(), childName), getCatalogue(), auth1, auth2);
                    } else {
                        return new WebDataFileResource(childLD, Path.path(getPath(), childName), getCatalogue(), auth1, auth2);
                    }
                } else {
                    return null;
                }
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                connection.rollback();
                return null;
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            return null;
        }
    }


    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException {
        WebDataDirResource.log.fine("getChildren() for " + getPath());
        try {
            List<WebDataResource> children = new ArrayList<>();
            Collection<LogicalData> childrenLD = getCatalogue().getChildrenByParentRef(getLogicalData().getUid());
            if (childrenLD != null) {
                for (LogicalData childLD : childrenLD) {
                    if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                        children.add(new WebDataDirResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), auth1, auth2));
                    } else {
                        children.add(new WebDataFileResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), auth1, auth2));
                    }
                }
            }
            return children;
        } catch (Exception e) {
            WebDataDirResource.log.log(Level.SEVERE, null, e);
            return null;
        }
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException,
            ConflictException, NotAuthorizedException, BadRequestException {
        WebDataDirResource.log.fine("createNew. for " + getPath() + "\n\t newName:\t" + newName + "\n\t length:\t" + length + "\n\t contentType:\t" + contentType);
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Long uid = getCatalogue().getLogicalDataUidByParentRefAndName(getLogicalData().getUid(), newName, connection);
                if (uid != null) { // Resource exists, conflict
                    throw new ConflictException(this, newName);
                } else { // Resource does not exists, create a new one
                    // new need write prmissions for current collection
                    LogicalData fileLogicalData = new LogicalData();
                    fileLogicalData.setName(newName);
                    fileLogicalData.setParentRef(getLogicalData().getUid());
                    fileLogicalData.setType(Constants.LOGICAL_FILE);
                    fileLogicalData.setOwner(getPrincipal().getUserId());
                    fileLogicalData.setLength(length);
                    fileLogicalData.setCreateDate(System.currentTimeMillis());
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.addContentType(contentType);
                    PDRI pdri = createPDRI(length, newName);
                    pdri.putData(inputStream);
                    //fileLogicalData.setChecksum(pdri.getChecksum());
                    fileLogicalData = getCatalogue().associateLogicalDataAndPdri(fileLogicalData, pdri, connection);
                    getCatalogue().setPermissions(fileLogicalData.getUid(), new Permissions(getPrincipal()), connection);
                    connection.commit();
                    return new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), auth1, auth2);
                }
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
        WebDataDirResource.log.fine("copyTo(" + toWDDR.getPath() + ", '" + name + "') for " + getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions newParentPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                if (!getPrincipal().canWrite(newParentPerm)) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().copyFolder(getLogicalData(), toWDDR.getLogicalData(), name, getPrincipal(), connection);
                connection.commit();
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        WebDataDirResource.log.fine("delete() for " + getPath());
        if(getPath().isRoot()){
            throw new ConflictException(this, "Cannot delete root");
        }
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                getCatalogue().remove(getLogicalData(), getPrincipal(), connection);
                connection.commit();
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        WebDataDirResource.log.fine("sendContent(" + contentType + ") for " + getPath());
        try (PrintStream ps = new PrintStream(out)) {
            ps.println("<HTML>\n" +
                    "\n" +
                    "<HEAD>\n" +
                    "<TITLE>" + getPath() + "</TITLE>\n" +
                    "</HEAD>\n" +
                    "<BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\">");
            ps.println("<dl>");
            for (LogicalData ld : getCatalogue().getChildrenByParentRef(getLogicalData().getUid())) {
                if (ld.isFolder()) {
                    if (ld.getUid() != 1) {
                        ps.println("<dt><a href=\"../" + getPath() + "/" + ld.getName() + "\">" + ld.getName() + "</a></dt>");
                    } else {
                        ps.println("<dt>ROOT</dt>");
                    }
                } else {
                    ps.println("<dd><a href=\"../" + getPath() + "/" + ld.getName() + "\">" + ld.getName() + "</a></dd>");
                }
            }
            ps.println("</dl>");
            ps.println("</BODY>\n" +
                    "\n" +
                    "</HTML>");
        } catch (SQLException e) {
            WebDataDirResource.log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this);
        }

    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        WebDataDirResource.log.fine("getMaxAgeSeconds() for " + getPath());
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        WebDataDirResource.log.fine("getContentType(" + accepts + ") for " + getPath());
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        WebDataDirResource.log.fine("getContentLength() for " + getPath());
        return null;
    }

    @Override
    public void moveTo(CollectionResource toCollection, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
        WebDataDirResource.log.fine("moveTo(" + toWDDR.getPath() + ", '" + name + "') for " + getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions newParentPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                if (!getPrincipal().canWrite(newParentPerm)) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().moveEntry(getLogicalData(), toWDDR.getLogicalData(), name, connection);
                connection.commit();
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public Date getCreateDate() {
        WebDataDirResource.log.fine("getCreateDate() for " + getPath());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public boolean isLockedOutRecursive(Request rqst) {
        return false;
    }
}
