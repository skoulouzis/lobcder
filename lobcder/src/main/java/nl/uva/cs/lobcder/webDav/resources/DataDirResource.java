/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.Map.Entry;
import java.util.Set;
import nl.uva.cs.lobcder.catalogue.IDRCatalogue;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import nl.uva.cs.lobcder.resources.ResourceFileEntry;
import nl.uva.cs.lobcder.resources.ResourceFolderEntry;

public class DataDirResource extends DataResource implements
        com.bradmcevoy.http.FolderResource {

    public DataDirResource(IDataResourceEntry entry) {
        super(entry);
    }

    @Override
    public CollectionResource createCollection(String newName)
            throws NotAuthorizedException, ConflictException {
        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public Resource child(String name) {
        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public List<? extends Resource> getChildren() {
        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public Resource createNew(String arg0, InputStream arg1, Long arg2,
            String arg3) throws IOException, ConflictException {
        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public void copyTo(CollectionResource arg0, String arg1) {
        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public void delete() {
        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public Long getContentLength() {
        if (getNodeEntry().getMetadata() == null) {
            return null;
        }
        return getNodeEntry().getMetadata().getLength();
    }

    @Override
    public String getContentType(String accepts) {
        return "folder";
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
//        throw new RuntimeException("Not Implemented yet");
        if (auth != null) {
            debug("getMaxAgeSeconds. getCnonce " + auth.getCnonce());
            debug("getMaxAgeSeconds. " + auth.getNc());
            debug("getMaxAgeSeconds. getNc " + auth.getNonce());
            debug("getMaxAgeSeconds. " + auth.getPassword());
            debug("getMaxAgeSeconds. getPassword " + auth.getQop());
            debug("getMaxAgeSeconds. getRealm " + auth.getRealm());
            debug("getMaxAgeSeconds. getResponseDigest " + auth.getResponseDigest());
            debug("getMaxAgeSeconds. getUri " + auth.getUri());
            debug("getMaxAgeSeconds. getUser " + auth.getUser());
            debug("getMaxAgeSeconds. getScheme " + auth.getScheme().name());
            debug("getMaxAgeSeconds. getTag " + auth.getTag());
        }

        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException {
        if (range != null) {
            debug("sendContent. Start: " + range.getStart() + " Finish: " + range.getFinish());
            debug("sendContent. Start: " + range.getStart() + " Finish: " + range.getFinish());
        }

        Set<Entry<String, String>> set = params.entrySet();
        Iterator<Entry<String, String>> iter = set.iterator();
        while (iter.hasNext()) {
            Entry<String, String> e = iter.next();
            debug(e.getKey() + " : " + e.getValue());
        }
        debug("sendContent. contentType: " + contentType);



//        throw new RuntimeException("Not Implemented yet");
    }

    @Override
    public void moveTo(CollectionResource arg0, String arg1)
            throws ConflictException {
        throw new RuntimeException("Not Implemented yet");
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + ": " + msg);
    }
}
