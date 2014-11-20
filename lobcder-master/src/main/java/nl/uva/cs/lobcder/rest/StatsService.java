/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.cs.lobcder.util.CatalogueHelper;

/**
 * Gets and sets stats about transfers.
 *
 * @author S. Koulouzis
 */
@Log
@Path("lob_statistics/")
public class StatsService extends CatalogueHelper {

    @Context
    HttpServletRequest request;
    @Context
    UriInfo info;

    @Path("set")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void setStats(JAXBElement<Stats> jbStats) {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.getRoles().contains("worker") || mp.isAdmin()) {
            try {
                Stats stats = jbStats.getValue();
                getCatalogue().setSpeed(stats);
            } catch (SQLException ex) {
                Logger.getLogger(StatsService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
