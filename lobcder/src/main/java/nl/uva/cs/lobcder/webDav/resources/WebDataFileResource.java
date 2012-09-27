/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.authdb.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.catalogue.ResourceExistsException;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.MMTypeTools;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataFileResource extends WebDataResource implements
        com.bradmcevoy.http.FileResource {

    private static final boolean debug = true;

    public WebDataFileResource(JDBCatalogue catalogue, ILogicalData logicalData) throws CatalogueException, Exception {
        super(catalogue, logicalData);
        if (!logicalData.getType().equals(Constants.LOGICAL_FILE)) {
            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
        }
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws ConflictException, NotAuthorizedException {
        WebDataDirResource toWDDR = (WebDataDirResource) collectionResource;
        try {
            debug(getLogicalData().getLDRI().toPath() + " file copyTo.");
            debug("\t toCollection: " + toWDDR.getLogicalData().getLDRI().toPath());
            debug("\t name: " + name);
//            isReadable();
//            Permissions p = new Permissions(getPrincipal());                 
//            getCatalogue().copyEntry(getLogicalData().getUID(), p.getRolesPerm(), toWDDR, name);
            throw new CatalogueException("Not implemented");
            
        } catch (CatalogueException ex) {
            throw new ConflictException(this, ex.toString());
        } catch (Exception e) {
            throw new NotAuthorizedException();
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
            debug(getLogicalData().getLDRI().toPath() + " file delete.");
            Path parentPath = getLogicalData().getLDRI().getParent();
            ILogicalData parentLD = getCatalogue().getResourceEntryByLDRI(parentPath, null);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions p = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), null);
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException();
            }
            getCatalogue().removeResourceEntry(getLogicalData(), getPrincipal(), null);

        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        }
    }

    @Override
    public Long getContentLength() {
        return getLogicalData().getLength();
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);

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
                    return type;
                }
            }
        } else {
            String regex = "(^.*?\\[|\\]\\s*$)";
            type = fileContentTypes.toString().replaceAll(regex, "");
            return type;
        }
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

        debug("sendContent.");
        debug("\t range: " + range);
        debug("\t params: " + params);
        debug("\t contentType: " + contentType);


        isReadable();
        PDRI pdri;
        try {
            pdri = getPDRI();
            IOUtils.copy(pdri.getData(), out);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
        
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        debug("#########################################moveTo: file " + getLogicalData().getLDRI().toPath());
        WebDataDirResource rdst = (WebDataDirResource) rDest;
        debug("\t rDestgetName: " + rdst.getLogicalData().getLDRI().toPath() + " name: " + name);

        try {
            Path parentPath = getLogicalData().getLDRI().getParent(); //getPath().getParent();
            if (parentPath == null) {
                throw new NotAuthorizedException();
            }
            ILogicalData parentLD = getCatalogue().getResourceEntryByLDRI(getLogicalData().getLDRI().getParent(), null);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions p = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), null);
                    
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException();
            }
            rdst.isWritable();
            getCatalogue().moveEntry(getLogicalData(), rdst.getLogicalData(), name, null);

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
        }
    }

    @Override
    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException {

        //Maybe we can do more smart things here with deltas. So if we update a file send only the diff
        debug("processForm.");
        debug("\t parameters: " + parameters);
        debug("\t files: " + files);
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
    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + getLogicalData().getLDRI() + ": " + msg);
        }
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

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        return new Date(getLogicalData().getCreateDate());
    }
}
