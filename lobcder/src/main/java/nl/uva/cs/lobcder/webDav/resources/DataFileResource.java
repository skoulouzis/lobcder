/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.ContentTypeUtils;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import nl.uva.cs.lobcder.catalogue.IDRCatalogue;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import nl.uva.cs.lobcder.resources.Metadata;

/**
 *
 * @author S. Koulouzis
 */
public class DataFileResource implements
        com.bradmcevoy.http.FileResource {

    private final IDRCatalogue catalogue;
    private final IDataResourceEntry entry;

    public DataFileResource(IDRCatalogue catalogue, IDataResourceEntry entry) {
        this.catalogue = catalogue;
        this.entry = entry;
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) {
        throw new RuntimeException(
                "Not Implemented yet. Args: CollectionResource: "
                + collectionResource + ", name: " + name);
    }

    @Override
    public void delete() {
    }

    @Override
    public Long getContentLength() {
        Metadata meta = entry.getMetadata();
        if (meta != null) {
            return meta.getLength();
        }
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);
        String type = null;
        Metadata meta = entry.getMetadata();
        if (meta != null) {
            String mime = meta.getMimeType();
            debug("getContentType: mime: " + mime);
            type = mime;
            if (accepts != null && !accepts.equals("")) {
                type = ContentTypeUtils.findAcceptableContentType(mime, accepts);
            }
        } else {
            debug("Metadata is NULL!!!");
        }

        debug("getContentType: type: " + type);

        return type;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
//        throw new RuntimeException("Not Implemented yet: Args: auth: " + auth);
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException {
        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public void moveTo(CollectionResource rDest, String name)
            throws ConflictException {
        // catalogue.renameEntry();
        throw new RuntimeException("Not Implemented yet. Args, DestName: "
                + rDest.getName() + " fileName?:" + name);
    }

    @Override
    public String processForm(Map<String, String> arg0,
            Map<String, FileItem> arg1) throws BadRequestException,
            NotAuthorizedException {
        throw new RuntimeException("Not Implemented yet");
    }

    protected void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + "." + entry.getLDRI() + ": " + msg);
//        log.debug(msg);
    }

    @Override
    public String getUniqueId() {
        return entry.getUID();
    }

    @Override
    public String getName() {
        return entry.getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        return null;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return true;
    }

    @Override
    public String getRealm() {
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }
}
