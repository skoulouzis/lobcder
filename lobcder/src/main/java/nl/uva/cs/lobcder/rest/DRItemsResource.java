/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * REST Web Service
 *
 * @author dvasunin
 */

@Log
public class DRItemsResource {

    JDBCatalogue catalogue = null;
    HttpServletRequest request = null;
    HttpServletResponse servletResponse = null;
    UriInfo info = null;

    /**
     * Creates a new instance of ItemsResource
     */
    public DRItemsResource(JDBCatalogue catalogue, HttpServletRequest request, HttpServletResponse servletResponse, UriInfo info) {
        this.catalogue = catalogue;
        this.request = request;
        this.servletResponse = servletResponse;
        this.info = info;
    }

    private void setDirSupervised(Long uid, Boolean flag, PreparedStatement ps1, PreparedStatement ps2) throws SQLException {
        ps1.setBoolean(1, flag);
        ps1.setLong(2, uid);
        ps1.executeUpdate();
        ArrayList<Long> uidArrayList = new ArrayList<>();
        ps2.setLong(1, uid);
        ResultSet rs = ps2.executeQuery();
        while (rs.next()) {
            Long _uid = rs.getLong(1);
            if(_uid != 1)
                uidArrayList.add(_uid);
        }
        for (Long uid1 : uidArrayList) {
            setDirSupervised(uid1, flag, ps1, ps2);
        }
    }

    @Path("supervised/{flag}")
    @PUT
    public void setSupervised(@PathParam("flag") Boolean param, @QueryParam("path") String path) {
        try (Connection cn = catalogue.getConnection()) {
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (mp.isAdmin()) {
                try {
                    LogicalData logicalData;
                    if(path == null || path.isEmpty()){
                        logicalData = catalogue.getLogicalDataByUid(1L, cn);
                    } else {
                        logicalData = catalogue.getLogicalDataByPath(io.milton.common.Path.path(path), cn);
                    }
                    catalogue.setLogicalDataSupervised(logicalData.getUid(), param, cn);
                    if (logicalData.isFolder()) {
                        try (PreparedStatement ps1 = cn.prepareStatement("UPDATE ldata_table SET isSupervised=? WHERE parentRef = ?");
                             PreparedStatement ps2 = cn.prepareStatement("SELECT uid FROM ldata_table WHERE parentRef = ? AND datatype = '" + Constants.LOGICAL_FOLDER + "'")) {
                            setDirSupervised(logicalData.getUid(), param, ps1, ps2);
                        }
                    }
                    cn.commit();
                } catch (SQLException e) {
                    DRItemsResource.log.log(Level.SEVERE, null, e);
                    cn.rollback();
                }
            } else {
                log.warning("NOT a superuser: want change DRI flag for " + path);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
