/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.rest;

import nl.uva.cs.lobcder.util.CatalogueHelper;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
@Path("items/")
public class Items extends CatalogueHelper {

    @Context
    UriInfo info;
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse servletResponse;

    @Path("query/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<LogicalData> getXml() {
        try {
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            List<LogicalData> resq = getCatalogue().queryLogicalData(info.getQueryParameters(), null);
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

    @Path("dri/")
    public DRItemsResource getDRI(){
        return new DRItemsResource(getCatalogue(), request, servletResponse, info);
    }
    
    @Path("permissions/")
    public SetBulkPermissionsResource getPermissions() {
        return new SetBulkPermissionsResource(getCatalogue(), request);
    }   
}
