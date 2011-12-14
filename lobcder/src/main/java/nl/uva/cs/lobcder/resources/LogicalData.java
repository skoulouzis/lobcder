/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.Serializable;

import java.util.ArrayList;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;

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
    private ArrayList<StorageSite> storageSites;
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
        return this.storageSites;
    }

    @Override
    public Metadata getMetadata() {
        if (this.metadata == null) {
            Metadata meta = new Metadata();
            return meta;
        }
        return this.metadata;
    }

    @Override
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
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
    public void setStorageSites(ArrayList<StorageSite> storageSites) {
        this.storageSites = storageSites;
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
    public boolean isRedirectAllowed() {
        //Read policy and decide....
        return false;
    }

    @Override
    public VFSNode getVNode() throws VlException {
        StorageSite site = null;
        for (StorageSite s : this.storageSites) {
            if (s != null) {
                site = s;
                break;
            }
        }
        return site.getVNode(this.getLDRI());
    }

    @Override
    public boolean hasPhysicalData() throws VlException {
        if (storageSites != null && !storageSites.isEmpty()) {
            for (StorageSite s : storageSites) {
                if (s.LDRIHasPhysicalData(ldri)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public VFSNode createPhysicalData() throws VlException {
        StorageSite site = null;
        if (this.storageSites == null || this.storageSites.isEmpty()) {
            return null;
        }
        for (StorageSite s : this.storageSites) {
            if (s != null) {
                site = s;
                break;
            }
        }
        return site.createVFSFile(getLDRI());
    }

    @Override
    public void setLDRI(Path ldri) {
        this.ldri = ldri;
    }
}
