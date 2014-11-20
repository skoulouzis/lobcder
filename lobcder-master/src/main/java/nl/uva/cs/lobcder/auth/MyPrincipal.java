/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author dvasunin
 */
public class MyPrincipal {

    @Getter private final String token;
    @Getter private final String userId;
    @Getter private final Set<String> roles;
    @Getter private final boolean admin;
    @Getter @Setter private Long validUntil;

    public MyPrincipal(String userId, Set<String> roles, String token) {
        this.userId = userId;
        this.roles = roles;
        admin = roles.contains("admin");
        this.token = token;
        roles.add(userId);
        roles.add(userId + "_dev");
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
        if (isAdmin()) {
            return true;
        }
        Set<String> r1 = new HashSet<String>(roles);
        r1.retainAll(p.getRead());
        return !r1.isEmpty();
    }

    public boolean canWrite(Permissions p) {
        if (p.getOwner().equals(userId)) {
            return true;
        }
        if (isAdmin()) {
            return true;
        }
        Set<String> r1 = new HashSet<String>(roles);
        r1.retainAll(p.getWrite());
        return !r1.isEmpty();
    }
}
