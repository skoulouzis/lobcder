/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.util.Collection;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vrs.ServerInfo;

/**
 *
 * @author S. Koulouzis
 */

public interface IStorageSite {

    String getEndpoint();

    Collection<String> getVPHUsernames();

    Collection<String> getLogicalPaths();
    
    boolean LDRIHasPhysicalData(String ldri)throws VlException;
    
    VFSNode getVNode(String path) throws VlException;
        
    VFSNode createVFSFile(String path) throws VlException;

    public Credential getCredentials();

    public void deleteVNode(String lDRI)throws VlException;

    public ServerInfo getInfo();
    
    public VFSClient getVfsClient() throws VlException;

    public void removeLogicalPath(String pdrI);
    
    public String getVPHUsernamesCSV();
    
    public String getUid();
}
