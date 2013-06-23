/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.CatalogueHelper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 *
 * @author dvasunin
 */
@Log
@Path("item/")
public class Item extends CatalogueHelper {

    @Context
    HttpServletRequest request;
    @Context
    UriInfo info;

    @Path("query/{uid}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public LogicalDataWrapped getLogicalData(@PathParam("uid") Long uid) {
        try (Connection cn = getCatalogue().getConnection()) {
            LogicalData resLD = getCatalogue().getLogicalDataByUid(uid, cn);
            if (resLD == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            Permissions p = getCatalogue().getPermissions(uid, resLD.getOwner(), cn);
            if (!mp.canRead(p)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            LogicalDataWrapped res = new LogicalDataWrapped();
            res.setLogicalData(resLD);
            res.setPermissions(p);
            res.setPath(getCatalogue().getPathforLogicalData(resLD));
            if (!resLD.isFolder() && mp.isAdmin()) {
                res.setPdriList(getCatalogue().getPdriDescrByGroupId(resLD.getPdriGroupId(), cn));
            }
            return res;
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

//    @Path("data/{uid}")
//    @GET
//    public Void getData(@PathParam("uid") Long uid, @Context HttpServletResponse _currentResponse) {
//        try {
//            LogicalData res = getCatalogue().getLogicalDataByUid(uid);
//            if (res == null) {
//                throw new WebApplicationException(Response.Status.NOT_FOUND);
//            }
//            String path = info.getBaseUri().toString();
//            if(path.endsWith("/")) {
//                path = path.substring(0, path.length() -1);
//            }
//            path = path.substring(0, path.lastIndexOf("/"));
//
//            _currentResponse.sendRedirect(path + res.getPath());
//            return null;
//
//        } catch (Exception ex) {
//            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
//            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
//        }
//    }
    @Path("dri/")
    public DRIDataResource getDRI() {
        return new DRIDataResource(getCatalogue(), request);
    }

    @Path("permissions/")
    public PermissionsResource getPermissions() {
        return new PermissionsResource(getCatalogue(), request);
    }
}