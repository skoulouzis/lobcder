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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

/**
 * Sets and gets permissions for a resource 
 * @author dvasunin
 */
@Log
public class PermissionsResource {

    private JDBCatalogue catalogue;
    private HttpServletRequest request;

    PermissionsResource(JDBCatalogue catalogue, HttpServletRequest request) {
        this.catalogue = catalogue;
        this.request = request;
    }

    /**
     * Gets the resource's permissions: owner, read, write
     *
     * @param uid the id of the resource
     * @return the resource's permissions: owner, read, write
     */
    @Path("{uid}/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Permissions getPermissions(@PathParam("uid") Long uid) {
        try (Connection cn = catalogue.getConnection()) {
            LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
            if (!mp.canRead(p)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return p;
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Sets the resource's permissions: owner, read, write
     *
     * @param uid the id of the resource
     * @param jbPermissions the permissions: owner, read, write
     */
    @Path("{uid}/")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void setPermissions(@PathParam("uid") Long uid, JAXBElement<Permissions> jbPermissions) {
        try (Connection cn = catalogue.getConnection()) {
            try {
                LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
                if (res == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
                Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
                if (!mp.canWrite(p)) {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
                Permissions permissions = jbPermissions.getValue();
                catalogue.updateOwner(uid, permissions.getOwner(), cn);
                catalogue.setPermissions(uid, permissions, cn);
                cn.commit();
            } catch (SQLException ex) {
                log.log(Level.SEVERE, null, ex);
                cn.rollback();
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UIDS {
        @XmlElement(name="guid")
        private Set<String> uids = new HashSet<>();
    }

    @Path("recursive/{uid}/")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public UIDS setPermissions1(@PathParam("uid") Long uid_p, JAXBElement<Permissions> jbPermissions) {
        UIDS result = new UIDS();
        try (Connection connection = catalogue.getConnection()) {
            try {
                Permissions permissions = jbPermissions.getValue();
                MyPrincipal principal = (MyPrincipal) request.getAttribute("myprincipal");
                LogicalData ld = catalogue.getLogicalDataByUid(uid_p, connection);
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

                try(PreparedStatement ps = connection.prepareStatement("SELECT permType, roleName, ldUidRef, id  FROM permission_table WHERE permission_table.ldUidRef = ?",
                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_UPDATABLE)) {
                    for (Long uid : elements) {
                        ps.setLong(1, uid);
                        ResultSet rs = ps.executeQuery();
                        Set<String> read = new HashSet<>(permissions.getRead());
                        Set<String> write = new HashSet<>(permissions.getWrite());
                        boolean updateFlag = false;
                        while (rs.next()) {
                            String permType = rs.getString(1);
                            String roleName = rs.getString(2);
                            if (permType.equals("read")) {
                                if(!read.remove(roleName)) {
                                    rs.deleteRow();
                                    updateFlag = true;
                                }
                            } else if (permType.equals("write")) {
                                if(!write.remove(roleName)) {
                                    rs.deleteRow();
                                    updateFlag = true;
                                }
                            }
                        }
                        for (String role : read) {
                            rs.moveToInsertRow();
                            rs.updateString(1, "read");
                            rs.updateString(2, role);
                            rs.updateLong(3, uid);
                            rs.insertRow();
                        }
                        for (String role : write) {
                            rs.moveToInsertRow();
                            rs.updateString(1, "write");
                            rs.updateString(2, role);
                            rs.updateLong(3, uid);
                            rs.insertRow();
                        }
                        if (updateFlag || !read.isEmpty() || !write.isEmpty()) {
                            String myuid = catalogue.getGlobalID(uid, connection);
                            if (myuid != null) {
                                result.uids.add(myuid);
                            }
                        }
                    }
                }
                if(permissions.getOwner() != null && !permissions.getOwner().isEmpty()) {
                    try (PreparedStatement ps = connection.prepareStatement("SELECT ownerId, uid from ldata_table WHERE uid = ?",
                            java.sql.ResultSet.TYPE_FORWARD_ONLY,
                            java.sql.ResultSet.CONCUR_UPDATABLE)) {
                        for (Long uid : changeOwner) {
                            ps.setLong(1, uid);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                String owner = rs.getString(1);
                                if (!owner.equals(permissions.getOwner())) {
                                    rs.updateString(1, permissions.getOwner());
                                    rs.updateRow();
                                    result.uids.add(catalogue.getGlobalID(uid, connection));
                                }
                            }
                        }
                    }
                }
                connection.commit();
                return result;
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

    @Path("recursive/{uid}/")
    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public UIDS addPermissionsRecursive(@PathParam("uid") Long uid_p, JAXBElement<Permissions> jbPermissions) {
        UIDS result = new UIDS();
        try (Connection connection = catalogue.getConnection()) {
            try {
                Permissions permissions = jbPermissions.getValue();
                MyPrincipal principal = (MyPrincipal) request.getAttribute("myprincipal");
                LogicalData ld = catalogue.getLogicalDataByUid(uid_p, connection);
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
                try(PreparedStatement ps = connection.prepareStatement("SELECT permType, roleName, ldUidRef, id  FROM permission_table WHERE permission_table.ldUidRef = ?",
                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_UPDATABLE)) {
                    for (Long uid : elements) {
                        ps.setLong(1, uid);
                        ResultSet rs = ps.executeQuery();
                        Set<String> read = new HashSet<>(permissions.getRead());
                        Set<String> write = new HashSet<>(permissions.getWrite());
                        while (rs.next()) {
                            String permType = rs.getString(1);
                            String roleName = rs.getString(2);
                            if (permType.equals("read")) {
                                read.remove(roleName);
                            } else if (permType.equals("write")) {
                                write.remove(roleName);
                            }
                        }
                        for (String role : read) {
                            rs.moveToInsertRow();
                            rs.updateString(1, "read");
                            rs.updateString(2, role);
                            rs.updateLong(3, uid);
                            rs.insertRow();
                        }
                        for (String role : write) {
                            rs.moveToInsertRow();
                            rs.updateString(1, "write");
                            rs.updateString(2, role);
                            rs.updateLong(3, uid);
                            rs.insertRow();
                        }
                        if (!read.isEmpty() || !write.isEmpty()) {
                            String myuid = catalogue.getGlobalID(uid, connection);
                            if (myuid != null) {
                                result.uids.add(myuid);
                            }
                        }
                    }
                }
                if(permissions.getOwner() != null && !permissions.getOwner().isEmpty()) {
                    try (PreparedStatement ps = connection.prepareStatement("SELECT ownerId, uid from ldata_table WHERE uid = ?",
                            java.sql.ResultSet.TYPE_FORWARD_ONLY,
                            java.sql.ResultSet.CONCUR_UPDATABLE)) {
                        for (Long uid : changeOwner) {
                            ps.setLong(1, uid);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                String owner = rs.getString(1);
                                if (!owner.equals(permissions.getOwner())) {
                                    rs.updateString(1, permissions.getOwner());
                                    rs.updateRow();
                                    result.uids.add(catalogue.getGlobalID(uid, connection));
                                }
                            }
                        }
                    }
                }
                connection.commit();
                return result;
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

    @Path("recursive/{uid}/")
    @DELETE
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public UIDS delPermissionsRecursive(@PathParam("uid") Long uid_p, JAXBElement<Permissions> jbPermissions) {
        UIDS result = new UIDS();
        try (Connection connection = catalogue.getConnection()) {
            try {
                Permissions permissions = jbPermissions.getValue();
                MyPrincipal principal = (MyPrincipal) request.getAttribute("myprincipal");
                LogicalData ld = catalogue.getLogicalDataByUid(uid_p, connection);
                Stack<Long> folders = new Stack<>();
                ArrayList<Long> elements = new ArrayList<>();
                Permissions p = catalogue.getPermissions(ld.getUid(), ld.getOwner(), connection);
                if(ld.isFolder() && principal.canRead(p)) {
                    folders.add(ld.getUid());
                }
                if(principal.canWrite(p)){
                    elements.add(ld.getUid());
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
                                }
                            }
                        }
                    }
                }
                try(PreparedStatement ps = connection.prepareStatement("DELETE FROM permission_table WHERE permType = ? AND ldUidRef = ? AND roleName=?")) {
                    for(Long uid : elements) {
                        for (String cr : permissions.getRead()) {
                            ps.setString(1, "read");
                            ps.setLong(2, uid);
                            ps.setString(3, cr);
                            ps.addBatch();
                        }
                        for (String cw : permissions.getWrite()) {
                            ps.setString(1, "write");
                            ps.setLong(2, uid);
                            ps.setString(3, cw);
                            ps.addBatch();
                        }
                        for(int i : ps.executeBatch()) {
                            if(i > 0) {
                                String myuid = catalogue.getGlobalID(uid, connection);
                                if (myuid != null) {
                                    result.uids.add(myuid);
                                }
                                break;
                            }
                        }
                    }
                }
                connection.commit();
                return result;
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
