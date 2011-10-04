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

    public ArrayList<Path> getChildren();

    public void addChildren(ArrayList<Path> children);

    public void addChild(Path child);

    public ArrayList<StorageResource> getStorageResources();

    public void setStorageResource(ArrayList<StorageResource> storageResources);

    public Metadata getMetadata();

    public void setMetadata(Metadata metadata);

    public String getUID();

    public boolean hasChildren();

    public void removeChild(Path childPath);
}
