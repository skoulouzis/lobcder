/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jdo.annotations.*;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;

/**
 *
 * JDO 2.0 introduces a new way of handling this situation, by detaching an
 * object from the persistence graph, allowing it to be worked on in the users
 * application. It can then be attached to the persistence graph later. The
 * first thing to do to use a class with this facility is to tag it as
 * "detachable". This is done by adding the attribute
 */
@PersistenceCapable(detachable = "true")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class LogicalData implements ILogicalData, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1529997963561059214L;
    @PrimaryKey
//    @Persistent(valueStrategy= IdGeneratorStrategy.UUIDSTRING)
    @Persistent
    private String uid;
    //When an object is retrieved from the datastore by JDO typically not all 
    //fields are retrieved immediately. This is because for efficiency purposes 
    //only particular field types are retrieved in the initial access of the 
    //object, and then any other objects are retrieved when accessed 
    //(lazy loading). The group of fields that are loaded is called a fetch 
    //group
    @Persistent(defaultFetchGroup = "true")
    private Path ldri;
    @Persistent(defaultFetchGroup = "true")
    private Path pdri;
    
    @Persistent
    private String strLDRI;
    @Persistent
    private int ldriLen;
    @Persistent(defaultFetchGroup = "true")
    private String parent;
    @Persistent(defaultFetchGroup = "true")
    private Metadata metadata;
    @Persistent(defaultFetchGroup = "true")
    private Collection<String> children;
    @Persistent(defaultFetchGroup = "true")
    private String type;
    private boolean debug = false;
    @Persistent(defaultFetchGroup = "true")
    @Join
    @Element(types = nl.uva.cs.lobcder.resources.StorageSite.class)
    @Order(column = "STORAGE_SITE")
    private Collection<IStorageSite> storageSites;

    public LogicalData(Path ldri, String type) {
        this.ldri = ldri;
        strLDRI = ldri.toString();
        ldriLen = ldri.getLength();
        
        this.type = type;
        //Data will hold the same pdri for ever.
        pdri = ldri;
        uid = pdri.toString();//new StringIdentity(this.getClass(), java.util.UUID.randomUUID().toString()).getKey();
        Path parentPath = ldri.getParent();
        if (parentPath != null && !StringUtil.isEmpty(parentPath.toString())) {
            parent = parentPath.toString();
        } else {
            parent = Path.root.toString();
        }

    }

    @Override
    public Path getLDRI() {
        return this.ldri;
    }

    @Override
    public Collection<String> getChildren() {
        return children;
    }

    @Override
    public Collection<IStorageSite> getStorageSites() {
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
    public void addChildren(Collection<String> children) {
        if (this.children == null) {
            this.children = children;
        } else {
            this.children.addAll(children);
        }
    }

    @Override
    public void setStorageSites(Collection<IStorageSite> storageSites) {
        if (storageSites != null) {//&& !storageSites.isEmpty()) {
            this.storageSites = storageSites;
            debug("StorageSite num : " + this.storageSites.size());
        }
    }

    @Override
    public void addChild(Path child) {
        if (this.children == null) {
            this.children = new CopyOnWriteArrayList<String>();
        }
        this.children.add(child.toString());
    }

    private void debug(String msg) {
//        if (debug) {
//            System.err.println(this.getClass().getName() + "." + this.ldri + ": " + msg);
//        }
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
        if (children != null && !children.isEmpty()) {
            children.remove(childPath.toString());
        }
    }

    @Override
    public Path getChild(Path path) {

        if (children != null && !children.isEmpty()) {
            for (String p : children) {
                if (p.toString().equals(path.toString())) {
                    return Path.path(p);
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
    public VFSNode getVFSNode() throws VlException {
        for (IStorageSite s : this.storageSites) {
            if (s != null && s.LDRIHasPhysicalData(pdri)) {
                return s.getVNode(pdri);
            }
        }
        return null;
    }

    @Override
    public boolean hasPhysicalData() throws VlException {
        if (storageSites != null && !storageSites.isEmpty()) {
            for (IStorageSite s : storageSites) {
                return s.LDRIHasPhysicalData(pdri);
            }
        }
        return false;
    }

    @Override
    public VFSNode createPhysicalData() throws VlException {
        if (this.storageSites == null || this.storageSites.isEmpty()) {
            return null;
        }
        for (IStorageSite s : this.storageSites) {
            if (s != null) {
                return s.createVFSFile(pdri);
            }
        }
        return null;
    }

    @Override
    public void setLDRI(Path ldri) {
        this.ldri = ldri;
        this.strLDRI = ldri.toString();
    }

    @Override
    public void removeChildren(Collection<String> childPath) {
        if (children != null || !children.isEmpty()) {
            this.children.removeAll(childPath);
        }
    }

    @Override
    public void removeStorageSites() {
        this.storageSites.clear();
    }

    @Override
    public void setChildren(Collection<String> children) {
        this.children = children;
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
    public Path getPDRI() {
        return pdri;
    }

    @Override
    public void setPDRI(Path pdrI) {
        this.pdri = pdrI;
    }

    @Override
    public String getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getStrLDRI() {
        return strLDRI;
    }

    @Override
    public void setUID(String uid) {
        this.uid = uid;
    }
}
