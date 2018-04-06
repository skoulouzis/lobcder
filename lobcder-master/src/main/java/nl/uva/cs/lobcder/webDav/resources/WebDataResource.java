/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.*;
import static io.milton.http.Request.Method.GET;
import static io.milton.http.Request.Method.POST;
import static io.milton.http.Request.Method.PUT;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.LockedException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.http.values.HrefList;
import io.milton.principal.DavPrincipals;
import io.milton.principal.Principal;
import io.milton.property.PropertySource;
import io.milton.resource.*;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.DesEncrypter;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.replication.policy.ReplicationPolicy;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author S. Koulouzis
 */
public class WebDataResource implements PropFindableResource, Resource,
        AccessControlledResource, MultiNamespaceCustomPropertyResource, LockableResource {

    private final LogicalData logicalData;
    private final JDBCatalogue catalogue;
    private final Path path;
    protected final List<AuthI> authList;
//    protected final AuthI auth2;
    private static final ThreadLocal<MyPrincipal> principalHolder = new ThreadLocal<>();
    protected String fromAddress;
    protected Map<String, String> mimeTypeMap = new HashMap<>();
    private static boolean redirectPosts = false;

    public WebDataResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull List<AuthI> authList) {
        this.authList = authList;
//        this.auth2 = auth2;
        this.logicalData = logicalData;
        this.catalogue = catalogue;
        this.path = path;
        mimeTypeMap.put("mp4", "video/mp4");
        mimeTypeMap.put("pdf", "application/pdf");
        mimeTypeMap.put("tex", "application/x-tex");
        mimeTypeMap.put("log", "text/plain");
        mimeTypeMap.put("png", "image/png");
        mimeTypeMap.put("aux", "text/plain");
        mimeTypeMap.put("bbl", "text/plain");
        mimeTypeMap.put("blg", "text/plain");
        try {
            redirectPosts = PropertiesHelper.doRedirectPosts();
        } catch (IOException ex) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Date getCreateDate() {
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public String getUniqueId() {
        return String.valueOf(getLogicalData().getUid());
    }

    @Override
    public String getName() {
        return getLogicalData().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        String token = password;
        MyPrincipal principal = null;

        for (AuthI a : authList) {
            principal = a.checkToken(user, token);
            if (principal != null) {
                break;
            }
        }

//        if (auth2 != null) {
//            principal = auth2.checkToken(token);
//        }
//        if (principal == null) {
//            principal = auth1.checkToken(token);
//        }
        if (principal != null) {
            principalHolder.set(principal);
//            Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "getUserId: {0}", principal.getUserId());
//            Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "getRolesStr: {0}", principal.getRolesStr());
            String msg = "From: " + fromAddress + " user: " + principal.getUserId() + " password: XXXX";
            Logger.getLogger(WebDataResource.class.getName()).log(Level.INFO, msg);

        }
        try {
            getCatalogue().updateAccessTime(getLogicalData().getUid());
        } catch (SQLException ex) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return principal;
    }

    MyPrincipal getPrincipal() {
        return principalHolder.get();
    }

    protected Permissions getPermissions() throws SQLException {
        return getCatalogue().getPermissions(getLogicalData().getUid(), getLogicalData().getOwner());
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {

        try {
            if (auth == null) {
                return false;
            }
            fromAddress = request.getFromAddress();
            String msg = "From: " + fromAddress + " User: " + getPrincipal().getUserId() + " Method: " + method;
            Logger.getLogger(WebDataResource.class.getName()).log(Level.INFO, msg);
            LogicalData parentLD;
            Permissions p;
            switch (method) {
                case ACL:
                    return getPrincipal().canWrite(getPermissions());
                case HEAD:
                    return true;
                case PROPFIND:
                    return getPrincipal().canRead(getPermissions());
                case PROPPATCH:
                    return getPrincipal().canWrite(getPermissions());
                case MKCALENDAR:
                    return false;
                case COPY:
                    return getPrincipal().canRead(getPermissions());
                case MOVE:
                    return true;
                case LOCK:
                    return getPrincipal().canWrite(getPermissions());
                case UNLOCK:
                    return getPrincipal().canWrite(getPermissions());
                case DELETE:
                    parentLD = getCatalogue().getLogicalDataByUid(getLogicalData().getParentRef());
                    p = getCatalogue().getPermissions(parentLD.getUid(), parentLD.getOwner());
                    return getPrincipal().canWrite(p);
                case GET:
                    return getPrincipal().canRead(getPermissions());
                case OPTIONS:
                    return getPrincipal().canRead(getPermissions());
                case POST:
                    return getPrincipal().canWrite(getPermissions());
                case PUT:
                    return getPrincipal().canWrite(getPermissions());
                case TRACE:
                    return false;
                case CONNECT:
                    return false;
                case REPORT:
                    return false;
                default:
                    return true;
            }
        } catch (Throwable th) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, "Exception in authorize for a resource " + getPath(), th);
            return false;
        }
//        return false;
    }

    @Override
    public String getRealm() {
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        return new Date(getLogicalData().getModifiedDate());
    }

    String getUserlUrlPrefix() {
        return "http://lobcder.net/user/";
    }

    String getRoleUrlPrefix() {
        return "http://lobcder.net/role/";
    }

    @Override
    public String getPrincipalURL() {
        String principalURL = getUserlUrlPrefix() + getPrincipal().getUserId();
        Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "getPrincipalURL for {0}: {1}", new Object[]{getPath(), principalURL});
        return principalURL;
    }

    @Override
    public List<Priviledge> getPriviledges(Auth auth) {
        final MyPrincipal currentPrincipal = getPrincipal();
        List<Priviledge> perm = new ArrayList<>();
        if (currentPrincipal.getUserId().equals(getLogicalData().getOwner())) {
            perm.add(Priviledge.ALL);
            return perm;
        }
        Set<String> currentRoles = currentPrincipal.getRoles();
        //We are supposed to get permissions for this resource for the current user
        Permissions p;
        try {
            p = getPermissions();
        } catch (SQLException e) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, "Could not get Permissions for resource " + getPath(), e);
            return perm;
        }
        Set<String> readRoles = p.getRead();
        Set<String> writeRoles = p.getWrite();
        readRoles.retainAll(currentRoles);
        if (!readRoles.isEmpty()) {
            perm.add(Priviledge.READ);
            perm.add(Priviledge.READ_ACL);
            perm.add(Priviledge.READ_CONTENT);
            perm.add(Priviledge.READ_CURRENT_USER_PRIVILEDGE);
            perm.add(Priviledge.READ_PROPERTIES);
        }
        writeRoles.retainAll(currentRoles);
        if (!writeRoles.isEmpty()) {
            perm.add(Priviledge.WRITE);
            perm.add(Priviledge.BIND);
            perm.add(Priviledge.UNBIND);
            perm.add(Priviledge.UNLOCK);
            perm.add(Priviledge.WRITE_ACL);
            perm.add(Priviledge.WRITE_CONTENT);
            perm.add(Priviledge.WRITE_PROPERTIES);
        }
        return perm;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "getAccessControlList for {0}", getPath());
        Permissions resourcePermission;
        HashMap<Principal, List<Priviledge>> acl = new HashMap<>();
        try {
            // Do the mapping
            Principal p = new DavPrincipals.AbstractDavPrincipal(getPrincipalURL()) {
                @Override
                public boolean matches(Auth auth, Resource current) {
                    return true;
                }
            };
            resourcePermission = getPermissions();
            List<Priviledge> perm = new ArrayList<>();
            if (getPrincipal().canRead(resourcePermission)) {
                perm.add(Priviledge.READ);
                perm.add(Priviledge.READ_ACL);
                perm.add(Priviledge.READ_CONTENT);
                perm.add(Priviledge.READ_CURRENT_USER_PRIVILEDGE);
                perm.add(Priviledge.READ_PROPERTIES);
            }
            if (getPrincipal().canWrite(resourcePermission)) {
                perm.add(Priviledge.WRITE);
                perm.add(Priviledge.BIND);
                perm.add(Priviledge.UNBIND);
                perm.add(Priviledge.UNLOCK);
                perm.add(Priviledge.WRITE_ACL);
                perm.add(Priviledge.WRITE_CONTENT);
                perm.add(Priviledge.WRITE_PROPERTIES);
            }
            acl.put(p, perm);
            for (String r : resourcePermission.getRead()) {
                perm = new ArrayList<>();
                p = new DavPrincipals.AbstractDavPrincipal(getRoleUrlPrefix() + r) {
                    @Override
                    public boolean matches(Auth auth, Resource current) {
                        return true;
                    }
                };
                perm.add(Priviledge.READ);
                perm.add(Priviledge.READ_ACL);
                perm.add(Priviledge.READ_CONTENT);
                perm.add(Priviledge.READ_CURRENT_USER_PRIVILEDGE);
                perm.add(Priviledge.READ_PROPERTIES);
                acl.put(p, perm);
            }
            for (String r : resourcePermission.getWrite()) {
                perm = new ArrayList<>();
                p = new DavPrincipals.AbstractDavPrincipal(getRoleUrlPrefix() + r) {
                    @Override
                    public boolean matches(Auth auth, Resource current) {
                        return true;
                    }
                };
                perm.add(Priviledge.WRITE);
                perm.add(Priviledge.BIND);
                perm.add(Priviledge.UNBIND);
                perm.add(Priviledge.UNLOCK);
                perm.add(Priviledge.WRITE_ACL);
                perm.add(Priviledge.WRITE_CONTENT);
                perm.add(Priviledge.WRITE_PROPERTIES);
                acl.put(p, perm);
            }
        } catch (SQLException e) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, "Cannot read permissions for resource " + getPath(), e);
        }
        return acl;
    }

    @Override
    public void setAccessControlList(Map<Principal, List<Priviledge>> map) {
        Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "PLACEHOLDER setAccessControlList() for {0}", getPath());

        for (Map.Entry<Principal, List<Priviledge>> me : map.entrySet()) {
            Principal principal = me.getKey();
            for (Priviledge priviledge : me.getValue()) {
                Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "Set priveledges {0} for {1}", new Object[]{priviledge, principal});
                //String id = principal.getIdenitifer().getValue();
                //id = id.substring(id.lastIndexOf("/") + 1);
            }
        }
    }

    @Override
    public HrefList getPrincipalCollectionHrefs() {
        HrefList list = new HrefList();
        list.add("");
        return list;
    }

    protected PDRI createPDRI(long fileLength, String fileName, Connection connection) throws SQLException, NoSuchAlgorithmException, IOException {
//        Collection<StorageSite> cacheSS = getCatalogue().getCacheStorageSites(connection);
        Collection<StorageSite> cacheSS = getCatalogue().getStorageSites(connection, Boolean.TRUE, getPrincipal().isAdmin());
        String nameWithoutSpace = fileName.replaceAll(" ", "_");
        if (cacheSS == null || cacheSS.isEmpty()) {
            return new CachePDRI(UUID.randomUUID().toString() + "-" + nameWithoutSpace);
        } else {
            StorageSite ss = cacheSS.iterator().next();
            PDRIDescr pdriDescr = new PDRIDescr(
                    UUID.randomUUID().toString() + "-" + nameWithoutSpace,
                    ss.getStorageSiteId(),
                    ss.getResourceURI(),
                    ss.getCredential().getStorageSiteUsername(),
                    ss.getCredential().getStorageSitePassword(), ss.isEncrypt(), DesEncrypter.generateKey(), null, null, ss.isCache());
            return PDRIFactory.getFactory().createInstance(pdriDescr, true);
        }
    }

    @Override
    public Object getProperty(QName qname) {
        try {
            if (qname.equals(Constants.DATA_DIST_PROP_NAME)) {
                return getDataDistString();
            } else if (qname.equals(Constants.DRI_SUPERVISED_PROP_NAME)) {
                return String.valueOf(getLogicalData().getSupervised());
            } else if (qname.equals(Constants.DRI_CHECKSUM_PROP_NAME)) {
                return String.valueOf(getLogicalData().getChecksum());
            } else if (qname.equals(Constants.DRI_LAST_VALIDATION_DATE_PROP_NAME)) {
                return String.valueOf(getLogicalData().getLastValidationDate());
            } else if (qname.equals(Constants.DRI_STATUS_PROP_NANE)) {
                return getLogicalData().getStatus();
            } else if (qname.equals(Constants.DAV_CURRENT_USER_PRIVILAGE_SET_PROP_NAME)) {
                //List<Priviledge> list = getPriviledges(null);
                return "";
            } else if (qname.equals(Constants.DAV_ACL_PROP_NAME)) {
                //List<Priviledge> list = getPriviledges(null);
                return "";
            } else if (qname.equals(Constants.DESCRIPTION_PROP_NAME)) {
                return getLogicalData().getDescription();
            } else if (qname.equals(Constants.DATA_LOC_PREF_NAME)) {
                return getDataLocationPreferencesString();
            } else if (qname.equals(Constants.ENCRYPT_PROP_NAME)) {
                return getEcryptionString();
            } else if (qname.equals(Constants.AVAIL_STORAGE_SITES_PROP_NAME)) {
                return getAvailStorageSitesString(getPrincipal().isAdmin());
            } else if (qname.equals(Constants.TTL)) {
                return getLogicalData().getTtlSec();
            } else if (qname.equals(Constants.REPLICATION_QUEUE)) {
                if (getPrincipal().isAdmin()) {
                    return getReplicationQueueString();
                }
            } else if (qname.equals(Constants.REPLICATION_QUEUE_LEN)) {
//                if (getPrincipal().isAdmin()) {
                return getReplicationQueueLen();
//                }
            } else if (qname.equals(Constants.REPLICATION_QUEUE_SIZE)) {
//                if (getPrincipal().isAdmin()) {
                return getReplicationQueueSize();
//                }
            }
            return PropertySource.PropertyMetaData.UNKNOWN;
        } catch (Throwable th) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, "Exception in getProperty() for resource " + getPath(), th);
            return PropertySource.PropertyMetaData.UNKNOWN;
        }
    }

    @Override
    public void setProperty(QName qname, Object o) throws PropertySource.PropertySetException, NotAuthorizedException {
        Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "setProperty for resource {0} : {1} = {2}", new Object[]{getPath(), qname, o});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                if (o != null) {
                    String value = (String) o;
                    if (qname.equals(Constants.DRI_SUPERVISED_PROP_NAME)) {
                        Boolean v = Boolean.valueOf(value);
                        getLogicalData().setSupervised(v);
                        getCatalogue().setLogicalDataSupervised(getLogicalData().getUid(), v, connection);
                    } else if (qname.equals(Constants.DRI_CHECKSUM_PROP_NAME)) {
                        getLogicalData().setChecksum(value);
                        getCatalogue().setFileChecksum(getLogicalData().getUid(), value, connection);
                    } else if (qname.equals(Constants.DRI_LAST_VALIDATION_DATE_PROP_NAME)) {
                        Long v = Long.valueOf(value);
                        getLogicalData().setLastValidationDate(v);
                        getCatalogue().setLastValidationDate(getLogicalData().getUid(), v, connection);
                    } else if (qname.equals(Constants.DRI_STATUS_PROP_NANE)) {
                        getLogicalData().setStatus(value);
                        getCatalogue().setDriStatus(getLogicalData().getUid(), value, connection);
                    } else if (qname.equals(Constants.DESCRIPTION_PROP_NAME)) {
                        String v = value;
                        getLogicalData().setDescription(v);
                        getCatalogue().setDescription(getLogicalData().getUid(), v, connection);
                    } else if (qname.equals(Constants.DATA_LOC_PREF_NAME)) {
                        setDataLocationPref(value, connection);
                    } else if (qname.equals(Constants.ENCRYPT_PROP_NAME)) {
                        setEncryptionPropertyValues(value, connection);
                    } else if (qname.equals(Constants.TTL)) {
                        String v = value;
                        getLogicalData().setTtlSec(Integer.valueOf(v));
                        getCatalogue().setTTL(getLogicalData().getUid(), Integer.valueOf(v), connection);
                    }
                    connection.commit();
                }
            } catch (SQLException | NumberFormatException e) {
                connection.rollback();
                throw new PropertySource.PropertySetException(Response.Status.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } catch (SQLException e) {
            throw new PropertySource.PropertySetException(Response.Status.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public PropertySource.PropertyMetaData getPropertyMetaData(QName qname) {
        if (qname.equals(Constants.DATA_DIST_PROP_NAME)
                || qname.equals(Constants.REPLICATION_QUEUE)
                || qname.equals(Constants.REPLICATION_QUEUE_LEN)) {
            return new PropertySource.PropertyMetaData(PropertySource.PropertyAccessibility.READ_ONLY, String.class);
        }
//        if (qname.equals(Constants.DRI_CHECKSUM_PROP_NAME)) {
//            return new PropertySource.PropertyMetaData(PropertySource.PropertyAccessibility.READ_ONLY, String.class);
//        }
        for (QName n : Constants.PROP_NAMES) {
            if (n.equals(qname) && !n.equals(Constants.DATA_DIST_PROP_NAME)) {
                return new PropertySource.PropertyMetaData(PropertySource.PropertyAccessibility.WRITABLE, String.class);
            }
        }
        return PropertySource.PropertyMetaData.UNKNOWN;
    }

    @Override
    public List<QName> getAllPropertyNames() {
        return Arrays.asList(Constants.PROP_NAMES);
    }

    @Override
    public LockResult lock(LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException, PreConditionFailedException, LockedException {
        if (getCurrentLock() != null) {
            throw new LockedException(this);
        }
        LockToken lockToken = new LockToken(UUID.randomUUID().toString(), lockInfo, timeout);
        Long lockTimeout;
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                getLogicalData().setLockTokenID(lockToken.tokenId);

                getCatalogue().setLockTokenID(getLogicalData().getUid(), getLogicalData().getLockTokenID(), connection);
                getLogicalData().setLockScope(lockToken.info.scope.toString());
                getCatalogue().setLockScope(getLogicalData().getUid(), getLogicalData().getLockScope(), connection);
                getLogicalData().setLockType(lockToken.info.type.toString());
                getCatalogue().setLockType(getLogicalData().getUid(), getLogicalData().getLockType(), connection);
                getLogicalData().setLockedByUser(lockToken.info.lockedByUser);
                getCatalogue().setLockByUser(getLogicalData().getUid(), getLogicalData().getLockedByUser(), connection);
                getLogicalData().setLockDepth(lockToken.info.depth.toString());
                getCatalogue().setLockDepth(getLogicalData().getUid(), getLogicalData().getLockDepth(), connection);
                lockTimeout = lockToken.timeout.getSeconds();
                if (lockTimeout == null) {
                    lockTimeout = Long.valueOf(System.currentTimeMillis() + Constants.LOCK_TIME);
                }
                getLogicalData().setLockTimeout(lockTimeout);

                getCatalogue().setLockTimeout(getLogicalData().getUid(), lockTimeout, connection);
                connection.commit();
                return LockResult.success(lockToken);
            } catch (Exception ex) {
                Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new PreConditionFailedException(this);
            }
        } catch (SQLException e) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, e);
            throw new PreConditionFailedException(this);
        }

    }

    @Override
    public LockResult refreshLock(String token) throws NotAuthorizedException, PreConditionFailedException {
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                if (getLogicalData().getLockTokenID() == null) {
                    throw new RuntimeException("not locked");
                } else if (!getLogicalData().getLockTokenID().equals(token)) {
                    throw new RuntimeException("invalid lock id");
                }
                getLogicalData().setLockTimeout(System.currentTimeMillis() + Constants.LOCK_TIME);

                getCatalogue().setLockTimeout(getLogicalData().getUid(), getLogicalData().getLockTimeout(), connection);
                LockInfo lockInfo = new LockInfo(LockInfo.LockScope.valueOf(getLogicalData().getLockScope()),
                        LockInfo.LockType.valueOf(getLogicalData().getLockType()), getLogicalData().getLockedByUser(),
                        LockInfo.LockDepth.valueOf(getLogicalData().getLockDepth()));
                LockTimeout lockTimeOut = new LockTimeout(getLogicalData().getLockTimeout());
                LockToken lockToken = new LockToken(token, lockInfo, lockTimeOut);
                connection.commit();
                return LockResult.success(lockToken);
            } catch (Exception ex) {
                Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new PreConditionFailedException(this);
            }
        } catch (SQLException e) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, e);
            throw new PreConditionFailedException(this);
        }
    }

    @Override
    public void unlock(String token) throws NotAuthorizedException, PreConditionFailedException {
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                String tokenID = getLogicalData().getLockTokenID();
                if (tokenID == null || tokenID.length() <= 0) {
                    return;
                } else {
                    if (tokenID.startsWith("<") && tokenID.endsWith(">") && !token.startsWith("<") && !token.endsWith(">")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("<").append(token).append(">");
                        token = sb.toString();
                    }
                    if (!tokenID.startsWith("<") && !tokenID.endsWith(">") && token.startsWith("<") && token.endsWith(">")) {
                        token = token.replaceFirst("<", "");
                        token = token.replaceFirst(">", "");
                    }

                    if (!tokenID.equals(token)) {
                        throw new PreConditionFailedException(this);
                    }
                }
                getCatalogue().setLockTokenID(getLogicalData().getUid(), null, connection);
                connection.commit();
                getLogicalData().setLockTokenID(null);
                getLogicalData().setLockScope(null);
                getLogicalData().setLockType(null);
                getLogicalData().setLockedByUser(null);
                getLogicalData().setLockDepth(null);
                getLogicalData().setLockTimeout(null);
            } catch (Exception ex) {
                Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new PreConditionFailedException(this);
            }
        } catch (SQLException e) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, e);
            throw new PreConditionFailedException(this);
        }
    }

    @Override
    public LockToken getCurrentLock() {
        if (getLogicalData().getLockTokenID() == null) {
            return null;
        } else {
            LockInfo lockInfo = new LockInfo(LockInfo.LockScope.valueOf(getLogicalData().getLockScope()),
                    LockInfo.LockType.valueOf(getLogicalData().getLockType()),
                    getLogicalData().getLockedByUser(), LockInfo.LockDepth.valueOf(getLogicalData().getLockDepth()));
            LockTimeout lockTimeOut = new LockTimeout(getLogicalData().getLockTimeout());
            return new LockToken(getLogicalData().getLockTokenID(), lockInfo, lockTimeOut);
        }
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        switch (request.getMethod()) {
            case PUT:
            case POST:
                if (!redirectPosts) {
                    return null;
                }
                String redirect = null;
                try {
                    if (!canRedirect(request)) {
                        return null;
                    }
                    Map<Long, Pair<WebDataFileResource, Long>> resources = createResouses(request);
//                    lockResources(resources);
                    Map<String, Pair<Long, Collection<Long>>> storageMap = getStorageMap(resources);

                    StringBuilder sb = new StringBuilder();
                    Set<String> keys = storageMap.keySet();
                    for (String k : keys) {
                        sb.append("file_name=").append(k).append("/");
                        Pair pair = storageMap.get(k);
                        Long fileUid = (Long) pair.getLeft();
                        sb.append("file_uid=").append(fileUid).append("/");

                        Long pdriGroupUid = resources.get(fileUid).getRight();
                        sb.append("pdrigroup_uid=").append(pdriGroupUid).append("/");

                        Collection<Long> ssids = (Collection<Long>) pair.getRight();
                        for (Long ssid : ssids) {
                            sb.append("ss_id=").append(ssid).append("/");
                        }
                        sb.append("&");
                    }
                    sb.deleteCharAt(sb.length() - 1);

                    String folder = request.getAbsolutePath();
                    if (!folder.endsWith("/")) {
                        folder += "/";
                    }
                    redirect = "http://localhost:8080/lobcder-worker" + folder + "?" + sb.toString();
                } catch (Exception ex) {
                    Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
                }
                return redirect;
            default:
                return null;
        }
//        return null;
    }

    private String getDataDistString() throws SQLException {
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                connection.commit();
                StringBuilder sb = new StringBuilder();
                if (getLogicalData().isFolder()) {
                    List<? extends WebDataResource> children = (List<? extends WebDataResource>) ((WebDataDirResource) (this)).getChildren();
                    if (children != null) {
                        sb.append("[");
                        for (WebDataResource r : children) {
                            if (r instanceof WebDataFileResource) {
//                                    sb.append("'").append(r.getName()).append("' : [");
                                sb.append(r.getName()).append(" : [");
                                Collection<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(r.getLogicalData().getPdriGroupId(), connection);
                                for (PDRIDescr p : pdris) {
//                                        sb.append("'").append(p.getResourceUrl()).append("/").append(p.getName()).append("',");
                                    sb.append(p.getResourceUrl()).append("/").append(p.getName()).append(",");
                                }
                                sb.replace(sb.lastIndexOf(","), sb.length(), "").append("],");
                            }
                        }
                    }

                } else {
                    Collection<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId(), connection);
                    if (pdris != null) {
                        sb.append("[");
                        for (PDRIDescr p : pdris) {
//                                sb.append("'").append(p.getResourceUrl()).append("/").append(p.getName()).append("'");
                            sb.append(p.getResourceUrl());
                            if (!sb.toString().endsWith("/")) {
                                sb.append("/");
                            }
                            sb.append(p.getName());
                            sb.append(",");
                        }
                    }

                }
                if (sb.toString().contains(",")) {
                    sb.replace(sb.lastIndexOf(","), sb.length(), "");
                }
                sb.append("]");
                return sb.toString();
            } catch (NotAuthorizedException | SQLException e) {
                connection.rollback();
            }
        }
        return null;
    }

    private String getEcryptionString() throws NotAuthorizedException, SQLException {
        try (Connection connection = getCatalogue().getConnection()) {
            StringBuilder sb = new StringBuilder();
            if (getLogicalData().isFolder()) {
                List<? extends WebDataResource> children = (List<? extends WebDataResource>) ((WebDataDirResource) (this)).getChildren();
                sb.append("[");
                if (children != null && !children.isEmpty()) {
                    for (WebDataResource r : children) {
                        if (r instanceof WebDataFileResource) {
                            sb.append("'").append(r.getName()).append("' : [");
                            Collection<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(r.getLogicalData().getPdriGroupId(), connection);
                            for (PDRIDescr p : pdris) {
                                sb.append("[");
                                sb.append(p.getResourceUrl()).append(",");
                                sb.append(p.getEncrypt());
                                sb.append("],");
                            }
                            sb.replace(sb.lastIndexOf(","), sb.length(), "").append("],");
                        }
                    }
                }
            } else {
                Collection<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId(), connection);
                sb.append("[");
                for (PDRIDescr p : pdris) {
                    sb.append("[");
                    sb.append(p.getResourceUrl());
                    sb.append(",");
                    sb.append(p.getEncrypt());
                    sb.append("]");
                    sb.append(",");
                }
            }
            if (sb.toString().contains(",")) {
                sb.replace(sb.lastIndexOf(","), sb.length(), "");
            }
            sb.append("]");
            connection.commit();

            return sb.toString();
        }
    }

    private String getAvailStorageSitesString(Boolean includePrivate) throws SQLException {
        try (Connection connection = getCatalogue().getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (String s : getAvailStorageSitesStr(includePrivate, connection)) {
                sb.append(s).append(",");
            }
            sb.replace(sb.lastIndexOf(","), sb.length(), "");
            sb.append("]");
            return sb.toString();
        }
    }

    private void setEncryptionPropertyValues(String value, Connection connection) {
        String v = value;
///                        HashMap<String, Boolean> hostEncryptMap = new HashMap<>();
//                        String[] parts = v.split("[\\[\\]]");
//                        for (String p : parts) {
//                            Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "Parts: {0}", p);
//                            if (!p.isEmpty()) {
//                                String[] hostEncryptValue = p.split(",");
//                                if (hostEncryptValue.length == 2) {
//                                    String hostStr = hostEncryptValue[0];
//                                    URI uri;
//                                    try {
//                                        uri = new URI(hostStr);
//                                        String host = uri.getScheme();
//                                        host += "://" + uri.getHost();
//                                        String encrypt = hostEncryptValue[1];
//                                        hostEncryptMap.put(host, Boolean.valueOf(encrypt));
//                                    } catch (URISyntaxException ex) {
//                                        //Wrong URI syntax, don't add it 
//                                    }
//                                }
//                            }
//                        }
//                        List<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId(), connection);
//                        List<PDRIDescr> pdrisToUpdate = new ArrayList<PDRIDescr>();
//                        for (PDRIDescr p : pdris) {
//                            URI uri = new URI(p.getResourceUrl());
//                            String host = uri.getScheme();
//                            host += "://" + uri.getHost();
//                            if (hostEncryptMap.containsKey(host)) {
//                                p.setEncrypt(hostEncryptMap.get(host));
//                                pdrisToUpdate.add(p);
//                            }}
//                        }
//                        if (!hostEncryptMap.isEmpty()) {
//                            getCatalogue().updateStorageSites(hostEncryptMap, connection);
//                        }
//                        if (!pdrisToUpdate.isEmpty()) {
//                            getCatalogue().updatePdris(pdrisToUpdate, connection);
//                        }
    }

    protected String getDataLocationPreferencesString() throws SQLException {
        try (Connection connection = getCatalogue().getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (String s : getLocationPrefStr(getLogicalData().getUid(), connection)) {
                sb.append(s).append(",");
            }
            if (sb.length() > 1) {
                sb.replace(sb.lastIndexOf(","), sb.length(), "");
                sb.append("]");
                return sb.toString();
            }

        }
        return null;
    }

    private Collection<Long> getLocationPrefLong(Long uid, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT storageSiteRef FROM pref_table WHERE ld_uid=?")) {
            Collection<Long> result = new ArrayList<>();
            ps.setLong(1, uid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        }
    }

    private Collection<String> getLocationPrefStr(Long uid, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT resourceUri "
                + "FROM pref_table "
                + "JOIN storage_site_table "
                + "ON storageSiteRef=storageSiteId "
                + "WHERE ld_uid=?")) {
            Collection<String> result = new ArrayList<>();
            ps.setLong(1, uid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        }
    }

    private Collection<String> getAvailStorageSitesStr(Boolean includePrivate, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            Collection<String> result = new ArrayList<>();
            ResultSet rs = includePrivate ? statement.executeQuery("SELECT resourceUri FROM storage_site_table WHERE isCache=FALSE AND removing=FALSE")
                    : statement.executeQuery("SELECT resourceUri FROM storage_site_table WHERE isCache=FALSE AND removing=FALSE AND private=FALSE");
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        }
    }

    protected List<String> property2List(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        return Arrays.asList(value.split("\\s*,\\s*"));
    }

    private void setDataLocationPref(String value, Connection connection) throws SQLException {
        List<String> list = property2List(value);
        getCatalogue().setLocationPreferences(connection, getLogicalData().getUid(), list, getPrincipal().isAdmin());
        List<String> sites = property2List(getDataLocationPreferencesString());
        getLogicalData().setDataLocationPreferences(sites);
    }

    private String getReplicationQueueString() throws SQLException {
        List<LogicalData> list = getCatalogue().getReplicationQueue();
        if (list != null && !list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (LogicalData ld : getCatalogue().getReplicationQueue()) {
                sb.append(ld.getName()).append(",");
            }
            sb.replace(sb.lastIndexOf(","), sb.length(), "").append("],");

            return sb.toString();
        }
        return null;

    }

    private String getReplicationQueueLen() throws SQLException {
        return String.valueOf(getCatalogue().getReplicationQueueLen());
    }

    private String getReplicationQueueSize() throws SQLException {
        return String.valueOf(getCatalogue().getReplicationQueueSize());
    }

    private void lockResources(List<WebDataFileResource> resources) throws NotAuthorizedException, PreConditionFailedException, LockedException {
        for (WebDataFileResource r : resources) {
            LockToken tocken = r.getCurrentLock();
            if (tocken == null || tocken.isExpired()) {
                LockTimeout timeout = new LockTimeout(System.currentTimeMillis() + Constants.LOCK_TIME);
                LockInfo info = new LockInfo(LockInfo.LockScope.EXCLUSIVE,
                        LockInfo.LockType.WRITE, getPrincipal().getUserId(),
                        LockInfo.LockDepth.INFINITY);
                LockResult lockResult = r.lock(timeout, info);
            }
        }
    }

    private Collection<Long> getPreferencesForFile(Long uid, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT "
                + "storageSiteRef FROM pref_table WHERE ld_uid=?")) {
            Collection<Long> result = new ArrayList<>();
            preparedStatement.setLong(1, uid);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }
            return result;
        }
    }

    private Map<Long, Pair<WebDataFileResource, Long>> createResouses(Request request) throws SQLException, UnsupportedEncodingException, NotAuthorizedException, NoSuchAlgorithmException, IOException {
        Map<Long, Pair<WebDataFileResource, Long>> resources = null;
        try (Connection connection = getCatalogue().getConnection()) {
            Map<String, FileItem> files = request.getFiles();
            Collection<FileItem> fileItems = files.values();

            resources = new HashMap<>();
            WebDataFileResource resource = null;
            for (FileItem fi : fileItems) {
                Long pdriGroupid;
                Path newPath = Path.path(getPath(), fi.getName());
                LogicalData fileLogicalData = getCatalogue().getLogicalDataByPath(newPath, connection);
                String contentType = mimeTypeMap.get(FilenameUtils.getExtension(fi.getName()));
                if (fileLogicalData != null) {
                    Permissions p = getCatalogue().getPermissions(fileLogicalData.getUid(), fileLogicalData.getOwner(), connection);
                    if (!getPrincipal().canWrite(p)) {
                        throw new NotAuthorizedException(this);
                    }
                    fileLogicalData.setLength(fi.getSize());
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.setLastAccessDate(fileLogicalData.getModifiedDate());
                    fileLogicalData.addContentType(contentType);
                    pdriGroupid = fileLogicalData.getPdriGroupId();
                    resource = new WebDataFileResource(fileLogicalData, Path.path(getPath(), fi.getName()), getCatalogue(), authList);
                } else {
                    fileLogicalData = new LogicalData();
                    fileLogicalData.setName(fi.getName());
                    fileLogicalData.setParentRef(getLogicalData().getUid());
                    fileLogicalData.setType(Constants.LOGICAL_FILE);
                    fileLogicalData.setOwner(getPrincipal().getUserId());
                    fileLogicalData.setLength(fi.getSize());
                    fileLogicalData.setCreateDate(System.currentTimeMillis());
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.setLastAccessDate(System.currentTimeMillis());
                    fileLogicalData.setTtlSec(getLogicalData().getTtlSec());
                    fileLogicalData.addContentType(contentType);
                    pdriGroupid = getCatalogue().associateLogicalDataAndPdriGroup(fileLogicalData, connection);

                    getCatalogue().setPreferencesOn(fileLogicalData.getUid(), getLogicalData().getUid(), connection);
                    List<String> pref = getLogicalData().getDataLocationPreferences();
                    fileLogicalData.setDataLocationPreferences(pref);

                    resource = new WebDataFileResource(fileLogicalData, Path.path(getPath(), fi.getName()), getCatalogue(), authList);
                }
                MutablePair<WebDataFileResource, Long> pair = new MutablePair<>();
                pair.setRight(pdriGroupid);
                pair.setLeft(resource);
                resources.put(Long.valueOf(resource.getUniqueId()), pair);
            }
            connection.commit();
            connection.close();
        }
        return resources;
    }

    private Map<String, Pair<Long, Collection<Long>>> getStorageMap(Map<Long, Pair<WebDataFileResource, Long>> resources) throws SQLException, Exception {
        Map<String, Pair<Long, Collection<Long>>> storageMap = new HashMap<>(resources.size());
        MutablePair<Long, Collection<Long>> pair;
        Collection<Pair<WebDataFileResource, Long>> val = resources.values();
        try (Connection connection = getCatalogue().getConnection()) {
            for (Pair<WebDataFileResource, Long> r : val) {
                Long uid = Long.valueOf(r.getLeft().getUniqueId());
                Collection<Long> pref = getPreferencesForFile(uid, connection);
                pair = new MutablePair<>();
                if (pref != null && !pref.isEmpty()) {
                    pair.setLeft(uid);
                    pair.setRight(pref);
                    storageMap.put(r.getLeft().getName(), pair);
                } else {
                    Collection<Long> sites = getReplicationPolicy().getSitesToReplicate(connection);
                    pair.setLeft(uid);
                    pair.setRight(sites);
                    storageMap.put(r.getLeft().getName(), pair);
                }
            }
        }
        return storageMap;
    }

    private ReplicationPolicy getReplicationPolicy() throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        Class<? extends ReplicationPolicy> replicationPolicyClass = Class.forName(PropertiesHelper.getReplicationPolicy()).asSubclass(ReplicationPolicy.class);
        return replicationPolicyClass.newInstance();
    }

    protected boolean canRedirect(Request request) throws SQLException, UnsupportedEncodingException, URISyntaxException, IOException {
        if (isInCache()) {
            return false;
        }
        Auth auth = request.getAuthorization();
        if (auth == null) {
            return false;
        }
        final String autheader = request.getHeaders().get("authorization");
        if (autheader != null) {
            final int index = autheader.indexOf(' ');
            if (index > 0) {
                final String credentials = new String(Base64.decodeBase64(autheader.substring(index).getBytes()), "UTF8");
                final String uname = credentials.substring(0, credentials.indexOf(":"));
                final String token = credentials.substring(credentials.indexOf(":") + 1);
                if (authenticate(uname, token) == null) {
                    return false;
                }
                if (!authorise(request, Request.Method.GET, auth)) {
                    return false;
                }
            }
        }
        String userAgent = request.getHeaders().get("user-agent");
        if (userAgent == null || userAgent.length() <= 1) {
            return false;
        }
//        WebDataFileResource.Logger.getLogger(WebDataResource.class.getName()).log(Level.FINE, "userAgent: {0}", userAgent);
        List<String> nonRedirectableUserAgents = PropertiesHelper.getNonRedirectableUserAgents();
        for (String s : nonRedirectableUserAgents) {
            if (userAgent.contains(s)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isInCache() throws SQLException, URISyntaxException, IOException {
        List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId());
        for (PDRIDescr pdri : pdriDescr) {
            if (pdri.getResourceUrl().startsWith("file")) {
                return true;
            }
        }

//        try (Connection cn = getCatalogue().getConnection()) {
//            List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId(), cn);
//            for (PDRIDescr pdri : pdriDescr) {
//                if (pdri.getResourceUrl().startsWith("file")) {
//                    return true;
//                }
//            }
//        }
        return false;
    }

    /**
     * @return the logicalData
     */
    public LogicalData getLogicalData() {
        return logicalData;
    }

    /**
     * @return the catalogue
     */
    public JDBCatalogue getCatalogue() {
        return catalogue;
    }

    /**
     * @return the path
     */
    public Path getPath() {
        return path;
    }
}
