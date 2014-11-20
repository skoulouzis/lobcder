/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Constants;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.sql.*;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;

/**
 *
 * @author dvasunin
 */
@Log
public class SetBulkPermissionsResource {

    private JDBCatalogue catalogue;
    private HttpServletRequest request;

    SetBulkPermissionsResource(JDBCatalogue catalogue, HttpServletRequest request) {
        this.catalogue = catalogue;
        this.request = request;
    }

    private void setPermissions(Long uid, MyPrincipal principal, CallableStatement cs, PreparedStatement ps, Connection cn) throws SQLException {
        ps.setLong(1, uid);
        ArrayList<Long> folders = new ArrayList<>();
        try (ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                Long myUid = resultSet.getLong(1);
                String myOwner = resultSet.getString(2);
                Permissions p = catalogue.getPermissions(myUid, myOwner, cn);
                if (principal.canRead(p) && myUid != 1) {
                    folders.add(uid);
                }
            }
        }
        cs.setLong(6, uid);
        cs.execute();
        cn.commit();
        for (Long _uid : folders) {
            setPermissions(_uid, principal, cs, ps, cn);
        }
    }

    private void setPermissions(String rootPath, Permissions perm, MyPrincipal principal, @Nonnull Connection connection) throws SQLException {
        LogicalData ld = catalogue.getLogicalDataByPath(io.milton.common.Path.path(rootPath), connection);
        Permissions p = catalogue.getPermissions(ld.getUid(), ld.getOwner(), connection);
        if (ld.isFolder() && principal.canRead(p)) {
            try (CallableStatement cs = connection.prepareCall("{CALL updatePermissionsDirProc(?, ?, ?, ?, ?, ?)}");
                    PreparedStatement ps = connection.prepareStatement("SELECT uid, ownerId, ldName FROM ldata_table WHERE parentRef = ? AND datatype = '" + Constants.LOGICAL_FOLDER + "'")) {
                cs.setString(1, principal.getUserId());
                cs.setString(2, principal.getRolesStr());
                cs.setString(3, perm.getOwner());
                cs.setString(4, perm.getReadStr());
                cs.setString(5, perm.getWriteStr());
                setPermissions(ld.getUid(), principal, cs, ps, connection);
            }
        }
        if (principal.canWrite(p)) {
            catalogue.updateOwner(ld.getUid(), perm.getOwner(), connection);
            catalogue.setPermissions(ld.getUid(), perm, connection);
        }
    }

    //@PUT
    //@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void setPermissions(@QueryParam("path") String path, JAXBElement<Permissions> jbPermissions) {
        try (Connection connection = catalogue.getConnection()) {
            try {
                Permissions permissions = jbPermissions.getValue();
                MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
                setPermissionsJava(path, permissions, mp, connection);
                connection.commit();
            } catch (SQLException ex) {
                log.log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void setPermissionsJava(String rootPath, Permissions perm, MyPrincipal principal, @Nonnull Connection connection) throws SQLException {
        LogicalData ld = catalogue.getLogicalDataByPath(io.milton.common.Path.path(rootPath), connection);
        Permissions p = catalogue.getPermissions(ld.getUid(), ld.getOwner(), connection);
        if(ld.isFolder() && principal.canRead(p)) {
            try(PreparedStatement ps = connection.prepareStatement("SELECT uid, ownerId, datatype FROM ldata_table WHERE parentRef = ?")){
                setPermissionsJava(ld.getUid(), principal, ps, perm, connection);
            }
        }
        if (principal.canWrite(p)) {
            catalogue.updateOwner(ld.getUid(), perm.getOwner(), connection);
            catalogue.setPermissions(ld.getUid(), perm, connection);
        }
    }

    private void setPermissionsJava(Long uid, MyPrincipal principal, PreparedStatement ps, Permissions perm, Connection cn) throws SQLException {
        ps.setLong(1, uid);
        ArrayList<Long> updatePermIds = new ArrayList<>();
        try(ResultSet resultSet = ps.executeQuery()){
            while (resultSet.next()) {
                Long entry_uid = resultSet.getLong(1);
                String entry_owner = resultSet.getString(2);
                String entry_datatype = resultSet.getString(3);
                Permissions entry_p = catalogue.getPermissions(entry_uid, entry_owner, cn);
                if(entry_datatype.equals(Constants.LOGICAL_FOLDER) && principal.canRead(entry_p)){
                    setPermissionsJava(entry_uid, principal, ps, perm, cn);
                }
                if(principal.canWrite(entry_p)){
                    updatePermIds.add(entry_uid);
                }
            }
        }
        for(Long e_uid : updatePermIds){
            catalogue.updateOwner(e_uid, perm.getOwner(), cn);
            catalogue.setPermissions(e_uid, perm, cn);
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void setPermissions2(@QueryParam("path") String path, JAXBElement<Permissions> jbPermissions) {
        try (Connection connection = catalogue.getConnection()) {
            try {
                Permissions permissions = jbPermissions.getValue();
                MyPrincipal principal = (MyPrincipal) request.getAttribute("myprincipal");
                LogicalData ld = catalogue.getLogicalDataByPath(io.milton.common.Path.path(path), connection);
                Stack<Long> folders = new Stack<>();
                ArrayList<Long> elements = new ArrayList<>();
                ArrayList<Long> changeOwner = new ArrayList<>();
                Permissions p = catalogue.getPermissions(ld.getUid(), ld.getOwner(), connection);
                if(ld.isFolder() && principal.canRead(p)) {
                    folders.add(ld.getUid());
                }
                if(principal.canWrite(p)){
                    elements.add(ld.getUid());
                    if(!ld.getOwner().equals(permissions.getOwner())){
                        changeOwner.add(ld.getUid());
                    }
                }
                try(PreparedStatement ps = connection.prepareStatement("SELECT uid, ownerId, datatype FROM ldata_table WHERE parentRef = ?")){
                    while(!folders.isEmpty()){
                        Long curUid = folders.pop();
                        ps.setLong(1, curUid);
                        try(ResultSet resultSet = ps.executeQuery()){
                            while(resultSet.next()) {
                                Long entry_uid = resultSet.getLong(1);
                                String entry_owner = resultSet.getString(2);
                                String entry_datatype = resultSet.getString(3);
                                Permissions entry_p = catalogue.getPermissions(entry_uid, entry_owner, connection);
                                if(entry_datatype.equals(Constants.LOGICAL_FOLDER) && principal.canRead(entry_p)){
                                    folders.push(entry_uid);
                                }
                                if(principal.canWrite(entry_p)){
                                    elements.add(entry_uid);
                                    if(!entry_owner.equals(permissions.getOwner())) {
                                        changeOwner.add(entry_uid);
                                    }
                                }
                            }
                        }
                    }
                }
                final int batchSize = 100;
                int count = 0;
                try(PreparedStatement psDel = connection.prepareStatement("DELETE FROM permission_table WHERE permission_table.ldUidRef = ?");
                    PreparedStatement psIns = connection.prepareStatement("INSERT INTO permission_table (permType, ldUidRef, roleName) VALUES (?, ?, ?)")) {
                    for(Long uid : elements) {
                        psDel.setLong(1, uid);
                        psDel.addBatch();
                        for (String cr : permissions.getRead()) {
                            psIns.setString(1, "read");
                            psIns.setLong(2, uid);
                            psIns.setString(3, cr);
                            psIns.addBatch();
                        }
                        for (String cw : permissions.getWrite()) {
                            psIns.setString(1, "write");
                            psIns.setLong(2, uid);
                            psIns.setString(3, cw);
                            psIns.addBatch();
                        }
                        count++;
                        if (count % batchSize == 0) {
                            psDel.executeBatch();
                            psIns.executeBatch();
                        }
                    }
                    psDel.executeBatch();
                    psIns.executeBatch();
                }
                try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET ownerId = ? WHERE uid = ?")) {
                    count = 0;
                    ps.setString(1, permissions.getOwner());
                    for(Long uid : changeOwner) {
                        ps.setLong(2, uid);
                        ps.addBatch();
                        count++;
                        if (count % batchSize == 0) {
                            ps.executeBatch();
                        }
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException ex) {
                log.log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
