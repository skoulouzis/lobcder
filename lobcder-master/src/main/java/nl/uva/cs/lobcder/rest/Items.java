/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.rest.wrappers.LogicalDataWrapped;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.GridHelper;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.annotation.Nonnull;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

/**
 * Gets resource properties like length owner physical location etc.
 *
 * @author dvasunin
 */
@Log
@Path("items/")
public class Items extends CatalogueHelper {

    private int defaultRowLimit;
    @Context
    UriInfo info;
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse servletResponse;

    public Items() throws NamingException, IOException {
        defaultRowLimit = PropertiesHelper.getDefaultRowLimit();
    }

    @Data
    @AllArgsConstructor
    class MyData {

        Long uid;
        String path;
    }

    private List<LogicalDataWrapped> queryLogicalData(MyData myData, int limit, PreparedStatement ps1, PreparedStatement ps2, MyPrincipal mp, Connection cn) throws Exception {
        List<LogicalDataWrapped> ldwl = new LinkedList<>();
        Queue<MyData> dirs = new LinkedList<>();
        dirs.offer(myData);
        MyData dir;
        while ((dir = dirs.poll()) != null) {
            ps1.setLong(1, dir.getUid());
            ps1.setInt(20, limit + 1);
            try (ResultSet resultSet = ps1.executeQuery()) {
                while (resultSet.next()) {
                    Long uid = resultSet.getLong(1);
                    String datatype = resultSet.getString(4);
                    String ldName = resultSet.getString(5);
                    String owner = resultSet.getString(3);
                    Permissions p = getCatalogue().getPermissions(uid, owner, cn);
                    if (mp.canRead(p) && uid != 1) {
                        LogicalData logicalData = new LogicalData();
                        logicalData.setUid(uid);
                        logicalData.setParentRef(dir.getUid());
                        logicalData.setOwner(owner);
                        logicalData.setType(datatype);
                        logicalData.setName(ldName);
                        logicalData.setCreateDate(resultSet.getTimestamp(6).getTime());
                        logicalData.setModifiedDate(resultSet.getTimestamp(7).getTime());
                        logicalData.setLength(resultSet.getLong(8));
                        logicalData.setContentTypesAsString(resultSet.getString(9));
                        logicalData.setPdriGroupId(resultSet.getLong(10));
                        logicalData.setSupervised(resultSet.getBoolean(11));
                        logicalData.setChecksum(resultSet.getString(12));
                        logicalData.setLastValidationDate(resultSet.getLong(13));
                        logicalData.setLockTokenID(resultSet.getString(14));
                        logicalData.setLockScope(resultSet.getString(15));
                        logicalData.setLockType(resultSet.getString(16));
                        logicalData.setLockedByUser(resultSet.getString(17));
                        logicalData.setLockDepth(resultSet.getString(18));
                        logicalData.setLockTimeout(resultSet.getLong(19));
                        logicalData.setDescription(resultSet.getString(20));
//                        logicalData.setDataLocationPreference(resultSet.getString(21));
                        logicalData.setStatus(resultSet.getString(22));

                        LogicalDataWrapped ldw = new LogicalDataWrapped();
                        ldw.setGlobalID(getCatalogue().getGlobalID(uid, cn));
                        ldw.setLogicalData(logicalData);
                        ldw.setPermissions(p);
                        ldw.setPath(dir.getPath().concat("/").concat(logicalData.getName()));
                        if (!logicalData.isFolder() && mp.isAdmin()) {
                            List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(logicalData.getPdriGroupId(), cn);
                            for (PDRIDescr pdri : pdriDescr) {
                                if (pdri.getResourceUrl().startsWith("lfc")
                                        || pdri.getResourceUrl().startsWith("srm")
                                        || pdri.getResourceUrl().startsWith("gftp")) {
                                    pdriDescr.remove(pdri);
                                    GridHelper.initGridProxy(pdri.getUsername(), pdri.getPassword(), null, false);
                                    pdri.setPassword(GridHelper.getProxyAsBase64String());
                                    pdriDescr.add(pdri);
                                }
                            }
                            ldw.setPdriList(pdriDescr);
                        }
                        ldwl.add(ldw);
                        limit--;
                    }
                    if (limit == 0) {
                        break;
                    }
                }
            }
            if (limit != 0) {
                ps2.setLong(1, dir.getUid());
                try (ResultSet resultSet = ps2.executeQuery()) {
                    while (resultSet.next()) {
                        Long myUid = resultSet.getLong(1);
                        String myOwner = resultSet.getString(2);
                        String myPath = dir.getPath().concat("/").concat(resultSet.getString(3));
                        Permissions p = getCatalogue().getPermissions(myUid, myOwner, cn);
                        if (mp.canRead(p) && myUid != 1) {
                            dirs.offer(new MyData(myUid, myPath));
                        }
                    }
                }
            } else {
                break;
            }
        }
        return ldwl;
    }

