/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.rest;

import nl.uva.cs.lobcder.util.CatalogueHelper;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author dvasunin
 */
@Path("item/")
public class Item extends CatalogueHelper {

    @Context
    HttpServletRequest request;
    @Context
    UriInfo info;

    @Path("query/{uid}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public LogicalData getLogicalData(@PathParam("uid") Long uid) {
        try {
            LogicalData res = getCatalogue().getResourceEntryByUID(uid, null);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canRead(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return res;
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("data/{uid}")
    @GET
    public void getData(@PathParam("uid") Long uid, @Context HttpServletResponse _currentResponse) {
        try {
            LogicalData res = getCatalogue().getResourceEntryByUID(uid, null);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            String path = info.getBaseUri().toString();
            if(path.endsWith("/")) {
                path = path.substring(0, path.length() -1);
            }
            path = path.substring(0, path.lastIndexOf("/"));
            
            _currentResponse.sendRedirect(path + "/dav" + res.getLDRI());
            
        } catch (Exception ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("dri/")
    public DRIDataResource getDRI() {
        return new DRIDataResource(getCatalogue(), request);
    }

    @Path("permissions/")
    public PermissionsResource getPermissions() {
        return new PermissionsResource(getCatalogue(), request);
    }
}
