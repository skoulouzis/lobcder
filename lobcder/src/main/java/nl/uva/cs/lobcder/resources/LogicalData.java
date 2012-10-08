/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import nl.uva.cs.lobcder.authdb.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.util.Constants;

/**
 *
 * JDO 2.0 introduces a new way of handling this situation, by detaching an
 * object from the persistence graph, allowing it to be worked on in the users
 * application. It can then be attached to the persistence graph later. The
 * first thing to do to use a class with this facility is to tag it as
 * "detachable". This is done by adding the attribute
 */

@XmlRootElement
public class LogicalData implements ILogicalData, Cloneable {

    private Long uid = Long.valueOf(0);
    private String ownerId = "";
    @XmlTransient
    private String datatype = "";
    private String ld_name = "";
    private String parent = "";
    private Long createDate = Long.valueOf(0);
    private Long modifiedDate = Long.valueOf(0);
    private Long ld_length = Long.valueOf(0);
    private String contentTypesStr = "";
    @XmlTransient
    private Long pdriGroupId = Long.valueOf(0);
    @XmlTransient
    private JDBCatalogue catalogue;
    @XmlTransient
    private List<String> decodedContentTypes = null;
    @XmlTransient
    private static final boolean debug = false;
    @XmlTransient
    private Boolean supervised;

    public Boolean getSupervised() {
        return supervised;
    }

    public void setSupervised(Boolean supervised) {
        this.supervised = supervised;
    }

    public LogicalData(Path ldri, String datatype, JDBCatalogue catalogue) {
        this.ld_name = ldri.getName() != null ? ldri.getName() : "";
        this.parent = ldri.getParent() != null ? ldri.getParent().toPath() : "";
        this.datatype = datatype;
        uid = Long.valueOf(0);
        this.catalogue = catalogue;
    }

    public LogicalData(JDBCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    public LogicalData() {
    }
    
    @XmlTransient
    @Override
    public Path getLDRI() {
        if (parent.isEmpty() && ld_name.isEmpty() && datatype.equals(Constants.LOGICAL_FOLDER)) {
            return Path.root;
        } else if (parent.isEmpty()) {
            return Path.root.child(ld_name);
        } else {
            return Path.path(parent).child(ld_name);
        }
    }

    @Override
    public Long getUID() {
        return this.uid;
    }

    @Override
    public void setUID(Long uid) {
        this.uid = uid;
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getName() + "." + Path.path(parent).child(ld_name) + ": " + msg);
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
        ld_name = ldri.getName();
    }

    @Override
    public void setLDRI(String parent, String name) {
        this.parent = parent;
        this.ld_name = name;
    }

    @XmlTransient
    @Override
    public String getType() {
        return datatype;
    }

   
    @Override
    public void setType(String type) {
        this.datatype = type;
    }

    @Override
    public String getParent() {
        return parent;
    }

    @Override
    public void setParent(String parent) {
        this.parent = parent;
    }
        
    @Override
    public String getName() {
        return ld_name;
    }
    
    @Override
    public void setName(String name) {
        this.ld_name = name;
    }
    
    @XmlTransient
    @Override
    public Long getPdriGroupId() {
        return pdriGroupId;
    }

    @Override
    public void setPdriGroupId(Long pdriGroupId) {
        this.pdriGroupId = pdriGroupId;
    }

    @Override
    public Object clone() {
        LogicalData clone = new LogicalData(catalogue);
        clone.uid = uid;
        clone.ownerId = ownerId;
        clone.datatype = datatype;
        clone.ld_name = ld_name;
        clone.parent = parent;
        clone.createDate = createDate;
        clone.modifiedDate = modifiedDate;
        clone.ld_length = ld_length;
        clone.contentTypesStr = contentTypesStr;
        clone.pdriGroupId = pdriGroupId;
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LogicalData) {
            return hashCode() == obj.hashCode();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return uid.intValue();
    }

    @Override
    public Long getCreateDate() {
        return this.createDate;
    }

    @Override
    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    @Override
    public Long getModifiedDate() {
        return this.modifiedDate;
    }

    @Override
    public void setModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public Long getLength() {
        return this.ld_length;
    }

    @Override
    public void setLength(Long length) {
        this.ld_length = length;
    }

    @Override
    public List<String> getContentTypes() {
        if (decodedContentTypes == null) {
            decodedContentTypes = Arrays.asList(contentTypesStr.split(","));
        }
        return Collections.unmodifiableList(decodedContentTypes);
    }

    @Override
    public String getContentTypesAsString() {
        return contentTypesStr;
    }

    @Override
    public void setContentTypesAsString(String ct) {
        contentTypesStr = ct;
        decodedContentTypes = null;
    }

    @Override
    public void addContentType(String contentType) {
        String ct[] = contentTypesStr.split(",");
        if (!Arrays.asList(ct).contains(contentType)) {
            contentTypesStr += contentTypesStr.isEmpty() ? contentType : "," + contentType;
        }
        decodedContentTypes = null;
    }
    
    @XmlTransient
    @Override
    public Permissions getPermissions() {
        try {
            return catalogue.getPermissions(uid, ownerId, null);
        } catch (Exception ex) {
            Logger.getLogger(LogicalData.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }

    @Override
    public void setPermissions(Permissions permissions) {
        try {
            catalogue.setPermissions(uid, permissions, null);
        } catch (Exception ex) {
            Logger.getLogger(LogicalData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean isFolder() {
        return datatype.equals(Constants.LOGICAL_FOLDER);
    }

    @Override
    public String getOwner() {
        return ownerId;
    }

    @Override
    public void setOwner(String owner) {
        ownerId = owner;
    }
}
