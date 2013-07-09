/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.LockInfo;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.LockedException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.FolderResource;
import io.milton.resource.LockingCollectionResource;
import io.milton.resource.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.SpeedLogger;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WebDataDirResource extends WebDataResource implements FolderResource,
        CollectionResource, DeletableCollectionResource, LockingCollectionResource {

    public WebDataDirResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull List<AuthI> authList) {
        super(logicalData, path, catalogue, authList);
        WebDataDirResource.log.fine("Init. WebDataDirResource:  " + getPath());
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (auth == null) {
            return false;
        }
        try {
            switch (method) {
                case MKCOL:
                    String msg = "From: " + fromAddress + " User: " + getPrincipal().getUserId() + " Method: " + method;
                    WebDataDirResource.log.log(Level.INFO, msg);
                    return getPrincipal().canWrite(getPermissions());
                default:
                    return super.authorise(request, method, auth);
            }
        } catch (Throwable th) {
            WebDataDirResource.log.log(Level.FINER, "Exception in authorize for a resource " + getPath(), th);
            return false;
        }
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        WebDataDirResource.log.log(Level.FINE, "createCollection {0} in {1}", new Object[]{newName, getPath()});
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
                    WebDataDirResource res = new WebDataDirResource(newFolderEntry, newCollectionPath, getCatalogue(), authList);
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
                        return new WebDataDirResource(childLD, Path.path(getPath(), childName), getCatalogue(), authList);
                    } else {
                        return new WebDataFileResource(childLD, Path.path(getPath(), childName), getCatalogue(), authList);
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
            List<Resource> children = new ArrayList<>();
            Collection<LogicalData> childrenLD = getCatalogue().getChildrenByParentRef(getLogicalData().getUid());
            if (childrenLD != null) {
                for (LogicalData childLD : childrenLD) {
                    if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                        children.add(new WebDataDirResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), authList));
                    } else {
                        children.add(new WebDataFileResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), authList));
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
        WebDataDirResource.log.log(Level.FINE, "createNew. for {0}\n\t newName:\t{1}\n\t length:\t{2}\n\t contentType:\t{3}", new Object[]{getPath(), newName, length, contentType});
        LogicalData fileLogicalData;
        List<PDRIDescr> pdriDescrList;
        WebDataFileResource resource;
        PDRI pdri;
        double start = System.currentTimeMillis();
        try (Connection connection = getCatalogue().getConnection()) {
            try {
//                Long uid = getCatalogue().getLogicalDataUidByParentRefAndName(getLogicalData().getUid(), newName, connection);
                Path newPath = Path.path(getPath(), newName);
                fileLogicalData = getCatalogue().getLogicalDataByPath(newPath, connection);
                if (fileLogicalData != null) {  // Resource exists, update
//                    throw new ConflictException(this, newName);
                    Permissions p = getCatalogue().getPermissions(fileLogicalData.getUid(), fileLogicalData.getOwner(), connection);
                    if (!getPrincipal().canWrite(p)) {
                        throw new NotAuthorizedException(this);
                    }
                    fileLogicalData.setLength(length);
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.addContentType(contentType);

                    //Create new
                    pdri = createPDRI(fileLogicalData.getLength(), newName, connection);
                    pdri.putData(inputStream);

                    fileLogicalData = getCatalogue().updateLogicalDataAndPdri(fileLogicalData, pdri, connection);
                    connection.commit();
                    resource = new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
//                    return new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
                } else { // Resource does not exists, create a new one
                    // new need write prmissions for current collection
                    fileLogicalData = new LogicalData();
                    fileLogicalData.setName(newName);
                    fileLogicalData.setParentRef(getLogicalData().getUid());
                    fileLogicalData.setType(Constants.LOGICAL_FILE);
                    fileLogicalData.setOwner(getPrincipal().getUserId());
                    fileLogicalData.setLength(length);
                    fileLogicalData.setCreateDate(System.currentTimeMillis());
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.addContentType(contentType);
                    pdri = createPDRI(length, newName, connection);
                    pdri.putData(inputStream);
                    //fileLogicalData.setChecksum(pdri.getChecksum());
                    fileLogicalData = getCatalogue().associateLogicalDataAndPdri(fileLogicalData, pdri, connection);
                    getCatalogue().setPermissions(fileLogicalData.getUid(), new Permissions(getPrincipal()), connection);
                    connection.commit();
                    resource = new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
//                    return new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
                }
            } catch (NoSuchAlgorithmException ex) {
                WebDataDirResource.log.log(Level.SEVERE, null, ex);
                throw new InternalError(ex.getMessage());
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
//                throw new InternalError(e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
//            throw new InternalError(e1.getMessage());
        }
        double elapsed = System.currentTimeMillis() - start;
        double speed = ((resource.getContentLength() * 8.0) * 1000.0) / (elapsed * 1000.0);
        String msg = null;
        try {
            msg = "Source: " + fromAddress + " Destination: " + new URI(pdri.getURI()).getScheme() + "://" + pdri.getHost() + " Rx_Speed: " + speed + " Kbites/sec Rx_Size: " + (resource.getContentLength()) + " bytes";
        } catch (URISyntaxException ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        WebDataDirResource.log.log(Level.INFO, msg);
        SpeedLogger.logSpeed(msg);
        return resource;
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
        WebDataDirResource.log.log(Level.FINE, "delete() for {0}", getPath());
        if (getPath().isRoot()) {
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
            ps.println("<HTML>\n"
                    + "\n"
                    + "<HEAD>\n"
                    + "<TITLE>" + getPath() + "</TITLE>\n"
                    + "</HEAD>\n"
                    + "<BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\">");
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
            ps.println("</BODY>\n"
                    + "\n"
                    + "</HTML>");
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
        Date date = new Date(getLogicalData().getCreateDate());
        WebDataDirResource.log.log(Level.FINE, "getCreateDate() for {0} date: " + date, getPath());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public boolean isLockedOutRecursive(Request rqst) {
        return false;
    }

    /**
     * This means to just lock the name Not to create the resource.
     *
     * @param name
     * @param lt
     * @param li
     * @return
     * @throws NotAuthorizedException
     */
    @Override
    public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException {
        try (Connection connection = getCatalogue().getConnection()) {
            Path newPath = Path.path(getPath(), name);
            //If the resource exists 
            LogicalData fileLogicalData = getCatalogue().getLogicalDataByPath(newPath, connection);
            if (fileLogicalData != null) {
                throw new PreConditionFailedException(new WebDataFileResource(fileLogicalData, Path.path(getPath(), name), getCatalogue(), authList));
            } 
            LockToken lockToken = new LockToken(UUID.randomUUID().toString(), lockInfo, timeout);
            return lockToken;

        } catch (SQLException | PreConditionFailedException ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if(ex instanceof PreConditionFailedException){
                throw new RuntimeException(ex);
            }
        }
        return null;
    }
}
