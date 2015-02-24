/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.optimization.SDNControllerClient;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Gets and sets stats about transfers.
 *
 * @author S. Koulouzis
 */
@Log
@Path("sdn/")
public class SDNService extends CatalogueHelper {

    @Context
    HttpServletRequest request;
    @Context
    UriInfo info;
    private SDNControllerClient sdnClient;

    @Path("optimizeFlow")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void setStats(JAXBElement<Endpoints> jbEndpoints) throws IOException, InterruptedException {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.getRoles().contains("worker") || mp.isAdmin()) {
            Endpoints endpoints = jbEndpoints.getValue();
            if (sdnClient == null) {
                String uri = PropertiesHelper.getSDNControllerURL();
                sdnClient = new SDNControllerClient(uri);
            }
            Set<String> sources = new HashSet<>();
            sources.add(endpoints.source);
            List<DefaultWeightedEdge> shortestPath = sdnClient.getShortestPath(endpoints.destination, sources);
            sdnClient.pushFlow(shortestPath);
        }
    }
}
