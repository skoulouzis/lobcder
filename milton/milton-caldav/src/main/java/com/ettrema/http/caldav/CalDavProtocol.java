package com.ettrema.http.caldav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.DigestResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HandlerHelper;
import com.bradmcevoy.http.HttpExtension;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.WellKnownResourceFactory.WellKnownHandler;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.bradmcevoy.http.http11.CustomPostHandler;
import com.bradmcevoy.http.http11.auth.DigestResponse;
import com.bradmcevoy.http.values.CData;
import com.bradmcevoy.http.values.HrefList;
import com.bradmcevoy.http.values.ValueWriters;
import com.bradmcevoy.http.values.WrappedHref;
import com.bradmcevoy.http.webdav.PropFindPropertyBuilder;
import com.bradmcevoy.http.webdav.PropFindXmlGenerator;
import com.bradmcevoy.http.webdav.PropertyMap;
import com.bradmcevoy.http.webdav.PropertyMap.StandardProperty;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.bradmcevoy.property.PropertySource;
import com.ettrema.common.LogUtils;
import com.ettrema.http.CalendarCollection;
import com.ettrema.http.CalendarResource;
import com.ettrema.http.ICalResource;
import com.ettrema.http.acl.ACLHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class CalDavProtocol implements HttpExtension, PropertySource, WellKnownHandler {

    private static final Logger log = LoggerFactory.getLogger(CalDavProtocol.class);
    // Standard caldav properties
    public static final String CALDAV_NS = "urn:ietf:params:xml:ns:caldav";
    // For extension properties
    public static final String CALSERVER_NS = "http://calendarserver.org/ns/";
    public static final String APPLE_ICAL_NS = "http://apple.com/ns/ical/";
    private final Set<Handler> handlers;
    private final PropertyMap propertyMapCalDav;
    private final PropertyMap propertyMapCalServer;
    private final PropertyMap propertyMapAppleCal;
    private final SchedulingCustomPostHandler schedulingCustomPostHandler;
    private final List<CustomPostHandler> customPostHandlers;

    public CalDavProtocol(ResourceFactory resourceFactory, WebDavResponseHandler responseHandler, HandlerHelper handlerHelper, WebDavProtocol webDavProtocol) {
        propertyMapCalDav = new PropertyMap(CALDAV_NS);
        propertyMapCalDav.add(new CalenderDescriptionProperty());
        propertyMapCalDav.add(new CalendarDataProperty());
        propertyMapCalDav.add(new CalenderHomeSetProperty());
        propertyMapCalDav.add(new CalenderUserAddressSetProperty());
        //propertyMapCalDav.add(new ScheduleInboxProperty());
        //propertyMapCalDav.add(new ScheduleOutboxProperty());

        propertyMapCalServer = new PropertyMap(CALSERVER_NS);
        propertyMapCalServer.add(new CTagProperty());
//        propertyMapCalServer.add(new XMPPProperty());
        //propertyMapCalServer.add(new DropBoxProperty());
        //propertyMapCalServer.add(new NotificationProperty());
//        propertyMapCalServer.add(new NotificationsProperty());
        
        propertyMapAppleCal = new PropertyMap(APPLE_ICAL_NS);
        propertyMapAppleCal.add(new ColorProperty());

        handlers = new HashSet<Handler>();
        handlers.add(new ACLHandler(responseHandler, handlerHelper));

        handlers.add(new MkCalendarHandler(webDavProtocol.getMkColHandler(), webDavProtocol.getPropPatchHandler()));

        ValueWriters valueWriters = new ValueWriters();
        PropFindXmlGenerator gen = new PropFindXmlGenerator(valueWriters);
        webDavProtocol.addPropertySource(this);
        PropFindPropertyBuilder propertyBuilder = new PropFindPropertyBuilder(webDavProtocol.getPropertySources());

        //Adding supported reports
        webDavProtocol.addReport(new MultiGetReport(resourceFactory, propertyBuilder, gen));
        webDavProtocol.addReport(new ACLPrincipalPropSetReport());
        webDavProtocol.addReport(new PrincipalMatchReport());
        webDavProtocol.addReport(new PrincipalPropertySearchReport());
        //webDavProtocol.addReport(new ExpandPropertyReport());
        webDavProtocol.addReport(new CalendarQueryReport(propertyBuilder, gen));

        schedulingCustomPostHandler = new SchedulingCustomPostHandler();
        List<CustomPostHandler> l = new ArrayList<CustomPostHandler>();
        l.add(schedulingCustomPostHandler);
        customPostHandlers = Collections.unmodifiableList(l);
    }

    @Override
    public Set<Handler> getHandlers() {
        return Collections.unmodifiableSet(handlers);
    }

    //TODO: remove debug logging once it's working
    @Override
    public Object getProperty(QName name, Resource r) {
        log.trace("getProperty: {}", name.getLocalPart());
        Object o;
        if (propertyMapCalDav.hasProperty(name)) {
            o = propertyMapCalDav.getProperty(name, r);
        } else if( propertyMapAppleCal.hasProperty(name)) {
            o = propertyMapAppleCal.getProperty(name, r);
        } else {
            o = propertyMapCalServer.getProperty(name, r);
        }
		if(log.isTraceEnabled()) {
			log.trace("getProperty result : " + o + " for property: " + name.getLocalPart());
		}
        return o;
    }

    @Override
    public void setProperty(QName name, Object value, Resource r) {
        log.trace("setProperty: {}", name.getLocalPart());
        if (propertyMapCalDav.hasProperty(name)) {
            propertyMapCalDav.setProperty(name, r, value);
        } else if( propertyMapAppleCal.hasProperty(name)) {
            propertyMapAppleCal.setProperty(name, r, value);
        } else {
            propertyMapCalServer.setProperty(name, r, value);
        }
    }

    @Override
    public PropertyMetaData getPropertyMetaData(QName name, Resource r) {
        log.trace("getPropertyMetaData: {}", name.getLocalPart());
        if (propertyMapCalDav.hasProperty(name)) {
            return propertyMapCalDav.getPropertyMetaData(name, r);
        } else if( propertyMapAppleCal.hasProperty(name)) {
            return propertyMapAppleCal.getPropertyMetaData(name, r);
        } else {
            return propertyMapCalServer.getPropertyMetaData(name, r);
        }
    }

    @Override
    public void clearProperty(QName name, Resource r) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QName> getAllPropertyNames(Resource r) {
        log.trace("getAllPropertyNames");
        List<QName> list = new ArrayList<QName>();
        list.addAll(propertyMapCalDav.getAllPropertyNames(r));
        list.addAll(propertyMapCalServer.getAllPropertyNames(r));
        return list;
    }

    @Override
    public List<CustomPostHandler> getCustomPostHandlers() {
        return customPostHandlers;
    }

    class CalendarDataProperty implements StandardProperty<CData> {

        @Override
        public String fieldName() {
            return "calendar-data";
        }

        @Override
        public CData getValue(PropFindableResource res) {
            if (res instanceof ICalResource) {
                ICalResource ical = (ICalResource) res;
                return new CData(ical.getICalData());
            } else {
                return null;
            }
        }

        @Override
        public Class<CData> getValueClass() {
            return CData.class;
        }
    }

    /*
    <calendar-description xmlns='urn:ietf:params:xml:ns:caldav'/>
     */
    class CalenderDescriptionProperty implements StandardProperty<String> {

        @Override
        public String fieldName() {
            return "calendar-description";
        }

        @Override
        public String getValue(PropFindableResource res) {
            if (res instanceof CalendarResource) {
                CalendarResource ical = (CalendarResource) res;
                return ical.getCalendarDescription();
            } else {
                log.warn("getValue: not a ICalResource");
                return null;
            }

        }

        @Override
        public Class<String> getValueClass() {
            return String.class;
        }
    }

    /*
    <calendar-home-set xmlns='urn:ietf:params:xml:ns:caldav'>
    <href xmlns='DAV:'>/calendars/__uids__/admin</href>
    </calendar-home-set>     
     */
    class CalenderHomeSetProperty implements StandardProperty<HrefList> {

        @Override
        public String fieldName() {
            return "calendar-home-set";
        }

        @Override
        public HrefList getValue(PropFindableResource res) {
            if (res instanceof CalDavPrincipal) {
                return ((CalDavPrincipal) res).getCalendarHomeSet();
            } else {
                return null;
            }
        }

        @Override
        public Class<HrefList> getValueClass() {
            return HrefList.class;
        }
    }

    /* Scheduling support
    see : http://ietfreport.isoc.org/idref/draft-desruisseaux-caldav-sched/
    for details
    
    <calendar-user-address-set xmlns='urn:ietf:params:xml:ns:caldav'>
    <href xmlns='DAV:'>http://polaris.home.j2anywhere.com:8008/principals/users/admin/</href>
    <href xmlns='DAV:'>urn:uuid:admin</href>
    <href xmlns='DAV:'>http://polaris.home.j2anywhere.com:8008/principals/__uids__/admin/</href>
    <href xmlns='DAV:'>/principals/__uids__/admin/</href>
    <href xmlns='DAV:'>/principals/users/admin/</href>
    </calendar-user-address-set>
     */
    class CalenderUserAddressSetProperty implements StandardProperty<HrefList> {

        @Override
        public String fieldName() {
            return "calendar-user-address-set";
        }

        /**
        <C:calendar-user-address-set xmlns:D="DAV:"
        xmlns:C="urn:ietf:params:xml:ns:caldav">
        <D:href>mailto:bernard@example.com</D:href>
        <D:href>mailto:bernard.desruisseaux@example.com</D:href>
        </C:calendar-user-address-set>
         * @param res
         * @return
         */
        @Override
        public HrefList getValue(PropFindableResource res) {
            if (res instanceof CalDavPrincipal) {
                return ((CalDavPrincipal) res).getCalendarUserAddressSet();
            } else {
                return null;
            }
        }

        @Override
        public Class<HrefList> getValueClass() {
            return HrefList.class;
        }
    }

    /*
    <schedule-inbox-URL xmlns='urn:ietf:params:xml:ns:caldav'>
    <href xmlns='DAV:'>/calendars/__uids__/admin/inbox/</href>
    </schedule-inbox-URL>
     */
    class ScheduleInboxProperty implements StandardProperty<WrappedHref> {

        public String fieldName() {
            return "schedule-inbox-URL";
        }

        public WrappedHref getValue(PropFindableResource res) {
            if (res instanceof CalDavPrincipal) {
                String s = ((CalDavPrincipal) res).getScheduleInboxUrl();
                return new WrappedHref(s);
            } else {
                return null;
            }
        }

        public Class<WrappedHref> getValueClass() {
            return WrappedHref.class;
        }
    }

    /*
    <schedule-outbox-URL xmlns='urn:ietf:params:xml:ns:caldav'>
    <href xmlns='DAV:'>/calendars/__uids__/admin/outbox/</href>
    </schedule-outbox-URL>
     */
    class ScheduleOutboxProperty implements StandardProperty<WrappedHref> {

        public String fieldName() {
            return "schedule-outbox-URL";
        }

        public WrappedHref getValue(PropFindableResource res) {
            if (res instanceof CalDavPrincipal) {
                String s = ((CalDavPrincipal) res).getScheduleOutboxUrl();
                return new WrappedHref(s);
            } else {
                return null;
            }

        }

        public Class<WrappedHref> getValueClass() {
            return WrappedHref.class;
        }
    }

    /*
    <dropbox-home-URL xmlns='http://calendarserver.org/ns/'>
    <href xmlns='DAV:'>/calendars/__uids__/admin/dropbox/</href>
    </dropbox-home-URL>
     */
    class DropBoxProperty implements StandardProperty<WrappedHref> {

        public String fieldName() {
            return "dropbox-home-URL";
        }

        public WrappedHref getValue(PropFindableResource res) {
            if (res instanceof CalDavPrincipal) {
                String s = ((CalDavPrincipal) res).getDropBoxUrl();
                return new WrappedHref(s);
            } else {
                return null;
            }
        }

        public Class<WrappedHref> getValueClass() {
            return WrappedHref.class;
        }
    }

    /*
     * I think this property probably isnt necessary, but will wait until things
     * are stable.
     *
    <xmpp-uri xmlns='http://calendarserver.org/ns/'/>
     */
    class XMPPProperty implements StandardProperty<String> {

        public String fieldName() {
            return "xmpp-uri";
        }

        public String getValue(PropFindableResource res) {
            return "xmpp:romeo@montague.net";
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

    /*
    <notification-URL xmlns='http://calendarserver.org/ns/'>
    <href xmlns='DAV:'>/calendars/__uids__/admin/notification/</href>
    </notification-URL>
     */
    class NotificationsProperty implements StandardProperty<WrappedHref> {

        @Override
        public String fieldName() {
            return "notifications-URL";
        }

        @Override
        public WrappedHref getValue(PropFindableResource res) {
            return new WrappedHref("http://localhost:7080/notificationsUrl");
        }

        @Override
        public Class<WrappedHref> getValueClass() {
            return WrappedHref.class;
        }
    }

    class NotificationProperty implements StandardProperty<WrappedHref> {

        @Override
        public String fieldName() {
            return "notification-URL";
        }

        @Override
        public WrappedHref getValue(PropFindableResource res) {
            return new WrappedHref("http://localhost:7080/notificationUrl");
        }

        @Override
        public Class<WrappedHref> getValueClass() {
            return WrappedHref.class;
        }
    }

    /**
     *  CalendarServer support
     *
     *  https://trac.calendarserver.org/browser/CalendarServer/trunk/doc/Extensions/caldav-ctag.txt\
     *  http://code.google.com/p/sabredav/wiki/ICal
     *
     *
     * 4.1.  getctag WebDAV Property
    173
    174	   Name:  getctag
    175
    176	   Namespace:  http://calendarserver.org/ns/
    177
    178	   Purpose:  Specifies a "synchronization" token used to indicate when
    179	      the contents of a calendar or scheduling Inbox or Outbox
    180	      collection have changed.
    181
    182	   Conformance:  This property MUST be defined on a calendar or
    183	      scheduling Inbox or Outbox collection resource.  It MUST be
    184	      protected and SHOULD be returned by a PROPFIND DAV:allprop request
    185	      (as defined in Section 12.14.1 of [RFC2518]).
    186
    187	   Description:  The CS:getctag property allows clients to quickly
    188	      determine if the contents of a calendar or scheduling Inbox or
    189	      Outbox collection have changed since the last time a
    190	      "synchronization" operation was done.  The CS:getctag property
    191	      value MUST change each time the contents of the calendar or
    192	      scheduling Inbox or Outbox collection change, and each change MUST
    193	      result in a value that is different from any other used with that
    194	      collection URI.
    195
    196	   Definition:
    197
    198	       <!ELEMENT getctag #PCDATA>
    199
    200	   Example:
    201
    202	       <T:getctag xmlns:T="http://calendarserver.org/ns/"
    203	       >ABCD-GUID-IN-THIS-COLLECTION-20070228T122324010340</T:getctag>
     */
    class CTagProperty implements StandardProperty<String> {

        @Override
        public String fieldName() {
            return "getctag";
        }

        @Override
        public String getValue(PropFindableResource res) {
            if (res instanceof CalendarCollection) {
                CalendarCollection ccol = (CalendarCollection) res;
                return ccol.getCTag();
            } else {
                return null;
            }
        }

        @Override
        public Class<String> getValueClass() {
            return String.class;
        }
    }

    class ColorProperty implements PropertyMap.WritableStandardProperty<String> {

        @Override
        public String fieldName() {
            return "calendar-color";
        }

        @Override
        public String getValue(PropFindableResource res) {
            if (res instanceof CalendarResource) {
                CalendarResource ccol = (CalendarResource) res;
                return ccol.getColor();
            } else {
                return null;
            }
        }

        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public void setValue(PropFindableResource res, String value) {
            if (res instanceof CalendarResource) {
                CalendarResource ccol = (CalendarResource) res;
                ccol.setColor(value);
            }
        }
    }    
    
    @Override
    public String getWellKnownName() {
        return "caldav";
    }

    @Override
    public Resource locateWellKnownResource(Resource host) {
        log.trace("found a caldav well-known resource");
        return new CaldavWellKnownResource(host);
    }

    public class CaldavWellKnownResource implements DigestResource, GetableResource, PropFindableResource {

        private final Resource host;

        public CaldavWellKnownResource(Resource host) {
            this.host = host;
        }

        @Override
        public String getUniqueId() {
            return null;
        }

        @Override
        public String getName() {
            return getWellKnownName();
        }

        @Override
        public Object authenticate(String user, String password) {
            return host.authenticate(user, password);
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
            // we require a user, so we know where to redirect to
            return (auth != null);
        }

        @Override
        public String getRealm() {
            return host.getRealm();
        }

        @Override
        public Date getModifiedDate() {
            return null; // no caching
        }

        @Override
        public String checkRedirect(Request request) {
            log.trace("well-known: checkRedirect");
            Auth auth = request.getAuthorization();
            HrefList calendars;
            String first;
            if (auth != null && auth.getTag() != null) {
                if (auth.getTag() instanceof CalDavPrincipal) {
                    CalDavPrincipal p = (CalDavPrincipal) auth.getTag();
                    calendars = p.getCalendarHomeSet();
                    if (calendars == null || calendars.isEmpty()) {
                        log.warn("can't redirect, CalDavPrincipal.getCalendatHomeSet did not return an address. Check implementation class: " + p.getClass());
                        return null;
                    } else {
                        first = calendars.get(0); // just use first
                        LogUtils.trace(log, "well-known: checkRedirect. redirecting to:", first);
                        return first;
                    }
                } else {
                    log.warn("can't redirect, auth.getTag is not a CalDavPrincipal, is a: " + auth.getTag().getClass() + " To use CALDAV, the user object returned from authenticate must be a " + CalDavPrincipal.class);
                    return null;
                }
            } else {
                log.trace("can't redirect, no authorisation");
                return null;
            }
        }

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            if (host instanceof DigestResource) {
                DigestResource dr = (DigestResource) host;
                return dr.authenticate(digestRequest);
            } else {
                return null;
            }
        }

        @Override
        public boolean isDigestAllowed() {
            if (host instanceof DigestResource) {
                DigestResource dr = (DigestResource) host;
                return dr.isDigestAllowed();
            } else {
                return false;
            }
        }

        @Override
        public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Long getMaxAgeSeconds(Auth auth) {
            return null; // no caching
        }

        @Override
        public String getContentType(String accepts) {
            return null;
        }

        @Override
        public Long getContentLength() {
            return null;
        }

        @Override
        public Date getCreateDate() {
            return null;
        }
    }
}
