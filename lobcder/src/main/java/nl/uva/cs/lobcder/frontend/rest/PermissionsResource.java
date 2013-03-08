/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.rest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author dvasunin
 */
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
        try {
            LogicalData res = catalogue.getResourceEntryByUID(uid, null);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canRead(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return res.getPermissions();
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("{uid}/")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void setPermissions(@PathParam("uid") Long uid, JAXBElement<Permissions> jbPermissions) {
        Connection cn = null;
        try {
            cn = catalogue.getConnection();
            LogicalData res = catalogue.getResourceEntryByUID(uid, cn);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canWrite(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }    
            Permissions permissions = jbPermissions.getValue();
            res.setOwner(permissions.getOwner());
            catalogue.registerOrUpdateResourceEntry(res, cn);
            catalogue.setPermissions(res.getUID(), permissions, cn);
        } catch (CatalogueException ex) {          
            try {
                Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
                if(cn != null) {
                    cn.rollback();
                }
            } catch (SQLException ex1) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex1);
            } finally {
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            try {
                if (cn != null && !cn.isClosed()) {
                    cn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
