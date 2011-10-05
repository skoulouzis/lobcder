package nl.uva.cs.lobcder.webDav.resources;

import java.util.Date;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataResource implements PropFindableResource {

    private IDataResourceEntry nodeEntry;
    private Logger log = LoggerFactory.getLogger(DataResource.class);
    private boolean debug = false;

    public DataResource(IDataResourceEntry entry) {
        debug("Init: " + entry.getUID() + " " + entry.getLDRI());
        this.setNodeEntry(entry);
    }

    @Override
    public Object authenticate(String user, String pwd) {
        debug("authenticate. User: " + user + " Password: " + pwd);
        debug("Returning: " + user);
        return user;
    }

    @Override
    public boolean authorise(Request arg0, Method arg1, Auth arg2) {
        debug("authorise. Request: " + arg0 + " Method: " + arg1 + " Auth: " + arg2);
        debug("Returning true");
        return true;
    }

    @Override
    public String checkRedirect(Request arg0) {
        debug("checkRedirect. Request: " + arg0);
        throw new RuntimeException("Not supported yet.");
    }

    @Override
    public Date getModifiedDate() {
        Long longDate = getNodeEntry().getMetadata().getModifiedDate();
        return new Date(longDate);
    }

    @Override
    public String getName() {
        debug("getName.");
        return getNodeEntry().getLDRI().getName();
    }

    @Override
    public String getRealm() {
        debug("getRealm.");
        throw new RuntimeException("Not supported yet.");
    }

    @Override
    public String getUniqueId() {
        debug("getUniqueId.");
        return getNodeEntry().getUID();
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        Long longDate = getNodeEntry().getMetadata().getCreateDate();
        return new Date(longDate);
    }

    protected void debug(String msg) {
        if(debug){
        System.err.println(this.getClass().getSimpleName() + ": " + msg);
//        log.debug(msg);
        }
    }

    protected void setNodeEntry(IDataResourceEntry nodeEntry) {
        this.nodeEntry = nodeEntry;
    }

    protected IDataResourceEntry getNodeEntry() {
        return nodeEntry;
    }
}
