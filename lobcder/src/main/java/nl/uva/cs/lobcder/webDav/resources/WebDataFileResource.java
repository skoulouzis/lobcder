/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;


import io.milton.common.ContentTypeUtils;
import io.milton.common.Path;
import io.milton.http.*;
import io.milton.http.exceptions.*;
import io.milton.resource.CollectionResource;
import io.milton.resource.FileResource;
import io.milton.resource.LockableResource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.vlet.io.CircularStreamBufferTransferer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WebDataFileResource extends WebDataResource implements
        FileResource, LockableResource {

    public WebDataFileResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull AuthI auth1,  AuthI auth2) {
        super(logicalData, path, catalogue, auth1, auth2);
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        switch(method) {
            case MKCOL : return false;
            default: return super.authorise(request, method, auth);
        }
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        WebDataDirResource toWDDR = (WebDataDirResource) collectionResource;
        log.fine("copyTo('" + toWDDR.getPath() + "', '" + name + "') for " + getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions newParentPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                if (!getPrincipal().canWrite(newParentPerm)) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().copyFile(getLogicalData(), toWDDR.getLogicalData(), name, getPrincipal(), connection);
                connection.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this, e.getMessage());
        }
    }

    @Override
    public void moveTo(CollectionResource collectionResource, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        WebDataDirResource toWDDR = (WebDataDirResource) collectionResource;
        log.fine("moveTo('" + toWDDR.getPath() + "', '" + name + "') for " + getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try{
                Permissions destPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                if (!getPrincipal().canWrite(destPerm)) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().moveEntry(getLogicalData(), toWDDR.getLogicalData(), name, connection);
                connection.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this, e.getMessage());
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, BadRequestException, ConflictException {
        log.fine("delete() file " + getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                getCatalogue().remove(getLogicalData(), getPrincipal(), connection);
                connection.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this, e.getMessage());
        }
    }

    @Override
    public Long getContentLength() {
        log.fine("getContentLength()" + " for " + getPath());
        return getLogicalData().getLength();
    }

    @Override
    public String getContentType(String accepts) {
        String res = ContentTypeUtils.findAcceptableContentType(getLogicalData().getContentTypesAsString(), accepts);
        log.fine("getContentType('" + accepts + "') = '" + res + "'  for " + getPath());
        return res;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        log.fine("getMaxAgeSeconds() for " + getPath());
        return null;
    }

    private void circularStreamBufferTransferer(Iterator<PDRIDescr> it, OutputStream out, int tryCount, PDRI pdri) throws IOException {
        try {
            boolean reconnect;
            if (pdri == null && it.hasNext()) {
                pdri = PDRIFactory.getFactory().createInstance(it.next());
                reconnect = false;
            } else {
                reconnect = true;
            }
            if (pdri != null) {
                if (reconnect) {
                    pdri.reconnect();
                }
                WebDataFileResource.log.fine("sendContent() for " + getPath() + "--------- " + pdri.getFileName());
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((5 * 1024 * 1024), pdri.getData(), out);
                cBuff.startTransfer((long) -1);
            } else {
                throw new IOException("Could not get file content");
            }
        } catch (Exception e) {
            if (pdri == null) {
                //noinspection ConstantConditions
                throw (IOException)e;
            } else {
                if (e.getMessage() != null && e.getMessage().contains("Couldn open location")) {
                    circularStreamBufferTransferer(it, out, 0, null);
                } else {
                    if (++tryCount < Constants.RECONNECT_NTRY) {
                        circularStreamBufferTransferer(it, out, tryCount, pdri);
                    } else {
                        circularStreamBufferTransferer(it, out, 0, null);
                    }
                }
            }
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException, NotFoundException {
        Iterator<PDRIDescr> it;
        try{
            it = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId()).iterator();
            circularStreamBufferTransferer(it, out, 0, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(this, e.getMessage());
        }
    }

    @Override
    public String processForm(Map<String, String> parameters,
                              Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException {
        throw new BadRequestException(this, "Not implemented");
    }

    @Override
    public String checkRedirect(Request request) {
        WebDataFileResource.log.fine("checkRedirect() for " + getPath());
        return null;
    }

    @Override
    public Date getCreateDate() {
        WebDataFileResource.log.fine("getCreateDate() for " + getPath());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public LockResult lock(LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException, PreConditionFailedException, LockedException {
        if (getCurrentLock() != null) {
            throw new LockedException(this);
        }
        LockToken lockToken = new LockToken(UUID.randomUUID().toString(), lockInfo, timeout);
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
                getLogicalData().setLockTimeout(lockToken.timeout.getSeconds());
                getCatalogue().setLockTimeout(getLogicalData().getUid(), getLogicalData().getLockTimeout(), connection);
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
}