    private List<LogicalDataWrapped> queryLogicalData(@Nonnull MyPrincipal mp, @Nonnull Connection cn) throws Exception {
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        boolean addFlag = true;
        String rootPath = (queryParameters.containsKey("path") && queryParameters.get("path").iterator().hasNext())
                ? queryParameters.get("path").iterator().next() : "/";
        if (!rootPath.equals("/") && rootPath.endsWith("/")) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }

        int rowLimit;
        try {
            rowLimit = (queryParameters.containsKey("limit") && queryParameters.get("limit").iterator().hasNext())
                    ? Integer.valueOf(queryParameters.get("limit").iterator().next()).intValue() : defaultRowLimit;
        } catch (Throwable th) {
            rowLimit = defaultRowLimit;
        }
        LogicalData ld = getCatalogue().getLogicalDataByPath(io.milton.common.Path.path(rootPath), cn);
        List<LogicalDataWrapped> logicalDataWrappedList = new ArrayList<>();
        if (ld == null || rowLimit < 1) {
            return logicalDataWrappedList;
        }

        Permissions p = getCatalogue().getPermissions(ld.getUid(), ld.getOwner(), cn);
        if (mp.canRead(p)) {
            try (PreparedStatement ps1 = cn.prepareStatement("SELECT uid, parentRef, "
                    + "ownerId, datatype, ldName, createDate, modifiedDate, ldLength, "
                    + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                    + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                    + "description, locationPreference, status "
                    + "FROM ldata_table WHERE (parentRef = ?) "
                    + "AND (? OR (isSupervised = ?)) "
                    + "AND (? OR (createDate BETWEEN FROM_UNIXTIME(?) AND FROM_UNIXTIME(?))) "
                    + "AND (? OR (createDate >= FROM_UNIXTIME(?))) "
                    + "AND (? OR (createDate <= FROM_UNIXTIME(?))) "
                    + "AND (? OR (modifiedDate BETWEEN FROM_UNIXTIME(?) AND FROM_UNIXTIME(?))) "
                    + "AND (? OR (modifiedDate >= FROM_UNIXTIME(?))) "
                    + "AND (? OR (modifiedDate <= FROM_UNIXTIME(?))) "
                    + "AND (? OR (ldName LIKE CONCAT('%', ? , '%')))"
                    + "LIMIT ?");
                    PreparedStatement ps2 = cn.prepareStatement("SELECT uid, ownerId, "
                    + "ldName FROM ldata_table WHERE parentRef = ? AND datatype = '" + Constants.LOGICAL_FOLDER + "'")) {
                {
                    if (queryParameters.containsKey("name") && queryParameters.get("name").iterator().hasNext()) {
                        String name = queryParameters.get("name").iterator().next();
                        ps1.setBoolean(18, false);
                        ps1.setString(19, name);
                        addFlag &= ld.getName().contains(name);
                    } else {
                        ps1.setBoolean(18, true);
                        ps1.setString(19, "");
                    }

                    if (queryParameters.containsKey("cStartDate") && queryParameters.get("cStartDate").iterator().hasNext()
                            && queryParameters.containsKey("cEndDate") && queryParameters.get("cEndDate").iterator().hasNext()) {
                        long cStartDate = Long.valueOf(queryParameters.get("cStartDate").iterator().next());
                        long cEndDate = Long.valueOf(queryParameters.get("cEndDate").iterator().next());
                        ps1.setBoolean(4, false);
                        ps1.setBoolean(7, true);
                        ps1.setBoolean(9, true);
                        ps1.setLong(5, cStartDate);
                        ps1.setLong(6, cEndDate);
                        ps1.setLong(8, 0);
                        ps1.setLong(10, 0);
                        addFlag &= (ld.getCreateDate() >= cStartDate * 1000) && (ld.getCreateDate() <= cEndDate * 1000);
                    } else if (queryParameters.containsKey("cStartDate") && queryParameters.get("cStartDate").iterator().hasNext()) {
                        long cStartDate = Long.valueOf(queryParameters.get("cStartDate").iterator().next());
                        ps1.setBoolean(4, true);
                        ps1.setBoolean(7, false);
                        ps1.setBoolean(9, true);
                        ps1.setLong(5, 0);
                        ps1.setLong(6, 0);
                        ps1.setLong(8, cStartDate);
                        ps1.setLong(10, 0);
                        addFlag &= (ld.getCreateDate() >= cStartDate * 1000);
                    } else if (queryParameters.containsKey("cEndDate") && queryParameters.get("cEndDate").iterator().hasNext()) {
                        long cEndDate = Long.valueOf(queryParameters.get("cEndDate").iterator().next());
                        ps1.setBoolean(4, true);
                        ps1.setBoolean(7, true);
                        ps1.setBoolean(9, false);
                        ps1.setLong(5, 0);
                        ps1.setLong(6, 0);
                        ps1.setLong(8, 0);
                        ps1.setLong(10, cEndDate);
                        addFlag &= (ld.getCreateDate() <= cEndDate * 1000);
                    } else {
                        ps1.setBoolean(4, true);
                        ps1.setBoolean(7, true);
                        ps1.setBoolean(9, true);
                        ps1.setLong(5, 0);
                        ps1.setLong(6, 0);
                        ps1.setLong(8, 0);
                        ps1.setLong(10, 0);
                    }

                    if (queryParameters.containsKey("mStartDate") && queryParameters.get("mStartDate").iterator().hasNext()
                            && queryParameters.containsKey("mEndDate") && queryParameters.get("mEndDate").iterator().hasNext()) {
                        long mStartDate = Long.valueOf(queryParameters.get("mStartDate").iterator().next());
                        long mEndDate = Long.valueOf(queryParameters.get("mEndDate").iterator().next());
                        ps1.setBoolean(11, false);
                        ps1.setBoolean(14, true);
                        ps1.setBoolean(16, true);
                        ps1.setLong(12, mStartDate);
                        ps1.setLong(13, mEndDate);
                        ps1.setLong(15, 0);
                        ps1.setLong(17, 0);
                        addFlag &= (ld.getModifiedDate() >= mStartDate * 1000) && (ld.getModifiedDate() <= mEndDate * 1000);
                    } else if (queryParameters.containsKey("mStartDate") && queryParameters.get("mStartDate").iterator().hasNext()) {
                        long mStartDate = Long.valueOf(queryParameters.get("mStartDate").iterator().next());
                        ps1.setBoolean(11, true);
                        ps1.setBoolean(14, false);
                        ps1.setBoolean(16, true);
                        ps1.setLong(12, 0);
                        ps1.setLong(13, 0);
                        ps1.setLong(15, mStartDate);
                        ps1.setLong(17, 0);
                        addFlag &= (ld.getModifiedDate() >= mStartDate * 1000);
                    } else if (queryParameters.containsKey("mEndDate") && queryParameters.get("mEndDate").iterator().hasNext()) {
                        long mEndDate = Long.valueOf(queryParameters.get("mEndDate").iterator().next());
                        ps1.setBoolean(11, true);
                        ps1.setBoolean(14, true);
                        ps1.setBoolean(16, false);
                        ps1.setLong(12, 0);
                        ps1.setLong(13, 0);
                        ps1.setLong(15, 0);
                        ps1.setLong(17, mEndDate);
                        addFlag &= (ld.getModifiedDate() <= mEndDate * 1000);
                    } else {
                        ps1.setBoolean(11, true);
                        ps1.setBoolean(14, true);
                        ps1.setBoolean(16, true);
                        ps1.setLong(12, 0);
                        ps1.setLong(13, 0);
                        ps1.setLong(15, 0);
                        ps1.setLong(17, 0);
                    }

                    if (queryParameters.containsKey("isSupervised") && queryParameters.get("isSupervised").iterator().hasNext()) {
                        boolean isSupervised = Boolean.valueOf(queryParameters.get("isSupervised").iterator().next());
                        ps1.setBoolean(2, false);
                        ps1.setBoolean(3, isSupervised);
                        addFlag &= (ld.getSupervised() == isSupervised);
                    } else {
                        ps1.setBoolean(2, true);
                        ps1.setBoolean(3, true);
                    }
                    if (addFlag) {
                        LogicalDataWrapped ldw = new LogicalDataWrapped();
                        ldw.setGlobalID(getCatalogue().getGlobalID(ld.getUid(), cn));
                        ldw.setLogicalData(ld);
                        ldw.setPath(rootPath);
                        ldw.setPermissions(p);
                        List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(ld.getPdriGroupId(), cn);
                        if (mp.isAdmin()) {
                            for (PDRIDescr pdri : pdriDescr) {
                                if (pdri.getResourceUrl().startsWith("lfc")
                                        || pdri.getResourceUrl().startsWith("srm")
                                        || pdri.getResourceUrl().startsWith("gftp")) {
                                    pdriDescr.remove(pdri);
                                    GridHelper.initGridProxy(pdri.getUsername(), pdri.getPassword(), null, false);
                                    pdri.setPassword(GridHelper.getProxyAsBase64String());
                                    pdriDescr.add(pdri);
                                }
                            }
                        } else {
                            for (PDRIDescr pdri : pdriDescr) {
                                pdriDescr.remove(pdri);
                                pdri.setPassword(null);
                                pdri.setUsername(null);
                                pdri.setKey(null);
                                pdri.setId(null);
                                pdri.setPdriGroupRef(null);
                                pdri.setStorageSiteId(null);
                                pdriDescr.add(pdri);
                            }
                        }
                        ldw.setPdriList(pdriDescr);
                        logicalDataWrappedList.add(ldw);
                        rowLimit--;
                    }
                    if (rowLimit != 0) {
                        logicalDataWrappedList.addAll(queryLogicalData(new MyData(ld.getUid(), rootPath.equals("/") ? "" : rootPath), rowLimit, ps1, ps2, mp, cn));
                    }
                }
            }
        }
        return logicalDataWrappedList;
    }

    /**
     * Gets the resource's properties (length, owner, permitions etc.)
     *
     * @return the resource's properties
     * @throws Exception
     */
    @Path("query/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<LogicalDataWrapped> getXml() throws Exception {
        try (Connection cn = getCatalogue().getConnection()) {
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            List<LogicalDataWrapped> res = queryLogicalData(mp, cn);
            return res;
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("dri/")
    public DRItemsResource getDRI() {
        return new DRItemsResource(getCatalogue(), request, servletResponse, info);
    }

    @Path("permissions/")
    public SetBulkPermissionsResource getPermissions() {
        return new SetBulkPermissionsResource(getCatalogue(), request);
    }
}
