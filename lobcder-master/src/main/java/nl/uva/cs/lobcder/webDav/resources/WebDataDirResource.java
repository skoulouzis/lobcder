/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.*;
import static io.milton.http.Request.Method.MKCOL;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.cs.lobcder.util.Constants;
import org.apache.commons.io.FilenameUtils;
import static org.rendersnake.HtmlAttributesFactory.*;
import org.rendersnake.HtmlCanvas;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WebDataDirResource extends WebDataResource implements FolderResource,
        CollectionResource, DeletableCollectionResource, LockingCollectionResource, PostableResource {

    private int attempts = 0;
    private Map<String, String> mimeTypeMap = new HashMap<>();

    public WebDataDirResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull List<AuthI> authList) {
        super(logicalData, path, catalogue, authList);
        WebDataDirResource.log.log(Level.FINE, "Init. WebDataDirResource:  {0}", getPath());
        mimeTypeMap.put("mp4", "video/mp4");
        mimeTypeMap.put("pdf", "application/pdf");
        mimeTypeMap.put("tex", "application/x-tex");
        mimeTypeMap.put("log", "text/plain");
        mimeTypeMap.put("png", "image/png");
        mimeTypeMap.put("aux", "text/plain");
        mimeTypeMap.put("bbl", "text/plain");
        mimeTypeMap.put("blg", "text/plain");
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (auth == null) {
            attempts = 0;
            return false;
        }
        try {
            switch (method) {
                case MKCOL:
                    String msg = "From: " + fromAddress + " User: " + getPrincipal().getUserId() + " Method: " + method;
                    WebDataDirResource.log.log(Level.INFO, msg);
                    attempts = 0;
                    return getPrincipal().canWrite(getPermissions());
                default:
                    attempts = 0;
                    return super.authorise(request, method, auth);
            }
        } catch (Throwable th) {
            if (th instanceof java.sql.SQLException && attempts <= Constants.RECONNECT_NTRY) {
                attempts++;
                authorise(request, method, auth);

            } else {
                WebDataDirResource.log.log(Level.FINER, "Exception in authorize for a resource " + getPath(), th);
                attempts = 0;
                return false;
            }

        }
        return false;
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
                    newFolderEntry.setLastAccessDate(System.currentTimeMillis());
                    newFolderEntry.setTtlSec(getLogicalData().getTtlSec());
                    newFolderEntry.setOwner(getPrincipal().getUserId());
                    WebDataDirResource res = new WebDataDirResource(newFolderEntry, newCollectionPath, getCatalogue(), authList);
                    getCatalogue().setPermissions(
                            getCatalogue().registerDirLogicalData(newFolderEntry, connection).getUid(),
                            new Permissions(getPrincipal(), getPermissions()), connection);
                    connection.commit();
                    return res;
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
    public Resource child(String childName) throws NotAuthorizedException {
        WebDataDirResource.log.log(Level.FINE, "child({0}) for {1}", new Object[]{childName, getPath()});
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
        WebDataDirResource.log.log(Level.FINE, "getChildren() for {0}", getPath());
        try {
            try (Connection connection = getCatalogue().getConnection()) {
                try {
                    List<Resource> children = new ArrayList<>();
                    Collection<LogicalData> childrenLD = getCatalogue().getChildrenByParentRef(getLogicalData().getUid(), connection);
                    if (childrenLD != null) {
                        for (LogicalData childLD : childrenLD) {
                            if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                                children.add(new WebDataDirResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), authList));
                            } else {
                                children.add(new WebDataFileResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), authList));
                            }
                        }
                    }
                    getCatalogue().addViewForRes(getLogicalData().getUid(), connection);
                    connection.commit();
                    return children;
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }
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
//        List<PDRIDescr> pdriDescrList;
        WebDataFileResource resource;
        PDRI pdri;
        double start = System.currentTimeMillis();
        try (Connection connection = getCatalogue().getConnection()) {
            try {
//                Long uid = getCatalogue().getLogicalDataUidByParentRefAndName(getLogicalData().getUid(), newName, connection);
                Path newPath = Path.path(getPath(), newName);
                fileLogicalData = getCatalogue().getLogicalDataByPath(newPath, connection);
                if (contentType == null || contentType.equals("application/octet-stream")) {
                    contentType = mimeTypeMap.get(FilenameUtils.getExtension(newName));
                }
                if (fileLogicalData != null) {  // Resource exists, update
//                    throw new ConflictException(this, newName);
                    Permissions p = getCatalogue().getPermissions(fileLogicalData.getUid(), fileLogicalData.getOwner(), connection);
                    if (!getPrincipal().canWrite(p)) {
                        throw new NotAuthorizedException(this);
                    }
                    fileLogicalData.setLength(length);
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.setLastAccessDate(fileLogicalData.getModifiedDate());
                    if (contentType == null) {
                        contentType = mimeTypeMap.get(FilenameUtils.getExtension(newName));
                    }
                    fileLogicalData.addContentType(contentType);

                    //Create new
                    pdri = createPDRI(fileLogicalData.getLength(), newName, connection);
                    pdri.setLength(length);
                    pdri.putData(inputStream);
                    fileLogicalData = getCatalogue().updateLogicalDataAndPdri(fileLogicalData, pdri, connection);
                    connection.commit();
//                    String md5 = pdri.getStringChecksum();
//                    if (md5 != null) {
//                        fileLogicalData.setChecksum(md5);
//                    }
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
                    fileLogicalData.setLastAccessDate(System.currentTimeMillis());
                    fileLogicalData.setTtlSec(getLogicalData().getTtlSec());
                    fileLogicalData.addContentType(contentType);
                    fileLogicalData = inheritProperties(fileLogicalData, connection);
                    pdri = createPDRI(length, newName, connection);
                    pdri.setLength(length);
                    pdri.putData(inputStream);
//                    String md5 = pdri.getStringChecksum();
//                    if (md5 != null) {
//                        fileLogicalData.setChecksum(md5);
//                    }
                    fileLogicalData = getCatalogue().associateLogicalDataAndPdri(fileLogicalData, pdri, connection);
                    getCatalogue().setPermissions(fileLogicalData.getUid(), new Permissions(getPrincipal(), getPermissions()), connection);
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
            Stats stats = new Stats();
            stats.setSource(fromAddress);
            stats.setDestination(pdri.getHost());
            stats.setSpeed(speed);
            stats.setSize(resource.getContentLength());
            getCatalogue().setSpeed(stats);
            msg = "Source: " + fromAddress + " Destination: " + new URI(pdri.getURI()).getScheme() + "://" + pdri.getHost() + " Rx_Speed: " + speed + " Kbites/sec Rx_Size: " + (resource.getContentLength()) + " bytes";
        } catch (URISyntaxException | SQLException ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        WebDataDirResource.log.log(Level.INFO, msg);
        return resource;
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
        WebDataDirResource.log.log(Level.FINE, "copyTo({0}, ''{1}'') for {2}", new Object[]{toWDDR.getPath(), name, getPath()});
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
        WebDataDirResource.log.log(Level.FINE, "sendContent({0}) for {1}", new Object[]{contentType, getPath()});

        try (Connection connection = getCatalogue().getConnection()) {
            try (PrintStream ps = new PrintStream(out)) {
                HtmlCanvas html = new HtmlCanvas();
                html
                        //                    .a(href("otherpage.html")).content("Other Page")
                        .table(border("1"))
                        .tr()
                        .th().content("Name")
                        .th().content("Size")
                        .th().content("Modification Date")
                        .th().content("Creation Date")
                        .th().content("Owner")
                        .th().content("Content Type")
                        .th().content("Type")
                        .th().content("Is Supervised")
                        .th().content("Uid");


//            html.a(href("#top").class_("toplink")).content("top");
//            ps.println(html.toHtml());

//            ps.println("<HTML>\n"
//                    + "\n"
//                    + "<HEAD>\n"
//                    + "<TITLE>" + getPath() + "</TITLE>\n"
//                    + "</HEAD>\n"
//                    + "<BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\">");
//            ps.println("<dl>");
                String ref;
//            Path first = getPath().getStripFirst();
//            if(!first.equals(Path.path("/dav"))){
//                ref= "../dav" + getPath();
//            }else{
//                ref= "../" + getPath();
//            }
//            html._tr()
//                    .tr()
//                    .td()
//                    .a(href(ref))
//                    .img(src("").alt("../"))
//                    ._a()
//                    ._td();
                for (LogicalData ld : getCatalogue().getChildrenByParentRef(getLogicalData().getUid(), connection)) {
                    if (ld.isFolder()) {
                        ref = "../dav" + getPath() + "/" + ld.getName();
                        if (ld.getUid() != 1) {
//                        ps.println("<dt>\t<a href=\"../dav" + getPath() + "/" + ld.getName() + "\">" + ld.getName() + "</a><a>\t" + ld.getLength() + "</a></dt>");
                        } else {
//                        html._tr()
//                                .tr()
//                                .td().content("/")
//                                .td().content(String.valueOf(getChildren().size()))
//                                .td().content(new Date(ld.getModifiedDate()).toString());
//                        html._tr()
//                                .tr()
//                                .td()
//                                .a(href("../dav" + getPath() + "/" + ld.getName()))
//                                .img(src("").alt("/"))
//                                ._a()
//                                ._td()
//                                .td().content(String.valueOf(getChildren().size()))
//                                .td().content(new Date(ld.getModifiedDate()).toString());
                        }
                    } else {
                        ref = "../dav" + getPath() + "/" + ld.getName();
//                    ps.println("<dd>\t<a href=\"../dav" + getPath() + "/" + ld.getName() + "\">" + ld.getName() + "</a>\t" + ld.getLength() + "</a></dd>");
                    }
                    html._tr()
                            .tr()
                            .td()
                            //                        .a(href("../dav" + getPath() + "/" + ld.getName()))
                            .a(href(ref))
                            .img(src("").alt(ld.getName()))
                            ._a()
                            ._td()
                            .td().content(String.valueOf(ld.getLength()))
                            .td().content(new Date(ld.getModifiedDate()).toString())
                            .td().content(new Date(ld.getCreateDate()).toString())
                            .td().content(ld.getOwner())
                            .td().content(ld.getContentTypesAsString())
                            .td().content(ld.getType())
                            .td().content(ld.getSupervised().toString())
                            .td().content(ld.getUid().toString());
                }
//            ps.println("</dl>");
//            ps.println("</BODY>\n"
//                    + "\n"
//                    + "</HTML>");
                html._tr()
                        ._table();


//            html.
//                    form()
//                    .input();


                ps.println(html.toHtml());
                getCatalogue().addViewForRes(getLogicalData().getUid(), connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            WebDataDirResource.log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this);
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        WebDataDirResource.log.log(Level.FINE, "getMaxAgeSeconds() for {0}", getPath());
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        WebDataDirResource.log.log(Level.FINE, "getContentType({0}) for {1}", new Object[]{accepts, getPath()});
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        WebDataDirResource.log.log(Level.FINE, "getContentLength() for {0}", getPath());
        return null;
    }

    @Override
    public void moveTo(CollectionResource toCollection, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
        WebDataDirResource.log.log(Level.FINE, "moveTo({0}, ''{1}'') for {2}", new Object[]{toWDDR.getPath(), name, getPath()});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions destPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                LogicalData parentLD = getCatalogue().getLogicalDataByUid(getLogicalData().getParentRef());
                Permissions parentPerm = getCatalogue().getPermissions(parentLD.getUid(), parentLD.getOwner());
                if (!(getPrincipal().canWrite(destPerm) && getPrincipal().canWrite(parentPerm))) {
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
     * @param timeout
     * @param lockInfo
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
            if (ex instanceof PreConditionFailedException) {
                throw new RuntimeException(ex);
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException,
            ConflictException {
        //        Set<String> keys = parameters.keySet();
        //        for (String s : keys) {
        //            WebDataDirResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, parameters.get(s)});
        //        }
        Set<String> keys = files.keySet();
        for (String s : keys) {
            WebDataDirResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, files.get(s).getFieldName()});
            try {
                //         public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException,
                createNew(files.get(s).getFieldName(), files.get(s).getInputStream(), files.get(s).getSize(), files.get(s).getContentType());
            } catch (IOException ex) {
                throw new BadRequestException(this, ex.getMessage());
            }

        }
        return null;
    }

    private LogicalData inheritProperties(LogicalData fileLogicalData, Connection connection) throws SQLException {
        String value = (String) getProperty(Constants.DATA_LOC_PREF_NAME);
        if (value != null) {
            fileLogicalData.setDataLocationPreference(value);
            getCatalogue().setLocationPreference(fileLogicalData.getUid(), value, connection);
        }
        return fileLogicalData;
    }
}
