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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 *
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
}
