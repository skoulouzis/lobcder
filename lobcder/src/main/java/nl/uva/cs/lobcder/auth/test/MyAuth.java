/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nl.uva.cs.lobcder.auth.ExternAuthI;

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
    public Collection<Integer> checkToken(String token) {        
        List<Integer> roles = new ArrayList<Integer> ();
        roles.add(4);
        roles.add(2);
        roles.add(3);
        return roles;
    }
    
}
