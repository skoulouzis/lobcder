/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.List;
import java.util.Set;
import nl.uva.cs.lobcder.authdb.Permissions;

/**
 *
 * @author S. Koulouzis
 */
public interface ILogicalData extends Cloneable {

    public Path getLDRI();

    public void setLDRI(Path path);
        
    public void setLDRI(String parent, String name);
    
    public Long getCreateDate();
    
    public void setCreateDate(Long createDate);
    
    public Long getModifiedDate();
    
    public void setModifiedDate(Long modifiedDate);
    
    public Long getLength();
    
    public void setLength(Long length);
    
    public List<String> getContentTypes();
    
    public String getContentTypesAsString();
    
    public void setContentTypesAsString(String ct);

    public void addContentType(String contentType);
    
    public Permissions getPermissions();
    
    public void setPermissions(Permissions permissions);
    
    public String getOwner();
    
    public void setOwner(String owner);

    public Long getUID();
    
    public void setUID(Long uid);
    
    public Long getPdriGroupId();
    
    public void setPdriGroupId(Long pdriGroupId);
    
    public boolean isRedirectAllowed();

    public String getType();
    
    public void setType(String type);

    public String getParent();
    
    public void setParent(String parent);
    
    public String getName();
    
    public void setName(String name);

    public Object clone();
    
    public boolean isFolder();
}