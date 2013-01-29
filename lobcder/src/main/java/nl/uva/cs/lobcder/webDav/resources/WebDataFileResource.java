/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.*;
import com.bradmcevoy.http.exceptions.PreConditionFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.catalogue.ResourceExistsException;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.MMTypeTools;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.vfs.VFSNode;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataFileResource extends WebDataResource implements
        com.bradmcevoy.http.FileResource, LockableResource {

    private static final boolean debug = true;
//    private final int bufferSize;
    private int reconnectAttemts;

    public WebDataFileResource(JDBCatalogue catalogue, LogicalData logicalData) throws CatalogueException, Exception {
        super(catalogue, logicalData);
        if (!logicalData.getType().equals(Constants.LOGICAL_FILE)) {
            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
        }


//            OperatingSystemMXBean osMBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
//            bufferSize = (int) (osMBean.getFreePhysicalMemorySize() / 200);
//            debug("Alocated  physical memory:\t" + bufferSize / (1024.0 * 1024.0));
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws ConflictException, NotAuthorizedException {
        WebDataDirResource toWDDR = (WebDataDirResource) collectionResource;
        Connection connection = null;
        try {
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            debug(getLogicalData().getLDRI().toPath() + " file copyTo.");
            debug("\t toCollection: " + toWDDR.getLogicalData().getLDRI().toPath());
            debug("\t name: " + name);
            Permissions copyToPerm = getCatalogue().getPermissions(getLogicalData().getUID(), getLogicalData().getOwner(), connection);
            if (!getPrincipal().canRead(copyToPerm)) {
                throw new NotAuthorizedException(this);
            }
            Permissions newParentPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUID(), toWDDR.getLogicalData().getOwner(), connection);
            if (!getPrincipal().canWrite(newParentPerm)) {
                throw new NotAuthorizedException(this);
            }
            getCatalogue().copyEntry(getLogicalData(), toWDDR.getLogicalData(), name, getPrincipal(), connection);
            connection.commit();
            connection.close();
        } catch (CatalogueException ex) {
            throw new ConflictException(this, ex.toString());
        } catch (Exception e) {
            throw new NotAuthorizedException(this);
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Connection connection = null;
        try {
            debug(getLogicalData().getLDRI().toPath() + " file delete.");
            Path parentPath = getLogicalData().getLDRI().getParent();
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            LogicalData parentLD = getCatalogue().getResourceEntryByLDRI(parentPath, connection);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions p = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), connection);
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException(this);
            }
            getCatalogue().removeResourceEntry(getLogicalData(), getPrincipal(), connection);
            connection.commit();
            connection.close();
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Long getContentLength() {
        return getLogicalData().getLength();
    }

    @Override
    public String getContentType(String accepts) {
//        debug("getContentType. accepts: " + accepts);

        String type = "";
        List<String> fileContentTypes = getLogicalData().getContentTypes();


        if (accepts != null && fileContentTypes != null && !fileContentTypes.isEmpty()) {
            String[] acceptsTypes = accepts.split(",");
            Collection<String> acceptsList = new ArrayList<String>();
            acceptsList.addAll(Arrays.asList(acceptsTypes));

            for (String fileContentType : fileContentTypes) {
                type = MMTypeTools.bestMatch(acceptsList, fileContentType);
                debug("\t type: " + type);
                if (!StringUtil.isEmpty(type)) {
                    debug("getContentType: " + type);
                    return type;
                }
            }
        } else {
            String regex = "(^.*?\\[|\\]\\s*$)";
            type = fileContentTypes.toString().replaceAll(regex, "");
            debug("getContentType: " + type);
            return type;
        }
        debug("getContentType: null");
        return null;
    }

    /**
     * Specifies a lifetime for the information returned by this header. A
     * client MUST discard any information related to this header after the
     * specified amount of time.
     *
     * @param auth
     * @return
     */
    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException, NotFoundException {
//        long start = System.currentTimeMillis();
//        long totalWritten = this.getContentLength();
//        debug("sendContent.");
//        debug("\t range: " + range);
//        debug("\t params: " + params);
//        debug("\t contentType: " + contentType);
        Connection connection = null;
        PDRI pdri = null;
        try {
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            Permissions p = getCatalogue().getPermissions(getLogicalData().getUID(), getLogicalData().getOwner(), connection);
            if (!getPrincipal().canRead(p)) {
                throw new NotAuthorizedException(this);
            }

            Iterator<PDRI> it = getCatalogue().getPdriByGroupId(getLogicalData().getPdriGroupId(), connection).iterator();
            if (it.hasNext()) {
                pdri = it.next();
            }
            connection.commit();
            connection.close();
            debug("--------- " + pdri.getURI());
            debug("getLength: " + pdri.getLength());
            //IOUtils.copy(pdri.getData(), out); 
//            fastCopy(pdri.getData(), out);

            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((5 * 1024 * 1024), pdri.getData(), out);
            cBuff.startTransfer(new Long(-1));
        } catch (NotAuthorizedException ex) {
            debug("NotAuthorizedException");
            throw new NotAuthorizedException(this);
        } catch (Exception ex) {
            //Try one more time 
            debug("reconnect");
            pdri.reconnect();
            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((5 * 1024 * 1024), pdri.getData(), out);
            try {
                cBuff.startTransfer(new Long(-1));
            } catch (VlException ex1) {
                throw new IOException(ex1.getMessage());
            }
        } finally {
            try {
                out.flush();
                out.close();
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
//                System.gc();
//                long elapsed = System.currentTimeMillis() - start;
//                double speed = ((totalWritten * 8) / 1000.0 * 1000.0) / (elapsed / 1000.0);
//                debug("SendCont speed:  " + speed + " MBit/sec (SI)");
                //                if (pdri != null && pdri.getData() != null) {
                //                    pdri.getData().close();
                //                }
            } catch (Exception ex) {
                debug(ex.getMessage());
                //Ignore
            }
        }
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
//        debug("moveTo: file " + getLogicalData().getLDRI().toPath());
        WebDataDirResource rdst = (WebDataDirResource) rDest;
//        debug("\t rDestgetName: " + rdst.getLogicalData().getLDRI().toPath() + " name: " + name);
        Connection connection = null;
        try {
            Path parentPath = getLogicalData().getLDRI().getParent(); //getPath().getParent();
            if (parentPath == null) {
                throw new NotAuthorizedException(this);
            }
            connection = getCatalogue().getConnection();
            connection.setAutoCommit(false);
            LogicalData parentLD = getCatalogue().getResourceEntryByLDRI(getLogicalData().getLDRI().getParent(), connection);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions parentPerm = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), connection);

            if (!getPrincipal().canWrite(parentPerm)) {
                throw new NotAuthorizedException(this);
            }
            Permissions destPerm = getCatalogue().getPermissions(rdst.getLogicalData().getUID(), rdst.getLogicalData().getOwner(), connection);
            if (!getPrincipal().canWrite(destPerm)) {
                throw new NotAuthorizedException(this);
            }
            getCatalogue().moveEntry(getLogicalData(), rdst.getLogicalData(), name, connection);
            connection.commit();
            connection.close();
        } catch (ResourceExistsException ex) {
            throw new ConflictException(rDest, ex.getMessage());
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException {

        //Maybe we can do more smart things here with deltas. So if we update a file send only the diff
//        debug("processForm.");
//        debug("\t parameters: " + parameters);
//        debug("\t files: " + files);
        Collection<FileItem> values = files.values();
        VFSNode node;
        OutputStream out;
        InputStream in;
//        Metadata meta;
//        try {
//            for (FileItem i : values) {
//
//                debug("\t getContentType: " + i.getContentType());
//                debug("\t getFieldName: " + i.getFieldName());
//                debug("\t getName: " + i.getName());
//                debug("\t getSize: " + i.getSize());
//                
//                if (!logicalData.hasPhysicalData()) {
//                    node = logicalData.createPhysicalData();
//                    out = ((VFile)node).getOutputStream();
//                    in = i.getInputStream();
//                    IOUtils.copy(in, out);
////                     PartialGetHelper.writeRange(in, range, out);
//                    in.close();
//                    out.flush();
//                    out.close();
//                    meta = logicalData.getMetadata();
//                    meta.setLength(i.getSize());
//                    meta.addContentType(i.getContentType());
//                    meta.setModifiedDate(System.currentTimeMillis());
//                    logicalData.setMetadata(meta);
//                    
//                }else{
//                    throw new BadRequestException(this);
//                }
//            }
//        } catch (IOException ex) {
//            throw new BadRequestException(this);
//        } catch (VlException ex) {
//            throw new BadRequestException(this);
//        } finally {
//        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
//        debug("checkRedirect.");
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

    @Override
    public Date getCreateDate() {
//        debug("getCreateDate.");
        return new Date(getLogicalData().getCreateDate());
    }

    private void fastCopy(InputStream in, OutputStream out) throws IOException, VlException {
        final ReadableByteChannel inputChannel = Channels.newChannel(in);
        final WritableByteChannel outputChannel = Channels.newChannel(out);
        fastCopy(inputChannel, outputChannel);
    }

    private void fastCopy(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(5242880);
        int len;
        while ((len = src.read(buffer)) != -1) {
//            System.err.println("Read size: " + len);
            buffer.flip();
            dest.write(buffer);
            buffer.compact();
        }
//        System.err.println("--------------");
        buffer.flip();
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

    @Override
    public LockResult lock(LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException, PreConditionFailedException, LockedException {
        try {
            //        if (!getPrincipal().canWrite(getLogicalData().getPermissions())) {
            //        }
            //        }
            if (getCurrentLock() != null) {
                throw new LockedException(this);
            }
            LockToken token = new LockToken(UUID.randomUUID().toString(), lockInfo, timeout);
            getLogicalData().lock(token);
            return LockResult.success(token);
        } catch (CatalogueException ex) {
            throw new PreConditionFailedException(this);
        }

    }

    @Override
    public LockResult refreshLock(String token) throws NotAuthorizedException, PreConditionFailedException {
        try {
            // reset the current token
            if (getLogicalData().getCurrentLock() == null) {
                throw new RuntimeException("not locked");
            }

            if (!getLogicalData().getCurrentLock().tokenId.equals(token)) {
                throw new RuntimeException("invalid lock id");
            }
            //        if (!getPrincipal().canWrite(getLogicalData().getPermissions())) {
            //            throw new NotAuthorizedException(this);
            //        }
            LockToken lockToken = getLogicalData().refreshLock(token);
            return LockResult.success(lockToken);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (CatalogueException ex) {
            throw new PreConditionFailedException(this);
        }

    }

    @Override
    public void unlock(String token) throws NotAuthorizedException, PreConditionFailedException {
        try {
            if (getLogicalData().getCurrentLock() == null) {
                return;
            }

            if (!getLogicalData().getCurrentLock().tokenId.equals(token)) {
                throw new PreConditionFailedException(this);
            }
            //        if (!getPrincipal().canWrite(getLogicalData().getPermissions())) {
            //            throw new NotAuthorizedException(this);
            //        }
            getLogicalData().unlock();
        } catch (CatalogueException ex) {
            throw new PreConditionFailedException(this);
        }
    }

    @Override
    public LockToken getCurrentLock() {
//        MyPrincipal thisPrincipal = getPrincipal();
//        Permissions perm = getLogicalData().getPermissions();
//        boolean canRead = thisPrincipal.canRead(perm);
//        if (! canRead ) {
//            throw new RuntimeException("Not Authorized to get lock");
//        }
        LockToken token = getLogicalData().getCurrentLock();
        debug("getCurrentLock: " + token);
        return token;
    }
}
