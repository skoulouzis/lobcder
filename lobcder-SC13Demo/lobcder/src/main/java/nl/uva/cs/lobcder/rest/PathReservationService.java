/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.io.IOException;
import java.net.InetAddress;
import nl.uva.cs.lobcder.rest.wrappers.WorkerStatus;
import nl.uva.cs.lobcder.rest.wrappers.ReservationInfo;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
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
import nl.uva.cs.lobcder.util.Network;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.commons.io.FilenameUtils;

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

    @Path("get_workers/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<WorkerStatus> getXml() throws MalformedURLException {
        //        rest/reservation/get_workers/?id=all
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        if (mp.getRoles().contains("planner") || mp.isAdmin()
                && queryParameters != null && !queryParameters.isEmpty()) {
            String workerID = queryParameters.getFirst("id");
            ArrayList<WorkerStatus> workersStatus = new ArrayList<>();
            workers = PropertiesHelper.getWorkers();
            for (String s : workers) {
                WorkerStatus ws = new WorkerStatus();
                ws.setHostName(new URL(s).getHost());
                ws.setStatus("READY");
                workersStatus.add(ws);
            }

            return workersStatus;
        }
        return null;
    }

    @Path("{commID}/request/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public ReservationInfo request(@PathParam("commID") String communicationID) throws MalformedURLException, IOException {
        //        rest/reservation/5455/request/?dataPath=/sbuiifv/dsudsuds&storageSiteHost=sps1&storageSiteHost=sps2&storageSiteHost=sps3
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        if (mp.getRoles().contains("planner") || mp.isAdmin()
                && queryParameters != null && !queryParameters.isEmpty()) {

            String dataName = queryParameters.getFirst("dataName");
            if (dataName != null && dataName.length() > 0) {
                List<String> storageList = queryParameters.get("storageSiteHost");
                String storageSiteHost = null;
                int index = -1;
                if (storageList != null && storageList.size() > 0) {
                    storageSiteHost = getStorageSiteHost(storageList);
                    index = storageList.indexOf(storageSiteHost);
                } else {
                }

                LogicalData ld;
                Permissions p = null;
                try (Connection cn = getCatalogue().getConnection()) {
                    //-----------------THIS IS TEMPORARY IT'S ONLY FOR THE DEMO!!!!!!!!!!
                    String fileNameWithOutExt = FilenameUtils.removeExtension(dataName);
                    fileNameWithOutExt += ".webm";
                    List<LogicalData> ldList = getCatalogue().getLogicalDataByName(io.milton.common.Path.path(fileNameWithOutExt), cn);
                    if (ldList == null || ldList.isEmpty()) {
                        ldList = getCatalogue().getLogicalDataByName(io.milton.common.Path.path(dataName), cn);
                    }
                    //--------------------------------------------------------------
                    if (ldList == null || ldList.isEmpty()) {
                        Response.status(Response.Status.NOT_FOUND);
                        return null;
                    }
                    //Should be only one
                    ld = ldList.get(0);
                    if (ld != null) {
                        p = getCatalogue().getPermissions(ld.getUid(), ld.getOwner(), cn);
                    }
                } catch (SQLException ex) {
                    log.log(Level.SEVERE, null, ex);
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                }
                //                    Integer alocationStrategy = Integer.valueOf(queryParameters.getFirst("allocationStrategy"));

                ReservationInfo info = new ReservationInfo();
                if (p != null && mp.canRead(p)) {

                    info.setCommunicationID(communicationID);
                    String workerURL = scheduleWorker(storageSiteHost, ld);
                    info.setCommunicationID(communicationID);
                    storageSiteHost = Network.replaceIP(storageSiteHost);
                    info.setStorageHost(storageSiteHost);
                    info.setStorageHostIndex(index);

                    workerURL = Network.replaceIP(workerURL);
                    info.setWorkerDataAccessURL(workerURL);
                }
                return info;

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
            workers = PropertiesHelper.getWorkers();
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

    private String scheduleWorker(String storageSiteHost, LogicalData ld) throws MalformedURLException, IOException {
        workers = PropertiesHelper.getWorkers();
        if (workers == null || workers.size() < 1 || !PropertiesHelper.doRedirectGets()) {
            return null;
        }
        String worker = null;
        if (storageSiteHost != null) {
            for (String w : workers) {
                URL wURI = new URL(w);
                String ip = Network.getIP(storageSiteHost);
                if (wURI.getHost().equals(ip)) {
                    worker = w;
                    break;
                }
            }
        }

        if (worker == null || worker.length() <= 1) {
            if (workerIndex >= workers.size()) {
                workerIndex = 0;
            }
            worker = workers.get(workerIndex++);
        }



        URL workerURL = new URL(worker);
        String workerIP = Network.getIP(workerURL.getHost());
        worker =  new URL(workerURL.getProtocol(), workerIP, workerURL.getPort(), workerURL.getFile()).toString();

        if (!worker.endsWith("/")) {
            worker += "/";
        }
        String w = worker + ld.getUid();
        String token = UUID.randomUUID().toString();
        AuthWorker.setTicket(worker, token);
        return w + "/" + token;
    }

    private String getStorageSiteHost(List<String> storageList) throws MalformedURLException, UnknownHostException {
        workers = PropertiesHelper.getWorkers();
        List<String> selectionList = new ArrayList<>();
        for (String w : workers) {
            URL wURI = new URL(w);

            for (String h : storageList) {
                String ip = Network.getIP(h);
                if (ip.equals(wURI.getHost())) {
                    selectionList.add(new URL(wURI.getProtocol(), ip, wURI.getPort(), wURI.getFile()).toString());
                }
            }
        }
        if (!selectionList.isEmpty()) {
            int index = new Random().nextInt(selectionList.size());
            String[] storageArray = storageList.toArray(new String[selectionList.size()]);

            return storageArray[index];
        } else {
            return null;
        }
    }

   
}
