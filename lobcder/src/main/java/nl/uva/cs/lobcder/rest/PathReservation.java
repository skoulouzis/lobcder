/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthWorker;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.vlet.exception.VlException;

/**
 *
 * @author S. Koulouzis
 */
@Log
@Path("reservation/")
public class PathReservation extends CatalogueHelper {

    @Context
    UriInfo info;
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse servletResponse;

    @Path("{commID}/candidates/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Candidate setCandidate(@PathParam("commID") String communicationID) {
//        rest/reservation/5455/candidates/?host-id-file=sps1;dff;/sbuiifv/dsudsuds&host-id-file=sps2;dcsdcdff;/sbuiifv/dsudsud/asc
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        List<String> hostIdFiles = queryParameters.get("host-id-file");
        int index = new Random().nextInt(hostIdFiles.size());
        String[] array = hostIdFiles.toArray(new String[hostIdFiles.size()]);
        String[] hostIdFile = array[index].split(";");


        Candidate c = new Candidate();
        LogicalData ld;
        MyPrincipal mp;
        Permissions p=null;
        try (Connection cn = getCatalogue().getConnection()) {
            ld = getCatalogue().getLogicalDataByPath(io.milton.common.Path.path(hostIdFile[2]), cn);
            if (ld != null) {
                p = getCatalogue().getPermissions(ld.getUid(), ld.getOwner(), cn);
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (p!=null && mp.canRead(p)) {
            c.setCommunicationID(communicationID);
            String url = getWorker(hostIdFile[0], ld);
            c.setURL(url);
            c.setCandidateID(hostIdFile[1]);
            c.setFilePath(hostIdFile[2]);
        }
        return c;
    }

    @Path("workers/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<Worker> getWorkersState() {
//        rest/reservation/workers/?host=kscvdfv&host=sp2&host=192.168.1.1
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        List<String> workers = queryParameters.get("host");
        List<Worker> workersStatus = new ArrayList<>();
        for (String worker : workers) {
            Worker w = new Worker();
            w.setHostName(worker);
            w.setStatus("READY");
            workersStatus.add(w);
        }
        return workersStatus;
    }

    private String getWorker(String worker, LogicalData ld) {
        if (!worker.endsWith("/")) {
            worker += "/";
        }
        String w = worker + ld.getUid();
        String token = UUID.randomUUID().toString();
        AuthWorker.setTicket(worker, token);
        return w + "/" + token;
    }
}
