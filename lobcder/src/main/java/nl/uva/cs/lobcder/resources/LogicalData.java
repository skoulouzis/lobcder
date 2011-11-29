/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.Serializable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class LogicalData implements ILogicalData, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1529997963561059214L;
    @PrimaryKey
    @Persistent
    private String uid;
    @Persistent
    private Path ldri;
    @Persistent
    private Metadata metadata;
    @Persistent
    private ArrayList<Path> children;
    @Persistent
    private ArrayList<StorageSite> storageResource;
    private boolean debug = false;

    public LogicalData(Path ldri) {
        this.ldri = ldri;
        uid = java.util.UUID.randomUUID().toString();
    }

    @Override
    public Path getLDRI() {
        return this.ldri;
    }

    @Override
    public ArrayList<Path> getChildren() {
        return children;
    }

    @Override
    public ArrayList<StorageSite> getStorageSites() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata();
    }

    @Override
    public void setMetadata(Metadata metadata) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getUID() {
        return this.uid;
    }

    @Override
    public void addChildren(ArrayList<Path> children) {
        if (this.children == null) {
            this.children = children;
        } else {
            this.children.addAll(children);
        }
    }

    @Override
    public void setStorageSites(ArrayList<StorageSite> storageResource) {
        this.storageResource = storageResource;
    }

    @Override
    public void addChild(Path child) {
        if (this.children == null) {
            this.children = new ArrayList<Path>();
        }
        this.children.add(child);
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
        }
    }

    @Override
    public boolean hasChildren() {
        if (children != null && !children.isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public void removeChild(Path childPath) {
        children.remove(childPath);
    }

    @Override
    public Path getChild(Path path) {
        
        if (children != null && !children.isEmpty()) {
            for (Path p : children) {
                if (p.getName().equals(path.getName())) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public void setLDRI(Path path) {
        this.ldri = path;
    }

    @Override
    public boolean isRedirectAllowed() {
        //Read policy and decide....
        return false;
    }
}
