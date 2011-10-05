/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import java.util.Date;

/**
 *
 * @author skoulouz
 */
class DummyResource implements com.bradmcevoy.http.Resource {

    public DummyResource() {
        debug("Init");
    }

    @Override
    public String getUniqueId() {
        debug("getUniqueId");
        return "UID";
    }

    @Override
    public String getName() {
        debug("getName");
        return "Name";
    }

    @Override
    public Object authenticate(String user, String password) {
        debug("authenticate");
        return "Name";
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        debug("authorise");
        return true;
    }

    @Override
    public String getRealm() {
         debug("getRealm");
        return "Camelot";
    }

    @Override
    public Date getModifiedDate() {
         debug("getModifiedDate");
        return new Date(System.currentTimeMillis());
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect");
        return null;
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getName() + ": " + msg);
    }
}
