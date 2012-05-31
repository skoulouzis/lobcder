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
        roles.add(token.hashCode());
        roles.add(2);
        roles.add(Permissions.ROOT_ADMIN);
        return roles;
    }
    
}
