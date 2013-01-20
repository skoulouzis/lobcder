/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 * REST Web Service
 *
 * @author dvasunin
 */
@Path("/Items")
public class DRItemsResource {

    JDBCatalogue catalogue = null;
    @Context
    private UriInfo context;

    /**
     * Creates a new instance of ItemsResource
     */
    public DRItemsResource() throws Exception {
        String jndiName = "bean/JDBCatalog";
        javax.naming.Context ctx = new InitialContext();
        if (ctx == null) {
            throw new Exception("JNDI could not create InitalContext ");
        }
        javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
        catalogue = (JDBCatalogue) envContext.lookup(jndiName);
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<LogicalData> getXml(@QueryParam("path") String path) {
        List<LogicalData> res = null;
        try {
            res = catalogue.getSupervised(path, null);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
    
    @Path("/{isSupervised}")
    @PUT
    public void setDirSupervised(@PathParam("isSupervised") Boolean param, @QueryParam("path") String path) {
        try {
            debug("setDirSupervised:  "+path+" " + param);
            catalogue.setDirSupervised(path, param, null);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getName()+": "+msg);
    }
}
