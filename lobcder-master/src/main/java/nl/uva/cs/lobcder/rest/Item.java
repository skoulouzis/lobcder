/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.rest.wrappers.LogicalDataWrapped;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.GridHelper;
import nl.uva.vlet.exception.VlException;

/**
 * Gets resource properties like length owner physical location etc.
 *
 * @author dvasunin
 */
@Log
@Path("item/")
public class Item extends CatalogueHelper {

    @Context
    HttpServletRequest request;
    @Context
    UriInfo info;

    /**
     * Gets the resource's properties (length, owner, permitions etc.)
     *
     * @param uid the id of the resource
     * @return the resource's properties
     * @throws FileNotFoundException
     * @throws IOException
     * @throws VlException
     * @throws URISyntaxException
     * @throws MalformedURLException
     * @throws Exception
     */
    @Path("query/{uid}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public LogicalDataWrapped getLogicalData(@PathParam("uid") Long uid) throws FileNotFoundException, IOException, VlException, URISyntaxException, MalformedURLException, Exception {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
//        LogicalDataWrapped res = logicalDataCache.get(uid);
//        if(res !=null ){            
//        }

        try {
            LogicalData resLD = getCatalogue().getLogicalDataByUid(uid);
            if (resLD == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            Permissions p = getCatalogue().getPermissions(uid, resLD.getOwner());
            if (!mp.canRead(p)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            LogicalDataWrapped res = new LogicalDataWrapped();
            res.setGlobalID(getCatalogue().getGlobalID(uid));
            res.setLogicalData(resLD);
            res.setPermissions(p);
            res.setPath(getCatalogue().getPathforLogicalData(resLD));
            if (!resLD.isFolder()) {
                List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(resLD.getPdriGroupId());
                if (mp.isAdmin()) {
                    for (PDRIDescr pdri : pdriDescr) {
                        if (pdri.getResourceUrl().startsWith("lfc")
                                || pdri.getResourceUrl().startsWith("srm")
                                || pdri.getResourceUrl().startsWith("gftp")) {
                            pdriDescr.remove(pdri);
                            GridHelper.initGridProxy(pdri.getUsername(), pdri.getPassword(), null, false);
                            pdri.setPassword(GridHelper.getProxyAsBase64String());
                            pdriDescr.add(pdri);
                        }
                    }
                } else {
                    for (PDRIDescr pdri : pdriDescr) {
                        pdriDescr.remove(pdri);
                        pdri.setPassword(null);
                        pdri.setUsername(null);
                        pdri.setKey(null);
                        pdri.setId(null);
                        pdri.setPdriGroupRef(null);
                        pdri.setStorageSiteId(null);
                        pdriDescr.add(pdri);
                    }
                }
                res.setPdriList(pdriDescr);
            }
            return res;
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

//    @Path("data/{uid}")
//    @GET
//    public Void getData(@PathParam("uid") Long uid, @Context HttpServletResponse _currentResponse) {
//        try {
//            LogicalData res = getCatalogue().getLogicalDataByUid(uid);
//            if (res == null) {
//                throw new WebApplicationException(Response.Status.NOT_FOUND);
//            }
//            String path = info.getBaseUri().toString();
//            if(path.endsWith("/")) {
//                path = path.substring(0, path.length() -1);
//            }
//            path = path.substring(0, path.lastIndexOf("/"));
//
//            _currentResponse.sendRedirect(path + res.getPath());
//            return null;
//
//        } catch (Exception ex) {
//            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
//            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
//        }
//    }
    @Path("dri/")
    public DRIDataResource getDRI() {
        return new DRIDataResource(getCatalogue(), request);
    }

    @Path("permissions/")
    public PermissionsResource getPermissions() {
        return new PermissionsResource(getCatalogue(), request);
    }
}
