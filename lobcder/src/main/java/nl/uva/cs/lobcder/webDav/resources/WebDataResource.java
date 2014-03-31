/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.LockedException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.http.values.HrefList;
import io.milton.principal.DavPrincipals;
import io.milton.principal.Principal;
import io.milton.property.PropertySource;
import io.milton.resource.*;
import lombok.Getter;
import lombok.extern.java.Log;
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
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * @author S. Koulouzis
 */
@Log
public class WebDataResource implements PropFindableResource, Resource,
        AccessControlledResource, MultiNamespaceCustomPropertyResource, LockableResource {

    @Getter
    private final LogicalData logicalData;
    @Getter
    private final JDBCatalogue catalogue;
    @Getter
    private final Path path;
    protected final List<AuthI> authList;
//    protected final AuthI auth2;
    private static final ThreadLocal<MyPrincipal> principalHolder = new ThreadLocal<>();
    protected String fromAddress;

    public WebDataResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull List<AuthI> authList) {
        this.authList = authList;
//        this.auth2 = auth2;
        this.logicalData = logicalData;
        this.catalogue = catalogue;
        this.path = path;
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
        return logicalData.getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        String token = password;
        MyPrincipal principal = null;

        for (AuthI a : authList) {
            principal = a.checkToken(token);
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
            WebDataResource.log.log(Level.FINE, "getUserId: {0}", principal.getUserId());
            WebDataResource.log.log(Level.FINE, "getRolesStr: {0}", principal.getRolesStr());
            String msg = "From: " + fromAddress + " user: " + principal.getUserId() + " password: " + password;
            WebDataResource.log.log(Level.INFO, msg);
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
            WebDataResource.log.log(Level.INFO, msg);
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
                    parentLD = getCatalogue().getLogicalDataByUid(logicalData.getParentRef());
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
            WebDataResource.log.log(Level.SEVERE, "Exception in authorize for a resource " + getPath(), th);
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
        WebDataResource.log.log(Level.FINE, "getPrincipalURL for {0}: {1}", new Object[]{getPath(), principalURL});
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
            WebDataResource.log.log(Level.SEVERE, "Could not get Permissions for resource " + getPath(), e);
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
        WebDataResource.log.log(Level.FINE, "getAccessControlList for {0}", getPath());
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
            WebDataResource.log.log(Level.SEVERE, "Cannot read permissions for resource " + getPath(), e);
        }
        return acl;
    }

    @Override
    public void setAccessControlList(Map<Principal, List<Priviledge>> map) {
        WebDataResource.log.log(Level.FINE, "PLACEHOLDER setAccessControlList() for {0}", getPath());

        for (Map.Entry<Principal, List<Priviledge>> me : map.entrySet()) {
            Principal principal = me.getKey();
            for (Priviledge priviledge : me.getValue()) {
                WebDataResource.log.log(Level.FINE, "Set priveledges {0} for {1}", new Object[]{priviledge, principal});
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
        Collection<StorageSite> cacheSS = getCatalogue().getStorageSites(connection, Boolean.TRUE);
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
                    ss.getCredential().getStorageSitePassword(), ss.isEncrypt(), DesEncrypter.generateKey(), null, null);
            return PDRIFactory.getFactory().createInstance(pdriDescr, true);
        }
    }

    @Override
    public Object getProperty(QName qname) {
        try {
            log.log(Level.FINE, "qname: {0}", qname);
            if (qname.equals(Constants.DATA_DIST_PROP_NAME)) {
                try (Connection connection = getCatalogue().getConnection()) {
                    try {
                        connection.commit();
                        StringBuilder sb = new StringBuilder();
                        if (getLogicalData().isFolder()) {
                            List<? extends WebDataResource> children = (List<? extends WebDataResource>) ((WebDataDirResource) (this)).getChildren();
                            sb.append("[");
                            for (WebDataResource r : children) {
                                if (r instanceof WebDataFileResource) {
                                    sb.append("'").append(r.getName()).append("' : [");
                                    Collection<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(r.getLogicalData().getPdriGroupId(), connection);
                                    for (PDRIDescr p : pdris) {
                                        sb.append("'").append(p.getResourceUrl()).append("',");
                                    }
                                    sb.replace(sb.lastIndexOf(","), sb.length(), "").append("],");
                                }
                            }
                        } else {
                            Collection<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId(), connection);
                            sb.append("[");
                            for (PDRIDescr p : pdris) {
                                sb.append("'").append(p.getResourceUrl()).append("'");
                                sb.append(",");
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
                return getLogicalData().getDataLocationPreference();
            } else if (qname.equals(Constants.ENCRYPT_PROP_NAME)) {
                try (Connection connection = getCatalogue().getConnection()) {
                    StringBuilder sb = new StringBuilder();
                    if (getLogicalData().isFolder()) {
                        List<? extends WebDataResource> children = (List<? extends WebDataResource>) ((WebDataDirResource) (this)).getChildren();
                        sb.append("[");
                        for (WebDataResource r : children) {
                            if (r instanceof WebDataFileResource) {
                                sb.append("'").append(r.getName()).append("' : [");
                                Collection<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(r.getLogicalData().getPdriGroupId(), connection);
                                for (PDRIDescr p : pdris) {
                                    sb.append("[");
                                    sb.append(p.getResourceUrl());
                                    sb.append(p.getEncrypt());
                                    sb.append("],");
                                }
                                sb.replace(sb.lastIndexOf(","), sb.length(), "").append("],");
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
            } else if (qname.equals(Constants.AVAIL_STORAGE_SITES_PROP_NAME)) {
                try (Connection connection = getCatalogue().getConnection()) {
                    connection.commit();
                    Collection<StorageSite> ss = getCatalogue().getStorageSites(connection, Boolean.FALSE);
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    for (StorageSite s : ss) {
                        sb.append(s.getResourceURI()).append(",");
                    }
                    sb.replace(sb.lastIndexOf(","), sb.length(), "");
                    sb.append("]");
                    return sb.toString();
                }

            }
            return PropertySource.PropertyMetaData.UNKNOWN;
        } catch (Throwable th) {
            WebDataResource.log.log(Level.SEVERE, "Exception in getProperty() for resource " + getPath(), th);
            return PropertySource.PropertyMetaData.UNKNOWN;
        }
    }

    @Override
    public void setProperty(QName qname, Object o) throws PropertySource.PropertySetException, NotAuthorizedException {
        WebDataResource.log.log(Level.FINE, "setProperty for resource {0} : {1} = {2}", new Object[]{getPath(), qname, o});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                if (o != null) {
                    String value = (String) o;
                    if (qname.equals(Constants.DRI_SUPERVISED_PROP_NAME)) {
                        Boolean v = Boolean.valueOf(value);
                        getLogicalData().setSupervised(v);
                        catalogue.setLogicalDataSupervised(getLogicalData().getUid(), v, connection);
                    } else if (qname.equals(Constants.DRI_CHECKSUM_PROP_NAME)) {
                        getLogicalData().setChecksum(value);
                        catalogue.setFileChecksum(getLogicalData().getUid(), value, connection);
                    } else if (qname.equals(Constants.DRI_LAST_VALIDATION_DATE_PROP_NAME)) {
                        Long v = Long.valueOf(value);
                        getLogicalData().setLastValidationDate(v);
                        catalogue.setLastValidationDate(getLogicalData().getUid(), v, connection);
                    } else if (qname.equals(Constants.DRI_STATUS_PROP_NANE)) {
                        getLogicalData().setStatus(value);
                        catalogue.setDriStatus(getLogicalData().getUid(), value, connection);
                    } else if (qname.equals(Constants.DESCRIPTION_PROP_NAME)) {
                        String v = value;
                        getLogicalData().setDescription(v);
                        catalogue.setDescription(getLogicalData().getUid(), v, connection);
                    } else if (qname.equals(Constants.DATA_LOC_PREF_NAME)) {
                        String v = value;
                        getLogicalData().setDataLocationPreference(v);
                        catalogue.setLocationPreference(getLogicalData().getUid(), v, connection);
                    } else if (qname.equals(Constants.ENCRYPT_PROP_NAME)) {
//                        String v = value;
//                        HashMap<String, Boolean> hostEncryptMap = new HashMap<>();
//                        log.log(Level.FINE, "Value: {0}", v);
//                        String[] parts = v.split("[\\[\\]]");
//                        for (String p : parts) {
//                            log.log(Level.FINE, "Parts: {0}", p);
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
        if (qname.equals(Constants.DATA_DIST_PROP_NAME)) {
            return new PropertySource.PropertyMetaData(PropertySource.PropertyAccessibility.READ_ONLY, String.class);
        }
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
                    lockTimeout = Long.valueOf(12000);
                }
                getLogicalData().setLockTimeout(lockTimeout);

                getCatalogue().setLockTimeout(getLogicalData().getUid(), lockTimeout, connection);
                connection.commit();
                return LockResult.success(lockToken);
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new PreConditionFailedException(this);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new PreConditionFailedException(this);
        }

    }

    @Override
    public LockResult refreshLock(String token) throws NotAuthorizedException, PreConditionFailedException {
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                if (getLogicalData().getLockTokenID() == null) {
                    throw new RuntimeException("not locked");
                } else {
                    if (!getLogicalData().getLockTokenID().equals(token)) {
                        throw new RuntimeException("invalid lock id");
                    }
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
                log.log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new PreConditionFailedException(this);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new PreConditionFailedException(this);
        }
    }

    @Override
    public void unlock(String token) throws NotAuthorizedException, PreConditionFailedException {
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                if (getLogicalData().getLockTokenID() == null) {
                    return;
                } else {
                    if (!getLogicalData().getLockTokenID().equals(token)) {
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
                log.log(Level.SEVERE, null, ex);
                connection.rollback();
                throw new PreConditionFailedException(this);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
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
    public String checkRedirect(Request rqst) throws NotAuthorizedException, BadRequestException {
        return null;
    }
}
