/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author dvasunin
 */
public class MyPrincipal {
    private String token;
    private Integer uid;
    private Set<Integer> roles;
    private Date date;

    public class Exception extends java.lang.Exception {
        Exception(String reason){
            super(reason);
        }
    }
    
    
    public MyPrincipal(String token, List<Integer> blob) throws Exception{
        if(blob == null || blob.isEmpty())
            throw new Exception("Wrong parameter");
        uid = blob.iterator().next();
        roles = new HashSet<Integer>(blob.subList(1, blob.size()));               
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }

    public Integer getUid() {
        return uid;
    }


    public Set<Integer> getRoles() {
        return roles;
    }

    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
}
