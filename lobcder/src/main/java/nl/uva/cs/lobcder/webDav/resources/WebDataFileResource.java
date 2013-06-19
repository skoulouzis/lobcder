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
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.crypto.NoSuchPaddingException;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WebDataFileResource extends WebDataResource implements
        FileResource {

    private int sleepTime = 5;
//, ReplaceableResource {
    private ArrayList<String> workers;
    private boolean doRedirect=false;

    public WebDataFileResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull AuthI auth1, AuthI auth2) {
        super(logicalData, path, catalogue, auth1, auth2);
        workers = new ArrayList<>();
        workers.add("http://localhost:8080/lobcder-worker/");
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {

        switch (method) {
            case MKCOL:
                String msg = "From: " + fromAddress + " User: " + getPrincipal().getUserId() + " Method: " + method;
                WebDataFileResource.log.log(Level.INFO, msg);
                return false;
            default:
                return super.authorise(request, method, auth);
        }
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        WebDataDirResource toWDDR = (WebDataDirResource) collectionResource;
        log.log(Level.FINE, "copyTo(''{0}'', ''{1}'') for {2}", new Object[]{toWDDR.getPath(), name, getPath()});
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
            try {
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
        log.log(Level.FINE, "delete() file {0}", getPath());
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
        log.log(Level.FINE, "getContentLength()" + " for {0}", getPath());
        return getLogicalData().getLength();
    }

    @Override
    public String getContentType(String accepts) {
        String res = ContentTypeUtils.findAcceptableContentType(getLogicalData().getContentTypesAsString(), accepts);
        log.log(Level.FINE, "getContentType(''{0}'') = ''{1}''  for {2}", new Object[]{accepts, res, getPath()});
        return res;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        log.log(Level.FINE, "getMaxAgeSeconds() for {0}", getPath());
        return null;
    }

    private PDRI transferer(Iterator<PDRIDescr> it, OutputStream out, int tryCount, PDRI pdri, boolean doCircularStreamBufferTransferer) throws IOException, NotFoundException {
        try {
            boolean reconnect;
            if (pdri == null && it.hasNext()) {
                pdri = PDRIFactory.getFactory().createInstance(it.next(), false);
                reconnect = false;
            } else {
                reconnect = true;
            }
            if (pdri != null) {
                if (reconnect) {
                    pdri.reconnect();
                }
                WebDataFileResource.log.log(Level.FINE, "sendContent() for {0}--------- {1}", new Object[]{getPath(), pdri.getFileName()});
                if (!pdri.getEncrypted()) {
                    if (doCircularStreamBufferTransferer) {
                        CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), pdri.getData(), out);
                        cBuff.startTransfer((long) -1);
                    } else {
                        int read;
                        byte[] copyBuffer = new byte[Constants.BUF_SIZE];
                        while ((read = pdri.getData().read(copyBuffer, 0, copyBuffer.length)) != -1) {
                            out.write(copyBuffer, 0, read);
                        }
                    }
                } else {
                    DesEncrypter encrypter = new DesEncrypter(pdri.getKeyInt());
                    encrypter.decrypt(pdri.getData(), out);
                }
            } else {
                sleepTime = 5;
                throw new NotFoundException("Physical resource not found");
            }
        } catch (VlException | IOException | java.lang.IllegalStateException ex) {
            try {
                sleepTime = sleepTime + 20;
                Thread.sleep(sleepTime);

                if (ex instanceof nl.uva.vlet.exception.VlInterruptedException && ++tryCount < Constants.RECONNECT_NTRY) {
                    transferer(it, out, tryCount, pdri, false);
                } else if (++tryCount < Constants.RECONNECT_NTRY) {
                    transferer(it, out, tryCount, pdri, true);
                } else {
                    transferer(it, out, 0, null, true);
                }
            } catch (InterruptedException ex1) {
                sleepTime = 5;
                throw new IOException(ex);
            }
        } catch (NoSuchAlgorithmException ex) {
            sleepTime = 5;
            throw new IOException(ex);
        } catch (NoSuchPaddingException ex) {
            sleepTime = 5;
            throw new IOException(ex);
        } catch (InvalidKeyException ex) {
            sleepTime = 5;
            throw new IOException(ex);
        } catch (InvalidAlgorithmParameterException ex) {
            sleepTime = 5;
            throw new IOException(ex);
        }
        sleepTime = 5;
        return pdri;
    }

    private PDRI transfererRange(Iterator<PDRIDescr> it, OutputStream out, int tryCount, PDRI pdri, Range range) throws IOException, NotFoundException {
        try {
            boolean reconnect;
            if (pdri == null && it.hasNext()) {
                pdri = PDRIFactory.getFactory().createInstance(it.next(), false);
                reconnect = false;
            } else {
                reconnect = true;
            }
            if (pdri != null) {
                if (reconnect) {
                    pdri.reconnect();
                }
                WebDataFileResource.log.log(Level.FINE, "sendContent() for {0}--------- {1}", new Object[]{getPath(), pdri.getFileName()});
                pdri.copyRange(range, out, pdri.getEncrypted());
//                if (!) {
//                    
//                } else {
//                    
//                    DesEncrypter encrypter = new DesEncrypter(pdri.getKeyInt());
//                    encrypter.decrypt(pdri.getData(), out);
//                }
            } else {
                sleepTime = 5;
                throw new NotFoundException("Physical resource not found");
            }
        } catch (IOException | java.lang.IllegalStateException ex) {
            try {
                sleepTime = sleepTime + 20;
                Thread.sleep(sleepTime);
                if (++tryCount < Constants.RECONNECT_NTRY) {
                    transfererRange(it, out, tryCount, pdri, range);
                } else {
                    transfererRange(it, out, 0, null, range);
                }
            } catch (InterruptedException ex1) {
                sleepTime = 5;
                throw new IOException(ex1);
            }
        }
        sleepTime = 5;
        return pdri;
    }

