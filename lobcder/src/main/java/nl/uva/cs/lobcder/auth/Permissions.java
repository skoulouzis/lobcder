/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author dvasunin
 */
@XmlAccessorType(XmlAccessType.NONE)
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
    
    @XmlElement
    public String getOwner(){
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }    
    
    @XmlElement
    public Set<String> getCanRead() {
        return read;
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
    
    @XmlElement
    public Set<String> getCanWrite(){
        return write;
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
