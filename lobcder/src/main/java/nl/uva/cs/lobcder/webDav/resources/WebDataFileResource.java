/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.ContentTypeUtils;
import io.milton.common.Path;
import io.milton.http.*;
import io.milton.http.exceptions.*;
import io.milton.resource.BufferingControlResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.FileResource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.AuthWorker;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.cs.lobcder.util.WorkerHelper;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WebDataFileResource extends WebDataResource implements
        FileResource, BufferingControlResource {

    private int sleepTime = 5;
    private List<String> workers;
    private boolean doRedirect = true;
    private static int workerIndex = 0;

    public WebDataFileResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull List<AuthI> authList) {
        super(logicalData, path, catalogue, authList);
        if (doRedirect) {
            workers = WorkerHelper.getWorkers();
        }
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (auth == null) {
            return false;
        }
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
        InputStream in;
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
                in = pdri.getData();
                WebDataFileResource.log.log(Level.FINE, "sendContent() for {0}--------- {1}", new Object[]{getPath(), pdri.getFileName()});
                if (!pdri.getEncrypted()) {
                    if (doCircularStreamBufferTransferer) {
                        CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), in, out);
                        cBuff.startTransfer((long) -1);
                    } else {
                        int read;
                        byte[] copyBuffer = new byte[Constants.BUF_SIZE];
                        while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                            out.write(copyBuffer, 0, read);
                        }
                    }
                } else {
                    DesEncrypter encrypter = new DesEncrypter(pdri.getKeyInt());
                    encrypter.decrypt(in, out);
                }
            } else {
                sleepTime = 5;
                throw new NotFoundException("Physical resource not found");
            }
        } catch (Exception ex) {
            if (ex instanceof NotFoundException) {
                throw (NotFoundException) ex;
            }
            try {
                sleepTime = sleepTime + 20;
                Thread.sleep(sleepTime);
                if (ex instanceof nl.uva.vlet.exception.VlInterruptedException && ++tryCount < Constants.RECONNECT_NTRY) {
                    transferer(it, out, tryCount, pdri, false);
                } else if (++tryCount < Constants.RECONNECT_NTRY) {
                    transferer(it, out, tryCount, pdri, false);
                } else {
                    transferer(it, out, 0, null, true);
                }
            } catch (InterruptedException ex1) {
                sleepTime = 5;
                throw new IOException(ex1);
            }
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
                pdri.copyRange(range, out);
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
            if (!ex.getMessage().contains("does not support random reads")) {
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
            } else {
                sleepTime = 5;
                throw new IOException(ex);
            }

        }
        sleepTime = 5;
        return pdri;
    }

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
                pdri = transferer(it, out, 0, null, false);
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
        Set<String> keys = parameters.keySet();
        for (String s : keys) {
            WebDataFileResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, parameters.get(s)});
        }

        keys = files.keySet();
        for (String s : keys) {
            WebDataFileResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, files.get(s).getFieldName()});
        }

        throw new BadRequestException(this, "Not implemented");
    }

    @Override
    public Date getCreateDate() {
        WebDataFileResource.log.log(Level.FINE, "getCreateDate() for {0}", getPath());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public String checkRedirect(Request request) {
        try {
            switch (request.getMethod()) {
                case GET:
                    String redirect = null;

                    if (doRedirect) {

                        if (!canRedirect(request)) {
                            return null;
                        }

                        //Replica selection algorithm
                        redirect = getBestWorker();
                    }
                    WebDataFileResource.log.log(Level.INFO, "Redirecting to: {0}", redirect);
                    return redirect;
                default:
                    return null;
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException | IOException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String getBestWorker() throws IOException {
        if (doRedirect) {
            workers = WorkerHelper.getWorkers();

            if (workerIndex >= workers.size()) {
                workerIndex = 0;
            }
            String worker = workers.get(workerIndex++);
            String w = worker + getLogicalData().getUid();
            String token = UUID.randomUUID().toString();
            AuthWorker.setTicket(worker, token);
            return w + "/" + token;
        } else {
            return null;
        }
    }

    private boolean isInCache() throws SQLException, URISyntaxException {
        try (Connection cn = getCatalogue().getConnection()) {
            List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId(), cn);
            for (PDRIDescr pdri : pdriDescr) {
                if (pdri.getResourceUrl().startsWith("file")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canRedirect(Request request) throws SQLException, UnsupportedEncodingException, URISyntaxException {
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
        return true;
    }

    @Override
    public Boolean isBufferingRequired() {
        String res = ContentTypeUtils.findAcceptableContentType(getLogicalData().getContentTypesAsString(), null);
        for (String type : Constants.BUFFERED_TYPES) {
            if (res.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
