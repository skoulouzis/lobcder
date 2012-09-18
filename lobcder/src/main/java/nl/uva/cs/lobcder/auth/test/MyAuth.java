/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nl.uva.cs.lobcder.auth.ExternAuthI;
import nl.uva.cs.lobcder.auth.Permissions;

/**
 *
 * @author skoulouz
 */
public class MyAuth implements ExternAuthI{

    static MyAuth auth = new MyAuth();
    
    public static MyAuth getInstance(){
        return auth;
    }
    
    
    @Override
    public ArrayList<Integer> checkToken(String token) {   
        ArrayList<Integer> roles = new ArrayList<Integer> ();
        if(token.equals("token0")) {        
            roles.add(token.hashCode());
            roles.add(3);
            roles.add(Permissions.ROOT_ADMIN);
        } else if(token.equals("token1")) {
            roles.add(token.hashCode());
            roles.add(3);
        } else if(token.equals("token2")) {
            roles.add(token.hashCode());
            roles.add(4);
        } else {
            roles.add(token.hashCode());
        }
        return roles;
    }
    
}
