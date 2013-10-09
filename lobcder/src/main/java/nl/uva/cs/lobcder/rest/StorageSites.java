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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.vlet.exception.VlException;

/**
 *
 * @author S. Koulouzis
 */
@Log
@Path("storage_sites/")
public class StorageSites extends CatalogueHelper {

    @Context
    UriInfo info;
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse servletResponse;

    public StorageSites() throws NamingException {
    }

    @Path("query/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public StorageSiteWrapperList getXml() throws FileNotFoundException, VlException, URISyntaxException, IOException, MalformedURLException, Exception {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.isAdmin()) {
            try (Connection cn = getCatalogue().getConnection()) {
                List<StorageSiteWrapper> res = queryStorageSites(cn);
                StorageSiteWrapperList sswl = new StorageSiteWrapperList();
                sswl.setSites(res);
                return sswl;
            } catch (SQLException ex) {
                log.log(Level.SEVERE, null, ex);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    @Path("set/")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void set(JAXBElement<StorageSiteWrapperList> jbSites) throws SQLException {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.isAdmin()) {
            try (Connection connection = getCatalogue().getConnection()) {
                StorageSiteWrapperList sitesWL = jbSites.getValue();
                List<StorageSiteWrapper> sswl = sitesWL.sites;
                if (sswl != null && sswl.size() > 0) {
                    Collection<StorageSite> sites = new ArrayList<>();
                    for (StorageSiteWrapper ssw : sswl) {
                        StorageSite site = new StorageSite();
                        Credential cred = new Credential();
                        cred.setStorageSitePassword(ssw.getCredential().getStorageSitePassword());
                        cred.setStorageSiteUsername(ssw.getCredential().getStorageSiteUsername());
                        site.setCredential(cred);
                        site.setCurrentNum(ssw.getCurrentNum());
                        site.setCurrentSize(ssw.getCurrentSize());
                        site.setResourceURI(ssw.getResourceURI());
                        site.setEncrypt(ssw.isEncrypt());
                        site.setCache(ssw.isCache());
                        site.setQuotaNum(ssw.getQuotaNum());
                        site.setQuotaSize(ssw.getQuotaSize());
                        sites.add(site);
                    }
                    getCatalogue().insertOrUpdateStorageSites(sites, connection);
                    connection.commit();
                }

            }
        }
    }

    @Path("delete/")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void delete(IDWrapperList ids) throws SQLException {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.isAdmin()) {
            try (Connection connection = getCatalogue().getConnection()) {
                getCatalogue().deleteStorageSites(ids.ids, connection);
                connection.commit();
            }
        }
    }

    private List<StorageSiteWrapper> queryStorageSites(@Nonnull Connection cn) throws SQLException {
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        List<String> ids = queryParameters.get("id");
        if (ids != null && ids.size() > 0 && ids.get(0).equals("all")) {
            Collection<StorageSite> sites = getCatalogue().getStorageSites(cn);
            Collection<StorageSite> cachesites = getCatalogue().getCacheStorageSites(cn);
            List<StorageSiteWrapper> sitesWarpper = new ArrayList<>();
            for (StorageSite s : sites) {
                StorageSiteWrapper sw = new StorageSiteWrapper();
                CredentialWrapped cw = new CredentialWrapped();
//                cw.setStorageSitePassword(s.getCredential().getStorageSitePassword());
                cw.setStorageSitePassword("************");
                cw.setStorageSiteUsername(s.getCredential().getStorageSiteUsername());
                sw.setCredential(cw);
                sw.setCurrentNum(s.getCurrentNum());
                sw.setEncrypt(s.isEncrypt());
                sw.setQuotaNum(s.getQuotaNum());
                sw.setQuotaSize(s.getQuotaSize());
                sw.setResourceURI(s.getResourceURI());
                sw.setStorageSiteId(s.getStorageSiteId());
                sw.setCache(false);
                sw.setCurrentSize(s.getCurrentSize());
                sitesWarpper.add(sw);
            }
            for (StorageSite s : cachesites) {
                StorageSiteWrapper sw = new StorageSiteWrapper();
                CredentialWrapped cw = new CredentialWrapped();
//                cw.setStorageSitePassword(s.getCredential().getStorageSitePassword());
                cw.setStorageSitePassword("************");
                cw.setStorageSiteUsername(s.getCredential().getStorageSiteUsername());
                sw.setCredential(cw);
                sw.setCurrentNum(s.getCurrentNum());
                sw.setEncrypt(s.isEncrypt());
                sw.setQuotaNum(s.getQuotaNum());
                sw.setQuotaSize(s.getQuotaSize());
                sw.setResourceURI(s.getResourceURI());
                sw.setStorageSiteId(s.getStorageSiteId());
                sw.setCache(true);
                sw.setCurrentSize(s.getCurrentSize());
                sitesWarpper.add(sw);
            }
            return sitesWarpper;
        }
        return null;
    }
}
