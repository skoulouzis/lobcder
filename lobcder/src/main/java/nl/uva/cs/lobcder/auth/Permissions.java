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
    
    public String getReadStr(){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String s : read){
            if(first){
                sb.append(s);
                first = false;
            } else {
                sb.append(',').append(s);                
            }
        }
        return sb.toString();
    }
    
    public Set<String> canWrite(){
        return write;
    }
    
    public String getWriteStr(){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String s : write){
            if(first){
                sb.append(s);
                first = false;
            } else {
                sb.append(',').append(s);                
            }
        }
        return sb.toString();
    }
}
