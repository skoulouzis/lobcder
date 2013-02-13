/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.rest;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Debug;

/**
 * REST Web Service
 *
 * @author dvasunin
 */
public class DRItemsResource extends Debug {

    @Override
    protected boolean debug() {
        return true;
    }
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

    @Path("supervised/{flag}")
    @PUT
    public void setDirSupervised(@PathParam("flag") Boolean param, @QueryParam("path") String path) {
        try {
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            catalogue.setDirSupervised2(path, param, mp, null);
//            if (mp.getRoles().contains("admin")) {
//                debug("setDirSupervised:  " + path + " " + param);
//                catalogue.setDirSupervised(path, param, null);
//            } else {
//                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
//            }
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("supervised/{flag}")
    @GET
    public List<LogicalData> getDirSupervised(@PathParam("flag") Boolean param, @QueryParam("path") String path) {
        try {
            MultivaluedMap<String, String> queryParameters = info.getPathParameters();
            queryParameters.add("isSupervised", param.toString());
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            List<LogicalData> resq = catalogue.queryLogicalData(queryParameters, null);
            List<LogicalData> res = new LinkedList<LogicalData>();
            for (LogicalData ld : resq) {
                if (mp.canRead(ld.getPermissions())) {
                    res.add(ld);
                }
            }
            return res;
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
