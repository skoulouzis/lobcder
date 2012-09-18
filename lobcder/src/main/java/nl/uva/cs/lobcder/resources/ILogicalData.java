/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.Set;

/**
 *
 * @author S. Koulouzis
 */
public interface ILogicalData extends Cloneable {

    public Path getLDRI();

    public void setLDRI(Path path);
        
    public void setLDRI(String parent, String name);

    public Metadata getMetadata();

    public void setMetadata(Metadata metadata);

    public Long getUID();
    
    public Long getPdriGroupId();
    
    public void setPdriGroupId(Long pdriGroupId);
    
    public boolean isRedirectAllowed();

    public String getType();

    public String getParent();
    
    public String getName();

    public Object clone();
}