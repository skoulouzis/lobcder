/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author dvasunin
 */

public class MyPrincipal {

    
    private final String token;
    private final String userId;
    private final Set<String> roles;
    private final boolean admin;
    private Long validUntil;

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
        for (String s : getRoles()) {
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
        if (p.getOwner().equals(getUserId())) {
            return true;
        }
        if (isAdmin()) {
            return true;
        }
        Set<String> r1 = new HashSet<>(getRoles());
        r1.retainAll(p.getRead());
        return !r1.isEmpty();
    }

    public boolean canWrite(Permissions p) {
        if (p.getOwner().equals(getUserId())) {
            return true;
        }
        if (isAdmin()) {
            return true;
        }
        Set<String> r1 = new HashSet<>(getRoles());
        r1.retainAll(p.getWrite());
        return !r1.isEmpty();
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @return the admin
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * @return the roles
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * @return the validUntil
     */
    public Long getValidUntil() {
        return validUntil;
    }

    /**
     * @param validUntil the validUntil to set
     */
    public void setValidUntil(Long validUntil) {
        this.validUntil = validUntil;
    }
}
