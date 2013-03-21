/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import com.bradmcevoy.http.webdav.WebDavProtocol;
import com.ettrema.http.acl.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 *
 * @author dvasunin
 */
public class MyPrincipal implements Principal{

    private String userId;
    private Set<String> roles;

    public MyPrincipal(String userId, Set<String> roles) {
        this.userId = userId;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getRolesStr() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : roles) {
            if (first) {
                sb.append(s);
                first = false;
            } else {
                sb.append(',').append(s);
            }
        }
        return sb.toString();
    }

    public boolean canRead(Permissions p) {
        if (p.getOwner().equals(userId)) {
            return true;
        }
        Set<String> r1 = new HashSet<String>(roles);
        if (r1.contains("admin")) {
            return true;
        }
        r1.retainAll(p.getRead());
        return !r1.isEmpty();
    }

    public boolean canWrite(Permissions p) {
        if (p.getOwner().equals(userId)) {
            return true;
        }
        Set<String> r1 = new HashSet<String>(roles);
        if (r1.contains("admin")) {
            return true;
        }
        r1.retainAll(p.getWrite());
        return !r1.isEmpty();
    }

    @Override
    public PrincipleId getIdenitifer() {
        return new PrincipleId() {

            @Override
            public QName getIdType() {
                return new QName( WebDavProtocol.NS_DAV.getName(), userId);
            }

            @Override
            public String getValue() {
                return userId;
            }
        };
        
    }
}
