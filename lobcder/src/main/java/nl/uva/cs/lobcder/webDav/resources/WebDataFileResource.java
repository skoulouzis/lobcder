/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.http11.PartialGetHelper;
import eu.medsea.mimeutil.MimeType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.util.MMTypeTools;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataFileResource implements
        com.bradmcevoy.http.FileResource {

    private final IDLCatalogue catalogue;
    private final ILogicalData logicalData;

    public WebDataFileResource(IDLCatalogue catalogue, ILogicalData logicalData) {
        this.catalogue = catalogue;
        this.logicalData = logicalData;
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws ConflictException {
        try {
            debug("copyTo.");
            debug("\t toCollection: " + collectionResource.getName());
            debug("\t name: " + name);
            Path toCollectionLDRI = Path.path(collectionResource.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);

            LogicalFile newFolderEntry = new LogicalFile(newLDRI);

            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newFolderEntry);
        } catch (CatalogueException ex) {
            throw new ConflictException(this);
        } catch (IOException ex) {
            throw new ConflictException(this);
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
            catalogue.unregisterResourceEntry(logicalData);
        } catch (CatalogueException ex) {
            throw new BadRequestException(this);
        }
    }

    @Override
    public Long getContentLength() {
        Metadata meta = logicalData.getMetadata();
        if (meta != null) {
            return meta.getLength();
        }
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);
        int comp;
        String type;

        if (accepts != null) {
            String[] acceptsTypes = accepts.split(",");
            Collection<String> supported = new ArrayList<String>();
            supported.addAll(Arrays.asList(acceptsTypes));

            if (logicalData.getMetadata() != null) {
                ArrayList<String> fileMimeTypes = logicalData.getMetadata().getMimeTypes();
                for (String fileMimeType : fileMimeTypes) {
                    type = MMTypeTools.bestMatch(supported, fileMimeType);
                    if (!StringUtil.isEmpty(type)) {
                        return type;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException {
        InputStream in = null;
        try {
            debug("sendContent.");
            debug("\t range: " + range);
            debug("\t params: " + params);
            debug("\t contentType: " + contentType);

            VFile vFile;
            if (!logicalData.hasPhysicalData()) {
                vFile = (VFile) logicalData.createPhysicalData();
            } else {
                vFile = (VFile) logicalData.getVFSNode();
            }

            in = vFile.getInputStream();
            
            if (range != null) {
                debug("sendContent: ranged content: " + vFile.getVRL());
                PartialGetHelper.writeRange(in, range, out);
            } else {
                debug("sendContent: send whole file " + vFile.getVRL());
                IOUtils.copy(in, out);
            }
            out.flush();

        } catch (VlException ex) {
            throw new IOException(ex);
        } finally {
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
        Path parent;
        Path tmpPath;


        debug("\t rDestgetName: " + rDest.getName() + " name: " + name);
//        if (rDest == null || rDest.getName() == null) {
//            debug("----------------Have to throw forbidden ");
//            throw new ForbiddenException(this);
//        }

        debug("\t rDestgetUniqueId: " + rDest.getUniqueId());
        Path newPath = Path.path(Path.path(rDest.getName()), name);
        if (newPath.isRelative()) {
            parent = logicalData.getLDRI().getParent();
            tmpPath = Path.path(parent, name);
            newPath = tmpPath;
        }
        try {
            debug("\t rename: " + logicalData.getLDRI() + " to " + newPath);
            catalogue.renameEntry(logicalData.getLDRI(), newPath);
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(rDest, ex.getMessage());
            }
        }
    }

    @Override
    public String processForm(Map<String, String> arg0,
            Map<String, FileItem> arg1) throws BadRequestException,
            NotAuthorizedException {
        debug("processForm.");
        debug("\t arg0: " + arg0);
        debug("\t arg1: " + arg1);
        return null;
    }

    protected void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + "." + logicalData.getLDRI() + ": " + msg);
//        log.debug(msg);
    }

    @Override
    public String getUniqueId() {
        return logicalData.getUID();
    }

    @Override
    public String getName() {
        return logicalData.getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        debug("authenticate.");
        debug("\t user: " + user);
        debug("\t password: " + password);
        return user;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        debug("authorise.");
        return true;
    }

    @Override
    public String getRealm() {
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        if (logicalData.getMetadata() != null && logicalData.getMetadata().getModifiedDate() != null) {
            return new Date(logicalData.getMetadata().getModifiedDate());
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        switch (request.getMethod()) {
            case GET:
                if (logicalData.isRedirectAllowed()) {
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
        if (logicalData.getMetadata() != null && logicalData.getMetadata().getCreateDate() != null) {
            return new Date(logicalData.getMetadata().getCreateDate());
        }
        return null;
    }
}
