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
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.frontend.WebDavServlet;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.LobIOUtils;
import nl.uva.cs.lobcder.util.MMTypeTools;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataFileResource extends WebDataResource implements
        com.bradmcevoy.http.FileResource {

//    private final IDLCatalogue catalogue;
//    private ILogicalData entry;
    private static final boolean debug = true;
//    private String user;

    public WebDataFileResource(IDLCatalogue catalogue, ILogicalData logicalData) throws CatalogueException, Exception {
        super(catalogue, logicalData);
//        this.catalogue = catalogue;
//        this.entry = logicalData;
        if (!logicalData.getType().equals(Constants.LOGICAL_FILE)) {
            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
        }
        initMetadata();
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws ConflictException, NotAuthorizedException {
        try {
            debug("copyTo.");
            debug("\t toCollection: " + collectionResource.getName());
            debug("\t name: " + name);
            // check if request is authorized to read the resource
            Permissions p = new Permissions(getLogicalData().getMetadata().getPermissionArray());
            MyPrincipal principal = getPrincipal();
            if(!p.canRead(principal)){
                throw new NotAuthorizedException();
            }
            // check if we can write to the destination
            Permissions parentPerm = new Permissions(((WebDataResource)collectionResource).getLogicalData().getMetadata().getPermissionArray());
            if(!parentPerm.canWrite(principal)){
                throw new NotAuthorizedException();
            }
            Path toCollectionLDRI = Path.path(collectionResource.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);

            LogicalData newFolderEntry = new LogicalData(newLDRI, Constants.LOGICAL_FILE);
            newFolderEntry.getMetadata().setPermissionArray((ArrayList<Integer>)p.getRolesPerm().clone());
            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            getCatalogue().registerResourceEntry(newFolderEntry);
        } catch (CatalogueException ex) {
            throw new ConflictException(this, ex.toString());
        } catch(Exception e) {
            throw new NotAuthorizedException();
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {        
        try {     
            MyPrincipal principal = getPrincipal();
            Path parentPath = getPath().getParent();
            if(parentPath ==null || parentPath.isRoot()) {
                if(!principal.getRoles().contains(Permissions.ROOT_ADMIN))
                    throw new NotAuthorizedException();
            } else {
                Permissions p = new Permissions(getCatalogue().getResourceEntryByLDRI(getPath().getParent()).getMetadata().getPermissionArray());
                if(!p.canWrite(principal)){
                    throw new NotAuthorizedException();
                }
            }
            Collection<IStorageSite> sites = getLogicalData().getStorageSites();
            if (sites != null && !sites.isEmpty()) {
                for (IStorageSite s : sites) {
                    s.deleteVNode(getLogicalData().getPDRI());
                }
            }
            getCatalogue().unregisterResourceEntry(getLogicalData());
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception e){
            throw new NotAuthorizedException();
        }
    }

    @Override
    public Long getContentLength() {
        Metadata meta = getLogicalData().getMetadata();
        if (meta != null) {
            return meta.getLength();
        }
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);

        String type = "";
        ArrayList<String> fileContentTypes = null;
        if (getLogicalData().getMetadata() != null) {
            fileContentTypes = getLogicalData().getMetadata().getContentTypes();
        }

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
        InputStream in = null;
        debug("sendContent.");
        debug("\t range: " + range);
        debug("\t params: " + params);
        debug("\t contentType: " + contentType);
        
        try {
            Permissions p = new Permissions(getLogicalData().getMetadata().getPermissionArray());
            MyPrincipal principal = getPrincipal();
            if(!p.canRead(principal)){
                throw new NotAuthorizedException();
            }

            VFile vFile;
            if (!getLogicalData().hasPhysicalData()) {
                vFile = (VFile) getLogicalData().createPhysicalData();
            } else {
                vFile = (VFile) getLogicalData().getVFSNode();
            }
            if (vFile == null) {
                throw new IOException("Could not locate physical data source for " + getLogicalData().getLDRI());
            }

            in = vFile.getInputStream();

            if (range != null) {
                debug("sendContent: ranged content: " + vFile.getVRL());
                LobIOUtils.writeRange(in, range, out);
            } else {
                debug("sendContent: send whole file to " + vFile.getVRL());
                IOUtils.copy(in, out);
            }

        } catch (VlException ex) {
            throw new com.bradmcevoy.http.exceptions.NotFoundException(ex.getMessage());
//            throw new IOException(ex);
        } catch (Exception e) {
            throw new NotAuthorizedException();
        } finally {
            out.flush();
            out.close();
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    public void moveTo(CollectionResource rDest, String name)
            throws ConflictException, NotAuthorizedException, BadRequestException {
        debug("moveTo.");
        debug("\t name: " + name);
        try{
            MyPrincipal principal = getPrincipal();
            Path parentPath = getPath().getParent();
            if(parentPath == null || parentPath.isRoot()) {
                if(!principal.getRoles().contains(Permissions.ROOT_ADMIN))
                    throw new NotAuthorizedException();
            } else {
                Permissions p = new Permissions(getCatalogue().getResourceEntryByLDRI(getPath().getParent()).getMetadata().getPermissionArray());
                if(!p.canWrite(principal)){
                    throw new NotAuthorizedException();
                }
            }
            // check if we can write to the destination
            Permissions parentPerm = new Permissions(((WebDataResource)rDest).getLogicalData().getMetadata().getPermissionArray());
            if(!parentPerm.canWrite(principal)){
                throw new NotAuthorizedException();
            }
        } catch (Exception e){
            throw new NotAuthorizedException();
        }
        Path parent;
        Path tmpPath;

        debug("\t rDestgetName: " + rDest.getName() + " name: " + name);

        Path dirPath = ((WebDataDirResource) rDest).getPath();
        debug("\t rDestgetUniqueId: " + rDest.getUniqueId());

        Path newPath = Path.path(dirPath, name);
        parent = getLogicalData().getLDRI().getParent();
        if (newPath.isRelative() && parent != null) {
            tmpPath = Path.path(parent, name);
            newPath = tmpPath;
        }
        try {
            debug("\t rename: " + getLogicalData().getLDRI() + " to " + newPath);
            getCatalogue().renameEntry(getLogicalData().getLDRI(), newPath);
            ILogicalData newLogicData = getCatalogue().getResourceEntryByLDRI(newPath);
            setLogicalData(newLogicData);

            WebDataDirResource dir = (WebDataDirResource) rDest;
            dir.setLogicalData(getCatalogue().getResourceEntryByLDRI(dirPath));


        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(rDest, ex.getMessage());
            }
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
        Metadata meta;
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

    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + getLogicalData().getLDRI() + ": " + msg);
        }

//        log.debug(msg);
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
        if (getLogicalData().getMetadata() != null && getLogicalData().getMetadata().getCreateDate() != null) {
            return new Date(getLogicalData().getMetadata().getCreateDate());
        }
        return null;
    }

    private void initMetadata() {

        Metadata meta = this.getLogicalData().getMetadata();
        Long createDate = meta.getCreateDate();
        if (createDate == null) {
            meta.setCreateDate(System.currentTimeMillis());
            getLogicalData().setMetadata(meta);
        }
        Long modifiedDate = meta.getModifiedDate();
        if (modifiedDate == null) {
            meta.setModifiedDate(System.currentTimeMillis());
            getLogicalData().setMetadata(meta);
        }
    }
}
