/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.values.HrefList;
import com.ettrema.http.AccessControlledResource;
import com.ettrema.http.acl.Principal;
import java.io.IOException;
import java.util.*;
import nl.uva.cs.lobcder.auth.test.MyAuth;
import nl.uva.cs.lobcder.authdb.MyPrincipal;
import nl.uva.cs.lobcder.authdb.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.frontend.WebDavServlet;
import nl.uva.cs.lobcder.resources.*;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataResource implements PropFindableResource, Resource, AccessControlledResource {

    private ILogicalData logicalData;
    private final JDBCatalogue catalogue;
    private static final boolean debug = true;
    private Map<String, CustomProperty> properties;
    //Collection<Integer> roles = null;
    //private String uname;

    public WebDataResource(JDBCatalogue catalogue, ILogicalData logicalData) {
        this.logicalData = logicalData;
//        if (!logicalData.getType().equals(Constants.LOGICAL_DATA)) {
//            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
//        }
        this.catalogue = catalogue;
        properties = new HashMap<String, CustomProperty>();

    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public String getUniqueId() {
        debug("getUniqueId.");
        return String.valueOf(getLogicalData().getUID());
    }

    @Override
    public String getName() {
        debug("getName.");
        return getLogicalData().getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        debug("authenticate.\n"
                + "\t user: " + user
                + "\t password: " + password);


        String token = password;
        MyPrincipal principal = MyAuth.getInstance().checkToken(token);
        WebDavServlet.request().setAttribute("vph-user", principal);
        return principal;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {

        System.err.println("AUTHORIZE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!:  " + auth.getUser());
        System.err.println(WebDavServlet.request().getUserPrincipal());
        //Object permission = getPermissionForTheLogicalData();
        boolean authorized = true;
        if (authorized) {
            String absPath = null;
            String absURL = null;
            String acceptHeader = null;
            String fromAddress = null;
            String remoteAddr = null;
            String cnonce = null;
            String nc = null;
            String nonce = null;
            String password = null;
            String qop = null;
            String relm = null;
            String responseDigest = null;
            String uri = null;
            String user = null;
            Object tag = null;
            if (request != null) {
                absPath = request.getAbsolutePath();
                absURL = request.getAbsoluteUrl();
                acceptHeader = request.getAcceptHeader();
                fromAddress = request.getFromAddress();
                remoteAddr = request.getRemoteAddr();
            }
            if (auth != null) {
                cnonce = auth.getCnonce();
                nc = auth.getNc();
                nonce = auth.getNonce();
                password = auth.getPassword();
                qop = auth.getQop();
                relm = auth.getRealm();
                responseDigest = auth.getResponseDigest();
                uri = auth.getUri();
                user = auth.getUser();
                tag = auth.getTag();
            }
            debug("authorise. \n"
                    + "\t request.getAbsolutePath(): " + absPath + "\n"
                    + "\t request.getAbsoluteUrl(): " + absURL + "\n"
                    + "\t request.getAcceptHeader(): " + acceptHeader + "\n"
                    + "\t request.getFromAddress(): " + fromAddress + "\n"
                    + "\t request.getRemoteAddr(): " + remoteAddr + "\n"
                    + "\t auth.getCnonce(): " + cnonce + "\n"
                    + "\t auth.getNc(): " + nc + "\n"
                    + "\t auth.getNonce(): " + nonce + "\n"
                    + "\t auth.getPassword(): " + password + "\n"
                    + "\t auth.getQop(): " + qop + "\n"
                    + "\t auth.getRealm(): " + relm + "\n"
                    + "\t auth.getResponseDigest(): " + responseDigest + "\n"
                    + "\t auth.getUri(): " + uri + "\n"
                    + "\t auth.getUser(): " + user + "\n"
                    + "\t auth.getTag(): " + tag);
        }
        return authorized;
    }

    @Override
    public String getRealm() {
        debug("getRealm.");
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        return new Date(getLogicalData().getModifiedDate());
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        switch (request.getMethod()) {
            case GET:
                if (getLogicalData().isRedirectAllowed()) {
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
            System.err.println(this.getClass().getSimpleName() + "." + getLogicalData().getLDRI() + ": " + msg);
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

    /**
     * @return the catalogue
     */
    public JDBCatalogue getCatalogue() {
        return catalogue;
    }

    /**
     * @return the logicalData
     */
    public ILogicalData getLogicalData() {
        return logicalData;
    }

    /**
     * @return the logicalData
     */
    public void setLogicalData(ILogicalData logicalData) {
        this.logicalData = logicalData;
    }

    public MyPrincipal getPrincipal() {
        return (MyPrincipal) WebDavServlet.request().getAttribute("vph-user");
    }

//    public void isReadable(Permissions perm) throws NotAuthorizedException {
//        MyPrincipal principal = getPrincipal();
//        if (!principal.canRead(perm)) {
//            throw new NotAuthorizedException();
//        }
//
//    }
//
//    public void isWritable(Permissions perm) throws NotAuthorizedException {
//        MyPrincipal principal = getPrincipal();
//        if (!principal.canWrite(perm)) {
//            throw new NotAuthorizedException();
//        }
//    }

    @Override
    public String getPrincipalURL() {
        debug("getPrincipalURL");
        return getPrincipal().getUserId();
    }

    @Override
    public List<Priviledge> getPriviledges(Auth auth) {
        List<Priviledge> priviledgesList = new ArrayList<Priviledge>();
//        priviledgesList.add(Priviledge.ALL);
//        priviledgesList.add(Priviledge.BIND);
        priviledgesList.add(Priviledge.READ);
        priviledgesList.add(Priviledge.READ_ACL);
        priviledgesList.add(Priviledge.READ_CURRENT_USER_PRIVILEDGE);
//        priviledgesList.add(Priviledge.UNBIND);
//        priviledgesList.add(Priviledge.UNLOCK);
        priviledgesList.add(Priviledge.WRITE);
        priviledgesList.add(Priviledge.WRITE_ACL);
        priviledgesList.add(Priviledge.WRITE_CONTENT);
//        priviledgesList.add(Priviledge.WRITE_PROPERTIES);
        return priviledgesList;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        debug("getAccessControlList");

        // Do the mapping 
//        List<Integer> permArray = this.logicalData.getMetadata().getPermissionArray();
//        List<Priviledge> perm = new ArrayList<Priviledge>();
//        HashMap<Principal, List<Priviledge>> acl = new HashMap<Principal, List<Priviledge>>();
        throw new UnsupportedOperationException("Not supported yets.");
    }

    @Override
    public void setAccessControlList(Map<Principal, List<Priviledge>> map) {
        debug("setAccessControlList");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * - DAV:principal-collection-set - Collection of principals for this
     * server. For security and scalability reasons, a server MAY report only a
     * subset of the entire set of known principal collections, and therefore
     * clients should not assume they have retrieved an exhaustive listing. A
     * server MAY elect to report none of the principal collections it knows
     * about, in which case the property value would be empty.
     *
     * @return
     */
    @Override
    public HrefList getPrincipalCollectionHrefs() {
        HrefList list = new HrefList();
//        list.add("/users/");
//        return list;
        return null;
    }

    public PDRI createPDRI(long fileLength) throws CatalogueException {
//        Collection<MyStorageSite> sites = getCatalogue().getStorageSitesByUser(getPrincipal());
//        for (MyStorageSite s : sites) {
//            debug("Sites to choose from: " + s.getResourceURI());
//        }
        return new SimplePDRI(Long.valueOf(1), UUID.randomUUID().toString());
    }
}
