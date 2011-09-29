/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.ArrayList;

/**
 *
 * @author S. Koulouzis
 */
public interface IDataResourceEntry {

    public Path getLDRI();
        
    public ArrayList<IDataResourceEntry> getChildren();
    
    public void addChildren( ArrayList<IDataResourceEntry> children);
    
    public void addChild(IDataResourceEntry child);
    
    public ArrayList<StorageResource> getStorageResources();
    
    public void setStorageResource(ArrayList<StorageResource> storageResources);

    public Metadata getMetadata();

    public void setMetadata(Metadata metadata);

    public String getUID();
}
