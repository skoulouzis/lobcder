/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import java.util.Date;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataResource implements PropFindableResource, Resource {

    public WebDataResource(IDLCatalogue catalogue, ILogicalData ch) {
    }

    @Override
    public Date getCreateDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getUniqueId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object authenticate(String user, String password) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRealm() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date getModifiedDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String checkRedirect(Request request) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}