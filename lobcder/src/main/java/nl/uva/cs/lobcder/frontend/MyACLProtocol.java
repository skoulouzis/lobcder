/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;



import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.http11.CustomPostHandler;
import com.bradmcevoy.http.values.HrefList;
import com.bradmcevoy.http.values.WrappedHref;
import com.bradmcevoy.http.webdav.PropertyMap;
import com.bradmcevoy.http.webdav.PropertyMap.StandardProperty;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import com.bradmcevoy.property.PropertySource;
import com.ettrema.http.AccessControlledResource;
import com.ettrema.http.AccessControlledResource.Priviledge;
import com.ettrema.http.acl.DiscretePrincipal;
import com.ettrema.http.acl.PriviledgeList;
import com.ettrema.http.caldav.PrincipalSearchPropertySetReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import nl.uva.cs.lobcder.util.Constants;

/**
 * Copied from <code>ACLProtocol</code> source cause we where getting back null values 
 * @author S. Koulouzis 
 */
class MyACLProtocol implements HttpExtension, PropertySource {

    private final PropertyMap propertyMap;

    public MyACLProtocol(WebDavProtocol webdav) {
        propertyMap = new PropertyMap(WebDavProtocol.NS_DAV.getName());
        propertyMap.add(new PrincipalUrl());
        propertyMap.add(new PrincipalCollectionSetProperty());
        propertyMap.add(new CurrentUserPrincipalProperty());
        propertyMap.add(new CurrentUserPrivledges());

        //log.debug("registering the ACLProtocol as a property source");
        webdav.addPropertySource(this);
        //Adding supported reports
        webdav.addReport(new PrincipalSearchPropertySetReport());
    }

    @Override
    public Set<Handler> getHandlers() {
        return Collections.EMPTY_SET;
    }

    @Override
    public List<CustomPostHandler> getCustomPostHandlers() {
        return null;
    }

    @Override
    public Object getProperty(QName qname, Resource rsrc) throws NotAuthorizedException {
        debug("getProperty: " + qname.getLocalPart());
        return propertyMap.getProperty(qname, rsrc);
    }

    @Override
    public void setProperty(QName qname, Object o, Resource rsrc) throws PropertySetException, NotAuthorizedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PropertyMetaData getPropertyMetaData(QName qname, Resource rsrc) {
        debug("getPropertyMetaData: " + qname.getLocalPart());
        return propertyMap.getPropertyMetaData(qname, rsrc);
    }

    @Override
    public void clearProperty(QName qname, Resource rsrc) throws PropertySetException, NotAuthorizedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QName> getAllPropertyNames(Resource rsrc) {
        debug("getAllPropertyNames");
        List<QName> list = new ArrayList<QName>();
        list.addAll(propertyMap.getAllPropertyNames(rsrc));
        return list;
    }

    private void debug(String msg) {
//        System.err.println(this.getClass().getName() + ": " + msg);
    }

    class PrincipalUrl implements StandardProperty<String> {

        public String fieldName() {
            return "principal-URL";
        }

        @Override
        public String getValue(PropFindableResource res) {
            if (res instanceof AccessControlledResource) {
                AccessControlledResource acr = (AccessControlledResource) res;
                String url = acr.getPrincipalURL();
                if (url != null) {
                    HrefList listOfOne = new HrefList();
                    listOfOne.add(url);
                    return listOfOne.toString();
                } else {
                    return null;
                }
            } else {
                //log.warn("requested property 'principal-url', but resource doesnt implement AccessControlledResource: " + res.getClass().getCanonicalName());
                return null;
            }
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

    class PrincipalCollectionSetProperty implements StandardProperty<WrappedHref> {

        public String fieldName() {
            return "principal-collection-set";
        }

        public WrappedHref getValue(PropFindableResource res) {
            WrappedHref wrappedHref = new WrappedHref("/principals");
            //log.error("HREF : "+wrappedHref.getValue());
            return wrappedHref;
        }

        public Class<WrappedHref> getValueClass() {
            return WrappedHref.class;
        }
    }

    class CurrentUserPrincipalProperty implements StandardProperty<HrefList> {

        @Override
        public String fieldName() {
            return "current-user-principal";
        }

        @Override
        public HrefList getValue(PropFindableResource res) {
            Auth auth = HttpManager.request().getAuthorization();
            if (auth == null || auth.getTag() == null) {
                return null;
            } else {
                Object user = auth.getTag();
                if (user instanceof DiscretePrincipal) {
                    DiscretePrincipal p = (DiscretePrincipal) user;
                    HrefList hrefs = new HrefList();
                    hrefs.add(p.getPrincipalURL());
                    return hrefs;
                } else {
                    return null;
                }
            }
        }

        @Override
        public Class<HrefList> getValueClass() {
            return HrefList.class;
        }
    }

    class CurrentUserPrivledges implements StandardProperty<List<AccessControlledResource.Priviledge>> {

        @Override
        public String fieldName() {
            return Constants.CURRENT_USER_PRIVILEGE_PROP_NAME;
        }

        @Override
        public List<AccessControlledResource.Priviledge> getValue(PropFindableResource res) {
            if (res instanceof AccessControlledResource) {
                AccessControlledResource acr = (AccessControlledResource) res;
                Auth auth = HttpManager.request().getAuthorization();
                List<Priviledge> list = acr.getPriviledges(auth);
//                PriviledgeList privs = new PriviledgeList(list);
                return list;//privs;
            } else {
                return null;
            }
        }

        @Override
        public Class<PriviledgeList> getValueClass() {
            return PriviledgeList.class;
        }
    }
}
