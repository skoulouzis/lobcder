/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
        catalogue = new JDBCatalogue();
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<LogicalData> getXml() {
        List<LogicalData> res = null;
        try {
            res = catalogue.getSupervised(null);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
}
