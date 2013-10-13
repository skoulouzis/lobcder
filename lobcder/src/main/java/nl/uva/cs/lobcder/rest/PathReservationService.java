/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import nl.uva.cs.lobcder.rest.wrappers.WorkerStatus;
import nl.uva.cs.lobcder.rest.wrappers.ReservationInfo;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import nl.uva.cs.lobcder.util.WorkerHelper;

/**
 *
 * @author S. Koulouzis
 */
@Log
@Path("reservation/")
public class PathReservationService extends CatalogueHelper {

    @Context
    UriInfo info;
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse servletResponse;
    private List<String> workers;
    private static int workerIndex = 0;

    @Path("{commID}/request/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public ReservationInfo request(@PathParam("commID") String communicationID) throws MalformedURLException {
        //        rest/reservation/5455/request/?dataPath=/sbuiifv/dsudsuds&storageSiteHost=sps1&storageSiteHost=sps2&storageSiteHost=sps3
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        if (mp.getRoles().contains("planner") || mp.isAdmin()
                && queryParameters != null && !queryParameters.isEmpty()) {
            String dataPath = queryParameters.getFirst("dataPath");
            if (dataPath != null && dataPath.length() > 0) {
                List<String> storageList = queryParameters.get("storageSiteHost");
                if (storageList != null && storageList.size() > 0) {
                    int index = new Random().nextInt(storageList.size());
                    String storageSiteHost;
                    String[] storageArray = storageList.toArray(new String[storageList.size()]);
                    storageSiteHost = storageArray[index];

                    ReservationInfo info = new ReservationInfo();
                    LogicalData ld;
                    Permissions p = null;
                    try (Connection cn = getCatalogue().getConnection()) {
                        ld = getCatalogue().getLogicalDataByPath(io.milton.common.Path.path(dataPath), cn);
                        if (ld != null) {
                            p = getCatalogue().getPermissions(ld.getUid(), ld.getOwner(), cn);
                        }
                    } catch (SQLException ex) {
                        log.log(Level.SEVERE, null, ex);
                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                    }

                    if (p != null && mp.canRead(p)) {
                        info.setCommunicationID(communicationID);
                        String workerURL = scheduleWorker(storageSiteHost, ld);
                        info.setCommunicationID(communicationID);
                        info.setStorageHost(storageSiteHost);
                        info.setStorageHostIndex(index);
                        info.setWorkerDataAccessURL(workerURL);
                    }
                    return info;
                }
            }
        }
        return null;
    }

    @Path("workers/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<WorkerStatus> getWorkersState() {
//        rest/reservation/workers/?host=kscvdfv&host=sp2&host=192.168.1.1
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.getRoles().contains("planner") || mp.isAdmin()) {
            MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
            List<String> queryWorkers = queryParameters.get("host");
            List<WorkerStatus> workersStatus = new ArrayList<>();
            workers = WorkerHelper.getWorkers();
            for (String worker : queryWorkers) {
                WorkerStatus ws = new WorkerStatus();
                ws.setStatus("UNKNOWN");
                for (String w : workers) {
                    if (w.contains(worker)) {
                        ws.setStatus("READY");
                        break;
                    }
                }
                ws.setHostName(worker);
                workersStatus.add(ws);
            }
            return workersStatus;
        }
        return null;
    }

    private String scheduleWorker(String storageSiteHost, LogicalData ld) throws MalformedURLException {
        workers = WorkerHelper.getWorkers();
        String worker = null;
        for (String w : workers) {
            URL wURI = new URL(w);
            if (wURI.getHost().equals(storageSiteHost)) {
                worker = w;
                break;
            }
        }
        if (worker == null || worker.length() <= 1) {
            if (workerIndex >= workers.size()) {
                workerIndex = 0;
            }
            worker = workers.get(workerIndex++);
        }

        if (!worker.endsWith("/")) {
            worker += "/";
        }
        String w = worker + ld.getUid();
        String token = UUID.randomUUID().toString();
        AuthWorker.setTicket(worker, token);
        return w + "/" + token;
    }
}
