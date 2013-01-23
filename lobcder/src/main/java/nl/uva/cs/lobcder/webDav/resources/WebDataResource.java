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
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.values.HrefList;
import com.bradmcevoy.http.webdav.PropertyMap;
import com.bradmcevoy.property.MultiNamespaceCustomPropertyResource;
import com.bradmcevoy.property.PropertySource.PropertyAccessibility;
import com.bradmcevoy.property.PropertySource.PropertyMetaData;
import com.bradmcevoy.property.PropertySource.PropertySetException;
import com.ettrema.http.AccessControlledResource;
import com.ettrema.http.acl.DavPrincipal;
import com.ettrema.http.acl.DavPrincipals.AbstractDavPrincipal;
import com.ettrema.http.acl.Principal;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.frontend.WebDavServlet;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.MyStorageSite;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.Constants;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataResource implements PropFindableResource, Resource, AccessControlledResource, MultiNamespaceCustomPropertyResource {

    private LogicalData logicalData;
    private final JDBCatalogue catalogue;
    private static final boolean debug = true;
    private final Map<QName, Object> customProperties = new HashMap<QName, Object>();
    private final Map<String, PropertyMap.StandardProperty> userPrivledges = new HashMap<String, PropertyMap.StandardProperty>();
    //Collection<Integer> roles = null;
    //private String uname;
    private static AuthI auth;

    static {
        try {
            String jndiName = "bean/auth";
            javax.naming.Context ctx = new InitialContext();
            if (ctx == null) {
                throw new Exception("JNDI could not create InitalContext ");
            }
            javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
            auth = (AuthI) envContext.lookup(jndiName);
        } catch (Exception ex) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public WebDataResource(JDBCatalogue catalogue, LogicalData logicalData) {
        this.logicalData = logicalData;
//        if (!logicalData.getType().equals(Constants.LOGICAL_DATA)) {
//            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
//        }
        this.catalogue = catalogue;

        //We can't init the data dist props here cause there is no authorization 
        //yet. So the getPrincipal will return null
//        initProps();

    }

    private void initProps() {
        if (customProperties.isEmpty()) {
            customProperties.put(Constants.DRI_SUPERVISED_PROP_NAME, String.valueOf(getLogicalData().getSupervised()));
            customProperties.put(Constants.DRI_CHECKSUM_PROP_NAME, String.valueOf(getLogicalData().getChecksum()));
            customProperties.put(Constants.DRI_LAST_VALIDATION_DATE_PROP_NAME, String.valueOf(getLogicalData().getLastValidationDate()));
            customProperties.put(Constants.DATA_DIST_PROP_NAME, null);

//            getLogicalData().getPermissions()
//            customProperties.put(Constants.DAV_ACL_PROP_NAME, null);
//            List<Priviledge> list = getPriviledges(null);
//            customProperties.put(Constants.DAV_CURRENT_USER_PRIVILAGE_SET_PROP_NAME, list);
//        if(getLogicalData().getSupervised() != null) {
//            DataDistProperty ddip = new DataDistProperty();
//            ddip.setFormattedValue(getLogicalData().getSupervised().toString());
//            customProperties.put(Constants.DATA_DIST_PROP_NAME, ddip);
//        }
        }
    }

    @Override
    public Date getCreateDate() {
//        debug("getCreateDate.");
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public String getUniqueId() {
//        debug("getUniqueId.");
        return String.valueOf(getLogicalData().getUID());
    }

    @Override
    public String getName() {
//        debug("getName.");
        return getLogicalData().getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
//        debug("authenticate.\n"
//                + "\t user: " + user
//                + "\t password: " + password);
        String token = password;
        MyPrincipal principal = auth.checkToken(token);
        WebDavServlet.request().setAttribute("vph-user", principal);
        return principal;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
//        System.err.println(WebDavServlet.request().getUserPrincipal());
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
//            debug("authorise. \n"
//                    + "\t request.getAbsolutePath(): " + absPath + "\n"
//                    + "\t request.getAbsoluteUrl(): " + absURL + "\n"
//                    + "\t request.getAcceptHeader(): " + acceptHeader + "\n"
//                    + "\t request.getFromAddress(): " + fromAddress + "\n"
//                    + "\t request.getRemoteAddr(): " + remoteAddr + "\n"
//                    + "\t auth.getCnonce(): " + cnonce + "\n"
//                    + "\t auth.getNc(): " + nc + "\n"
//                    + "\t auth.getNonce(): " + nonce + "\n"
//                    + "\t auth.getPassword(): " + password + "\n"
//                    + "\t auth.getQop(): " + qop + "\n"
//                    + "\t auth.getRealm(): " + relm + "\n"
//                    + "\t auth.getResponseDigest(): " + responseDigest + "\n"
//                    + "\t auth.getUri(): " + uri + "\n"
//                    + "\t auth.getUser(): " + user + "\n"
//                    + "\t auth.getTag(): " + tag
//                    + "\t\t Method = " + method.name());
        }
        return authorized;
    }

    @Override
    public String getRealm() {
//        debug("getRealm.");
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
//        debug("getModifiedDate.");
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
    public LogicalData getLogicalData() {
        return logicalData;
    }

    /**
     * @return the logicalData
     */
    public void setLogicalData(LogicalData logicalData) {
        this.logicalData = logicalData;
    }

    protected MyPrincipal getPrincipal() {
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
    /**
     * A "principal" is a distinct human or computational actor that initiates
     * access to resources. In this protocol, a principal is an HTTP resource
     * that represents such an actor.
     *
     * @return
     */
    @Override
    public String getPrincipalURL() {
        debug("getPrincipalURL");
        return getPrincipal().getUserId();
    }

    /**
     * DAV:current-user-privilege-set is a protected property containing the
     * exact set of privileges (as computed by the server) granted to the
     * currently authenticated HTTP user. See
     * http://www.webdav.org/specs/rfc3744.html#rfc.section.5.4
     *
     * @param auth
     * @return
     */
    @Override
    public List<Priviledge> getPriviledges(Auth auth) {
        MyPrincipal currentPrincipal = getPrincipal();
        List<Priviledge> priviledgesList = new ArrayList<Priviledge>();

        if (currentPrincipal.getUserId().equals(getLogicalData().getOwner())) {
            priviledgesList.add(Priviledge.ALL);
            return priviledgesList;
        }

        Set<String> currentRoles = currentPrincipal.getRoles();
        //We are supposed to get permissions for this resource for the current user
        Permissions p = getLogicalData().getPermissions();
        Set<String> readRoles = p.canRead();
        Set<String> writeRoles = p.canWrite();
        for (String r : currentRoles) {
            if (readRoles.contains(r)) {
                priviledgesList.add(Priviledge.READ);
                break;
            }
        }
        for (String r : currentRoles) {
            if (writeRoles.contains(r)) {
                priviledgesList.add(Priviledge.WRITE);
                break;
            }
        }
        return priviledgesList;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        debug("getAccessControlList");
        //Ceck if this principal can get the acl
        if (!getPrincipal().canRead(getLogicalData().getPermissions())) {
//            throw new RuntimeException(new NotAuthorizedException(this));
        }
        // Do the mapping
        DavPrincipal p = new AbstractDavPrincipal(getPrincipalURL()) {

            @Override
            public boolean matches(Auth auth, Resource current) {
                return true;
            }
        };
        List<Priviledge> perm = new ArrayList<Priviledge>();
        if (getPrincipal().canRead(getLogicalData().getPermissions())) {
            perm.add(Priviledge.READ);
        }
        if (getPrincipal().canWrite(getLogicalData().getPermissions())) {
            perm.add(Priviledge.WRITE);
        }
        HashMap<Principal, List<Priviledge>> acl = new HashMap<Principal, List<Priviledge>>();
        acl.put(p, perm);

        Set<String> readRoles = getLogicalData().getPermissions().canRead();
        Set<String> writeRoles = getLogicalData().getPermissions().canWrite();
        for (String r : getPrincipal().getRoles()) {
            perm = new ArrayList<Priviledge>();
            p = new AbstractDavPrincipal(r) {

                @Override
                public boolean matches(Auth auth, Resource current) {
                    return true;
                }
            };
            if (readRoles.contains(r)) {
                perm.add(Priviledge.READ);
            }
            acl.put(p, perm);
        }
        for (String r : getPrincipal().getRoles()) {
            perm = new ArrayList<Priviledge>();
            p = new AbstractDavPrincipal(r) {

                @Override
                public boolean matches(Auth auth, Resource current) {
                    return true;
                }
            };
            if (writeRoles.contains(r)) {
                perm.add(Priviledge.WRITE);
            }
            acl.put(p, perm);
        }

        return acl;
    }

    @Override
    public void setAccessControlList(Map<Principal, List<Priviledge>> map) {
        debug("setAccessControlList");

        if (!getLogicalData().getPermissions().getOwner().equals(getPrincipal().getUserId())) {
            throw new RuntimeException(new NotAuthorizedException(this));
        }
        Set<Principal> principals = map.keySet();
        for (Principal p : principals) {
            List<Priviledge> davPerm = map.get(p);
            MyPrincipal princpal = new MyPrincipal(p.getIdenitifer().getValue(), null);

//            Permissions perm = new Permissions(princpal);
//            getLogicalData().setPermissions(perm);
        }
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
        list.add("");
        return list;
    }

    public PDRI createPDRI(long fileLength, String fileName, Connection connection) throws CatalogueException, IOException {
        Collection<MyStorageSite> sites = getCatalogue().getStorageSitesByUser(getPrincipal(), connection);
        if (!sites.isEmpty()) {
//            MyStorageSite site = sites.iterator().next();
            MyStorageSite site = selectBestSite(sites);
            return PDRIFactory.getFactory().createInstance(UUID.randomUUID().toString() + "-" + fileName,
                    site.getStorageSiteId(), site.getResourceURI(),
                    site.getCredential().getStorageSiteUsername(), site.getCredential().getStorageSitePassword());
        } else {
            return null;
        }
    }

    private void initDataDist() throws CatalogueException, SQLException, NotAuthorizedException, UnknownHostException {
        Connection connection = getCatalogue().getConnection();
        connection.setAutoCommit(false);
        StringBuilder sb = new StringBuilder();
        if (getLogicalData().isFolder()) {
            List<? extends WebDataResource> children = (List<? extends WebDataResource>) ((WebDataDirResource) (this)).getChildren();
            sb.append("[");
            for (WebDataResource r : children) {
                Collection<PDRI> pdris = getCatalogue().getPdriByGroupId(r.getLogicalData().getPdriGroupId(), connection);
                for (PDRI p : pdris) {
                    sb.append(p.getHost());
                    sb.append(",");
                }
            }
        } else {
            Collection<PDRI> pdris = getCatalogue().getPdriByGroupId(getLogicalData().getPdriGroupId(), connection);
            sb.append("[");
            for (PDRI p : pdris) {
                sb.append(p.getHost());
                sb.append(",");
            }
        }
        sb.replace(sb.lastIndexOf(","), sb.length(), "");
        sb.append("]");
        customProperties.put(Constants.DATA_DIST_PROP_NAME, sb.toString());
    }

    private MyStorageSite selectBestSite(Collection<MyStorageSite> sites) {
        for (MyStorageSite s : sites) {
            return s;
        }
        return null;
    }

    @Override
    public Object getProperty(QName qname) throws RuntimeException {
        try {
            debug("getProperty: " + qname);
            initProps();
            if (qname.toString().equals(Constants.DATA_DIST_PROP_NAME.toString())) {
                try {
                    initDataDist();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                } catch (NotAuthorizedException ex) {
                    throw new RuntimeException(ex);
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (customProperties.containsKey(qname)) {
                return customProperties.get(qname);
            } else {
                return PropertyMetaData.UNKNOWN;
            }
        } catch (CatalogueException ex) {
            throw new RuntimeException(ex);
        }
//        return PropertyMetaData.UNKNOWN;
    }

    @Override
    public void setProperty(QName qname, Object o) throws PropertySetException, NotAuthorizedException {
        if (!getPrincipal().canWrite(getLogicalData().getPermissions())) {
            throw new NotAuthorizedException(this);
        }
        debug("setProperty: " + qname + " " + o);
        if (o != null) {
            customProperties.put(qname, (String) o);
            updateCatalogue(qname, (String) o);
        }
    }

    @Override
    public PropertyMetaData getPropertyMetaData(QName qname) {
        try {
            debug("getPropertyMetaData: " + qname);
            if (qname.equals(Constants.DATA_DIST_PROP_NAME)) {
                try {
                    initDataDist();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                } catch (NotAuthorizedException ex) {
                    throw new RuntimeException(ex);
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            }
            initProps();
            if (customProperties.containsKey(qname)) {
                return new PropertyMetaData(PropertyAccessibility.WRITABLE, String.class);
            } else {
                return PropertyMetaData.UNKNOWN;
            }
        } catch (CatalogueException ex) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return PropertyMetaData.UNKNOWN;
    }

    @Override
    public List<QName> getAllPropertyNames() {
        ArrayList<QName> list = new ArrayList<QName>();
        debug("getAllPropertyNames: ");
        initProps();
        list.addAll(customProperties.keySet());
        for (QName n : list) {
            debug("Names: " + n);
        }
        return list;
    }

    private void updateCatalogue(QName qname, String value) {
        if (qname.equals(Constants.DRI_SUPERVISED_PROP_NAME)) {
            getLogicalData().updateSupervised(Boolean.valueOf(value));
        } else if (qname.equals(Constants.DRI_CHECKSUM_PROP_NAME)) {
            getLogicalData().updateChecksum(Long.valueOf(value));
        } else if (qname.equals(Constants.DRI_LAST_VALIDATION_DATE_PROP_NAME)) {
            getLogicalData().updateLastValidationDate(Long.valueOf(value));
        }
    }
}
