package com.ettrema.http.acl;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import javax.xml.namespace.QName;

/**
 * Used to represent aggregated principals defined by the ACL spec
 *
 * Eg D:all, D:authenticated, D:unauthenticated
 *
 *
 * @author brad
 */
public class DavPrincipals {

    public static DavPrincipal All = new AllDavPrincipal();
    public static DavPrincipal AUTHENTICATED = new AllDavPrincipal();
    public static DavPrincipal UNAUTHENTICATED = new UnAuthenticatedDavPrincipal();

    public abstract static class AbstractDavPrincipal implements DavPrincipal {

        private final PrincipleId id;
        private final QName qname;

        public AbstractDavPrincipal( String name ) {
            this.qname = new QName( WebDavProtocol.NS_DAV.getName(), name );
            this.id = new PrincipleId() {

                public QName getIdType() {
                    return qname;
                }

                public String getValue() {
                    return null;
                }
            };
        }

        public PrincipleId getIdenitifer() {
            return id;
        }
    }

    public static class AllDavPrincipal extends DavPrincipals.AbstractDavPrincipal {

        AllDavPrincipal() {
            super( "all" );
        }

        public boolean matches( Auth auth, Resource current ) {
            return true;
        }
    }

    public static class AuthenticatedDavPrincipal extends DavPrincipals.AbstractDavPrincipal {

        AuthenticatedDavPrincipal() {
            super( "authenticated" );
        }

        public boolean matches( Auth auth, Resource current ) {
            return auth.getTag() != null;
        }
    }

    public static class UnAuthenticatedDavPrincipal extends DavPrincipals.AbstractDavPrincipal {

        UnAuthenticatedDavPrincipal() {
            super( "unauthenticated" );
        }

        public boolean matches( Auth auth, Resource current ) {
            return auth.getTag() == null;
        }
    }
}