//    private void circularStreamBufferTransferer(Iterator<PDRIDescr> it, OutputStream out, int tryCount, PDRI pdri) throws IOException {
//        try {
//            boolean reconnect;
//            if (pdri == null && it.hasNext()) {
//                pdri = PDRIFactory.getFactory().createInstance(it.next());
//                reconnect = false;
//            } else {
//                reconnect = true;
//            }
//            if (pdri != null) {
//                if (reconnect) {
//                    pdri.reconnect();
//                }
//                WebDataFileResource.log.log(Level.FINE, "sendContent() for {0}--------- {1}", new Object[]{getPath(), pdri.getFileName()});
//                if (!pdri.getEncrypted()) {
//                    CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), pdri.getData(), out);
//                    cBuff.startTransfer((long) -1);
//                } else {
//                    DesEncrypter encrypter = new DesEncrypter(pdri.getKeyInt());
//                    encrypter.decrypt(pdri.getData(), out);
//                }
//            } else {
//                throw new IOException("Resource not found");
//            }
//        } catch (Exception e) {
//            if (pdri == null) {
//                //noinspection ConstantConditions
//                throw (IOException) e;
//            } else {
//                if (e.getMessage() != null && e.getMessage().contains("Resource not found")) {
//                    throw new IOException(e);
//                } else if (e.getMessage() != null && e.getMessage().contains("Couldn open location")) {
//                    circularStreamBufferTransferer(it, out, 0, null);
//                } else {
//                    if (++tryCount < Constants.RECONNECT_NTRY) {
//                        circularStreamBufferTransferer(it, out, tryCount, pdri);
//                    } else {
//                        circularStreamBufferTransferer(it, out, 0, null);
//                    }
//                }
//            }
//        }
//    }
    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException, NotFoundException {
        double start = System.currentTimeMillis();
        PDRI pdri;
        Iterator<PDRIDescr> it;
        try {
            it = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId()).iterator();
            if (range != null) {
                WebDataFileResource.log.log(Level.FINE, "Start: {0} end: {1} range: {2}", new Object[]{range.getStart(), range.getFinish(), range.getRange()});
                pdri = transfererRange(it, out, 0, null, range);
            } else {
                pdri = transferer(it, out, 0, null, true);
            }
        } catch (SQLException ex) {
            throw new BadRequestException(this, ex.getMessage());
        } catch (IOException ex) {
            if (ex.getMessage().contains("Resource not found")) {
                throw new NotFoundException(ex.getMessage());
            } else {
                throw new BadRequestException(this, ex.getMessage());
            }
        }
        double elapsed = System.currentTimeMillis() - start;
        long len;
        if (range != null) {
            len = range.getFinish() - range.getStart() + 1;
        } else {
            len = getContentLength();
        }
        double speed = ((len * 8.0) * 1000.0) / (elapsed * 1000.0);
        String msg = "Source: " + pdri.getHost() + " Destination: " + fromAddress + " Tx_Speed: " + speed + " Kbites/sec Tx_Size: " + getContentLength() + " bytes";
        WebDataFileResource.log.log(Level.INFO, msg);
    }

    @Override
    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException {
        throw new BadRequestException(this, "Not implemented");
    }

    @Override
    public Date getCreateDate() {
        WebDataFileResource.log.fine("getCreateDate() for " + getPath());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public String checkRedirect(Request request) {
        WebDataFileResource.log.log(Level.FINE, "checkRedirect for {0}", getPath());
        switch (request.getMethod()) {
            case GET:
                //Replica selection algorithm

                return getBestWorker();
            default:
                return null;
        }
    }

    private String getBestWorker() {
        if (doRedirect) {
            for (String s : workers) {
                return s + getPath();
            }
        } else {
            return null;
        }
        return null;
    }
}
