/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.authdb.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.PropertiesLoader;

/**
 *
 * @author skoulouz
 */
public class MyAuth {
    
    static MyAuth auth = new MyAuth();
    
    public static MyAuth getInstance() {
        return auth;
    }
    
    public MyPrincipal checkToken(String token) {
        try {
            HashSet<String> roles = new HashSet<String>();
            if (token.equals("token0")) {
                roles.add("megarole1");
                roles.add("other");
                roles.add("admin");
            } else if (token.equals("token1")) {
                roles.add("other");
            } else {
                File f = new File(Constants.LOBCDER_CONF_DIR + "lob-users.prop");
                Properties props = PropertiesLoader.getProperties(f);
                for (int i = 0; i < 5; i++) {
                    String pass = props.getProperty("password" + i);
                    if (pass != null && pass.equals(token)) {
                        String[] userRoles = props.getProperty("roles" + i).split(",");
                        roles.addAll(Arrays.asList(userRoles));
                    }
                }
            }
            
            MyPrincipal principal = new MyPrincipal(token, roles);
            return principal;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MyAuth.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MyAuth.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
