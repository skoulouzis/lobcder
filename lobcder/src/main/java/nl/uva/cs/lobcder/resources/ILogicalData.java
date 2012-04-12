/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.AbstractCollection;
import java.util.Collection;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;

/**
 *
 * @author S. Koulouzis
 */
public interface ILogicalData {

    public Path getLDRI();
    
    public Collection<Path> getChildren();

    public void addChildren(Collection<Path> children);

    public void addChild(Path child);

    public AbstractCollection<IStorageSite> getStorageSites();

    public void setStorageSites(AbstractCollection<IStorageSite> storageResources);

    public Metadata getMetadata();

    public void setMetadata(Metadata metadata);

    public String getUID();

    public boolean hasChildren();

    public void removeChild(Path childPath);
    
    public void removeChildren(Collection<Path> childPath);

    public Path getChild(Path path);

    public void setLDRI(Path path);

    public boolean isRedirectAllowed();

    public VFSNode getVFSNode() throws VlException;

    public boolean hasPhysicalData()throws VlException;

    public VFSNode createPhysicalData()throws VlException;

    public void removeStorageSites();

    public void setChildren(Collection<Path> children);
    
    public String getType();
    
}
