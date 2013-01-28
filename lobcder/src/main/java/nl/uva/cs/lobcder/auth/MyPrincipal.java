/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.PropertiesLoader;

/**
 *
 * @author dvasunin
 */
public class MyPrincipal {

    private String userId;
    private Set<String> roles;

    public MyPrincipal(String userId, Set<String> roles) {
//        try {
            this.userId = userId;
            this.roles = roles;


//            //This is here becuse at the moment we don't have any roles 
//            File f = new File(Constants.LOBCDER_CONF_DIR + "lob-users.prop");
//            Properties props = PropertiesLoader.getProperties(f);
//            for (int i = 0; i < 15; i++) {
//                String user = props.getProperty("username" + i);
//                if (user != null && user.equals(userId)) {
//                    String[] userRoles = props.getProperty("roles" + i).split(",");
//                    this.roles.addAll(Arrays.asList(userRoles));
//                }
//            }
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(MyPrincipal.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(MyPrincipal.class.getName()).log(Level.SEVERE, null, ex);
//        }
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
        r1.retainAll(p.canRead());
        return !r1.isEmpty();
    }

    public boolean canWrite(Permissions p) {
        if (p.getOwner().equals(userId)) {
            return true;
        }
        Set<String> r1 = new HashSet<String>(roles);
        r1.retainAll(p.canWrite());
        return !r1.isEmpty();
    }
}
