/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import nl.uva.cs.lobcder.authdb.*;

/**
 *
 * @author skoulouz
 */
public class MyAuth {

    static MyAuth auth = new MyAuth();
    
    public static MyAuth getInstance(){
        return auth;
    }
    
    
    public MyPrincipal checkToken(String token) {           
        HashSet<String> roles = new HashSet<String> ();
        if(token.equals("token0")) {        
            roles.add("megarole1");
            roles.add("other");
            roles.add("admin");
        } else if(token.equals("token1")) {
            roles.add("other");
        } 
        
        MyPrincipal principal = new MyPrincipal(token, roles);
        return principal;
    }
    
}
