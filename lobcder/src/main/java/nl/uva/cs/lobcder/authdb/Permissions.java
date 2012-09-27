/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.authdb;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author dvasunin
 */
public class Permissions {
    private Set<String> read = new HashSet<String>();
    private Set<String> write = new HashSet<String>();
    private String owner = "";
    
    public Permissions(){        
    }
    
    public Permissions(MyPrincipal mp){
        owner = mp.getUserId();
        read.addAll(mp.getRoles());
    }
    
    public String getOwner(){
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }
    public Set<String> canRead() {
        return read;
    }
    public Set<String> canWrite(){
        return write;
    }
}
