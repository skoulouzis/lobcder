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
import com.bradmcevoy.http.webdav.PropertyMap.WritableStandardProperty;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import com.bradmcevoy.property.PropertySource;
import com.ettrema.http.AccessControlledResource;
import com.ettrema.http.AccessControlledResource.Priviledge;
import com.ettrema.http.acl.DiscretePrincipal;
import com.ettrema.http.acl.Principal;
import com.ettrema.http.acl.PriviledgeList;
import com.ettrema.http.caldav.PrincipalSearchPropertySetReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.webDav.resources.WebDataResource;

/**
 * Copied from
 * <code>ACLProtocol</code> source cause we where getting back null values
 *
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
        propertyMap.add(new ACLProperty());

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
        Object prop = propertyMap.getProperty(qname, rsrc);
        debug("prop: " + prop + " " + prop.getClass().getName());
        return prop;
    }

    @Override
    public void setProperty(QName qname, Object o, Resource rsrc) throws PropertySetException, NotAuthorizedException {
        debug("setProperty: " + qname.getLocalPart());
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PropertyMetaData getPropertyMetaData(QName qname, Resource rsrc) {
        debug("getPropertyMetaData: " + qname.getLocalPart());
        PropertyMetaData prop = propertyMap.getPropertyMetaData(qname, rsrc);
        debug("prop: " + prop.getAccessibility().name());
        return prop;
    }

    @Override
    public void clearProperty(QName qname, Resource rsrc) throws PropertySetException, NotAuthorizedException {
        debug("clearProperty: " + qname.getLocalPart());
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QName> getAllPropertyNames(Resource rsrc) {
        debug("getAllPropertyNames");
        List<QName> list = new ArrayList<QName>();
        list.addAll(propertyMap.getAllPropertyNames(rsrc));
        for (QName n : list) {
            debug("Names: " + n);
        }
        return list;
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getName() + ": " + msg);
    }

    class PrincipalUrl implements StandardProperty<String> {

        @Override
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

        @Override
        public String fieldName() {
            return "principal-collection-set";
        }

        /**
         * - DAV:principal-collection-set - Collection of principals for this
         * server. For security and scalability reasons, a server MAY report only a
         * subset of the entire set of known principal collections, and therefore
         * clients should not assume they have retrieved an exhaustive listing. A
         * server MAY elect to report none of the principal collections it knows
         * about, in which case the property value would be empty.
         *
         * @return
         */
        @Override
        public WrappedHref getValue(PropFindableResource res) {
            if (res instanceof WebDataResource) {
                return new WrappedHref(((WebDataResource) res).getPrincipalCollectionHrefs().get(0));
            }
            return null;
        }

        @Override
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
            WebDataResource dataRes = (WebDataResource) res;
            HrefList hrefs = new HrefList();
            hrefs.add(dataRes.getPrincipalURL());
            return hrefs;

            //            Auth auth = HttpManager.request().getAuthorization();
            //            if (auth == null || auth.getTag() == null) {
            //                return null;
            //            } else {
            //                Object user = auth.getTag();
            //                debug("user: "+user.getClass().getName());
            //                if (user instanceof MyPrincipal) {
            //                    MyPrincipal p = (MyPrincipal) user;
            //                    HrefList hrefs = new HrefList();
            //                    hrefs.add(p.getPrincipalURL());
            //                    return hrefs;
            //                } else {
            //                    return null;
            //                }
            //            }
        }

        @Override
        public Class<HrefList> getValueClass() {
            return HrefList.class;
        }
    }

    class CurrentUserPrivledges implements StandardProperty<List<AccessControlledResource.Priviledge>> {

        @Override
        public String fieldName() {
            return Constants.DAV_CURRENT_USER_PRIVILAGE_SET_PROP_NAME.getLocalPart();
        }

        @Override
        public List<Priviledge> getValue(PropFindableResource res) {
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

    class ACLProperty implements WritableStandardProperty<Map<Principal, List<Priviledge>>> {

        @Override
        public String fieldName() {
            return "acl";
        }

        @Override
        public Class getValueClass() {
            debug("getValueClass: ");
            return Map.class;
        }

        @Override
        public void setValue(PropFindableResource pfr, Map<Principal, List<Priviledge>> t) {
            debug("setValue: " + pfr.getName());
            if (pfr instanceof AccessControlledResource) {
                AccessControlledResource acr = (AccessControlledResource) pfr;
                acr.setAccessControlList(t);
            }
        }

        @Override
        public Map<Principal, List<Priviledge>> getValue(PropFindableResource pfr) {
            debug("getValue: " + pfr.getName());
            if (pfr instanceof AccessControlledResource) {
                AccessControlledResource acr = (AccessControlledResource) pfr;
                return acr.getAccessControlList();
            }
            return null;
        }
    }
}
