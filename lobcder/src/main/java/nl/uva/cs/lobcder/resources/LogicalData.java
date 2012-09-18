/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import nl.uva.cs.lobcder.util.Constants;

/**
 *
 * JDO 2.0 introduces a new way of handling this situation, by detaching an
 * object from the persistence graph, allowing it to be worked on in the users
 * application. It can then be attached to the persistence graph later. The
 * first thing to do to use a class with this facility is to tag it as
 * "detachable". This is done by adding the attribute
 */
public class LogicalData implements ILogicalData, Cloneable {

    private Long uid;
    private String type;
    private String name;
    private String parent;
    private Long pdriGroupId;
    private Metadata metadata;
    
    
    private static final boolean debug = false;
    
    private static AtomicLong count = new AtomicLong();



    public LogicalData(Path ldri, String type) {
        this.name = ldri.getName() != null ? ldri.getName() : "";
        this.parent = ldri.getParent() != null ? ldri.getParent().toPath() : "";
        this.type = type;
        uid = count.incrementAndGet();        
        metadata = new Metadata();
        pdriGroupId = null;
    }

    public LogicalData(Path ldri, String type, Long uid, Metadata md) {
        this.name = ldri.getName() != null ? ldri.getName() : "";
        this.parent = ldri.getParent() != null ? ldri.getParent().toPath() : "";
        this.type = type;
        this.uid = uid;        
        metadata = md;
        pdriGroupId = null;
    }
        
    @Override
    public Path getLDRI() {
        if(parent.isEmpty() && name.isEmpty() && type.equals(Constants.LOGICAL_FOLDER) )
            return Path.root;
        else if(parent.isEmpty())
            return Path.root.child(name);
        else
            return Path.path(parent).child(name);
    }

    @Override
    public Metadata getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public Long getUID() {
        return this.uid;
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getName() + "." + Path.path(parent).child(name) + ": " + msg);
        }
    }

    @Override
    public boolean isRedirectAllowed() {
        //Read policy and decide....
        return false;
    }


    @Override
    public void setLDRI(Path ldri) {
        parent = ldri.getParent().toPath();
        name = ldri.getName();
    }
    
    @Override
    public void setLDRI(String parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getParent() {        
        return parent;           
    }

    @Override
    public String getName() {
        return name;
    }  

    @Override
    public Long getPdriGroupId() {
        return pdriGroupId;
    }
    
    @Override
    public void setPdriGroupId(Long pdriGroupId){        
        this.pdriGroupId = pdriGroupId;        
    }
    
    @Override
    public Object clone(){
        Metadata md = (Metadata) getMetadata().clone();
        LogicalData clone = new LogicalData(getLDRI(), type, uid, md);           
        clone.pdriGroupId = pdriGroupId;
        return clone;        
    }
    
    @Override
    public boolean equals(Object obj){
        if(obj instanceof LogicalData){
            return hashCode() == obj.hashCode();
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode(){
        return uid.intValue();
    }
    
}
