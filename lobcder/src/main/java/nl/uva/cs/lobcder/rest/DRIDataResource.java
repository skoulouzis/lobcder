/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

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
 *
 * @author dvasunin
 */
@Path("/Item")
public class DRIDataResource {
    
    JDBCatalogue catalogue = null;
    
    @Context
    private UriInfo context;
    
    public DRIDataResource() throws Exception {
        String jndiName = "bean/JDBCatalog";
        javax.naming.Context ctx = new InitialContext();
        if (ctx == null) {
            throw new Exception("JNDI could not create InitalContext ");
        }
        javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
        catalogue = (JDBCatalogue) envContext.lookup(jndiName);
        catalogue = new JDBCatalogue();
    }
    
    @Path("/{uid}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public LogicalData getLogicalData(@PathParam("uid") Long uid) {
        LogicalData res = null;
        try {
            res = catalogue.getResourceEntryByUID(uid, null);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
    
    @Path("/{uid}/supervised/{flag}")
    @PUT
    public void setSupervised(@PathParam("uid") Long uid, @PathParam("flag") Boolean flag) {
        try {
            catalogue.setFileSupervised(uid, flag, null);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    @Path("/{uid}/checksum/{checksum}")
    @PUT
    public void setSupervised(@PathParam("uid") Long uid, @PathParam("checksum") Long checksum) {
        try {
            catalogue.setFileChecksum(uid, checksum, null);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }    
    
    @Path("/{uid}/lastValidationDate/{lastValidationDate}")
    @PUT
    public void setLastValidationDate(@PathParam("uid") Long uid, @PathParam("lastValidationDate") Long lastValidationDate) {
        try {
            catalogue.setLastValidationDate(uid, lastValidationDate, null);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }           
}
