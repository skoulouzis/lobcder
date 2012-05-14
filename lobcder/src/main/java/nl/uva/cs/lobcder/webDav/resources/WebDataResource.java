/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import java.util.Collection;
import java.util.Date;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataResource implements PropFindableResource, Resource {

    private final ILogicalData logicalData;
    private final IDLCatalogue catalogue;
    private static final boolean debug = false;

    public WebDataResource(IDLCatalogue catalogue, ILogicalData logicalData) {
        this.logicalData = logicalData;
//        if (!logicalData.getType().equals(Constants.LOGICAL_DATA)) {
//            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
//        }
        this.catalogue = catalogue;
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        if (logicalData.getMetadata() != null && logicalData.getMetadata().getCreateDate() != null) {
            return new Date(logicalData.getMetadata().getCreateDate());
        }
        return null;
    }

    @Override
    public String getUniqueId() {
        return String.valueOf(logicalData.getUID());
    }

    @Override
    public String getName() {
        return logicalData.getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        try {
            debug("authenticate.\n"
                    + "\t user: " + user
                    + "\t password: " + password);
            Collection<IStorageSite> sites = logicalData.getStorageSites();
            if (sites == null || sites.isEmpty()) {
                sites = (Collection<IStorageSite>) catalogue.getSitesByUname(user);
                if (sites == null || sites.isEmpty()) {
                    debug("\t StorageSites for " + this.getName() + " are empty!");
                    throw new RuntimeException("StorageSites for " + this.getName() + " are empty!");
                }
                logicalData.setStorageSites(sites);
            }
        } catch (CatalogueException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        return user;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        debug("authorise.");
        return true;
    }

    @Override
    public String getRealm() {
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        if (logicalData.getMetadata() != null && logicalData.getMetadata().getModifiedDate() != null) {
            return new Date(logicalData.getMetadata().getModifiedDate());
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        switch (request.getMethod()) {
            case GET:
                if (logicalData.isRedirectAllowed()) {
                    //Replica selection algorithm 
                    return null;
                }
                return null;

            default:
                return null;
        }
    }

    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + logicalData.getLDRI() + ": " + msg);
        }
//        log.debug(msg);
    }
//    @Override
//    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
//        try {
//            Collection<IStorageSite> sites = logicalData.getStorageSites();
//            if (sites != null && !sites.isEmpty()) {
//                for (IStorageSite s : sites) {
//                    s.deleteVNode(logicalData.getLDRI());
//                }
//            }
//            catalogue.unregisterResourceEntry(logicalData);
//        } catch (CatalogueException ex) {
//            throw new BadRequestException(this, ex.toString());
//        } catch (VlException ex) {
//            throw new BadRequestException(this, ex.toString());
//        }
//    }
}
