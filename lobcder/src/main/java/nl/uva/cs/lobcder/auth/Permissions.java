/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author dvasunin
 */

@XmlRootElement
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
    
    public Set<String> getRead() {
        return new HashSet<String>(read);
    } 
    
    public void setRead(Set<String> read) {
        this.read = read;
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
    
 
    public Set<String> getWrite(){
        return new HashSet<String>(write);
    }
    
    public void setWrite(Set<String> write) {
        this.write = write;
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
