package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.CustomProperty;
import java.util.Date;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CustomPropertyResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import java.util.Set;
import nl.uva.cs.lobcder.catalogue.IDRCatalogue;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataResource implements PropFindableResource {

    private IDataResourceEntry nodeEntry;
//    private IDRCatalogue catalogue;
    private Logger log = LoggerFactory.getLogger(DataResource.class);

    public DataResource(IDataResourceEntry entry) {
        this.setNodeEntry(entry);
//        this.setCatalogue(catalogue);
        if (entry.getMetadata() == null) {
//            throw new RuntimeException(entry.getLDRI() + " has no metadata!");
        }
    }

    @Override
    public Object authenticate(String user, String pwd) {
        debug("User: " + user + " Password: " + pwd);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean authorise(Request arg0, Method arg1, Auth arg2) {
        debug("Request: " + arg0 + " Method: " + arg1 + " Auth: " + arg2);
        return true;
    }

    @Override
    public String checkRedirect(Request arg0) {
        debug("Request: " + arg0);
        return null;
    }

    @Override
    public Date getModifiedDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        return getNodeEntry().getLDRI().toString();
    }

    @Override
    public String getRealm() {
        return "relm";
    }

    @Override
    public String getUniqueId() {
        return "" + getNodeEntry().getUID();
    }

    @Override
    public Date getCreateDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + ": " + msg);
        log.debug(msg);
    }

    protected void setNodeEntry(IDataResourceEntry nodeEntry) {
        this.nodeEntry = nodeEntry;
    }

    protected IDataResourceEntry getNodeEntry() {
        return nodeEntry;
    }
}
