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
public class LogicalData implements Cloneable {

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
    private Long checkSum;
    private Long lastValidationDate;

    public Boolean getSupervised() {
        return supervised;
    }

    public void setSupervised(Boolean supervised) {
        this.supervised = supervised;
    }

    public void updateSupervised(Boolean supervised) {
        this.supervised = supervised;
        try {
            catalogue.setFileSupervised(uid, supervised, null);
        } catch (Exception ex) {
            Logger.getLogger(LogicalData.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    public Path getLDRI() {
        if (parent.isEmpty() && ld_name.isEmpty() && datatype.equals(Constants.LOGICAL_FOLDER)) {
            return Path.root;
        } else if (parent.isEmpty()) {
            return Path.root.child(ld_name);
        } else {
            return Path.path(parent).child(ld_name);
        }
    }

    public Long getUID() {
        return this.uid;
    }

    public void setUID(Long uid) {
        this.uid = uid;
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getName() + "." + Path.path(parent).child(ld_name) + ": " + msg);
        }
    }

    public boolean isRedirectAllowed() {
        //Read policy and decide....
        return false;
    }

    public void setLDRI(Path ldri) {
        parent = ldri.getParent().toPath();
        ld_name = ldri.getName();
    }

    public void setLDRI(String parent, String name) {
        this.parent = parent;
        this.ld_name = name;
    }

    @XmlTransient
    public String getType() {
        return datatype;
    }

    public void setType(String type) {
        this.datatype = type;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getName() {
        return ld_name;
    }

    public void setName(String name) {
        this.ld_name = name;
    }

    @XmlTransient
    public Long getPdriGroupId() {
        return pdriGroupId;
    }

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

    public boolean equals(Object obj) {
        if (obj instanceof LogicalData) {
            return hashCode() == obj.hashCode();
        } else {
            return false;
        }
    }

    public int hashCode() {
        return uid.intValue();
    }

    public Long getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    public Long getModifiedDate() {
        return this.modifiedDate;
    }

    public void setModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Long getLength() {
        return this.ld_length;
    }

    public void setLength(Long length) {
        this.ld_length = length;
    }

    public List<String> getContentTypes() {
        if (decodedContentTypes == null) {
            decodedContentTypes = Arrays.asList(contentTypesStr.split(","));
        }
        return Collections.unmodifiableList(decodedContentTypes);
    }

    public String getContentTypesAsString() {
        return contentTypesStr;
    }

    public void setContentTypesAsString(String ct) {
        contentTypesStr = ct;
        decodedContentTypes = null;
    }

    public void addContentType(String contentType) {
        String ct[] = contentTypesStr.split(",");
        if (!Arrays.asList(ct).contains(contentType)) {
            contentTypesStr += contentTypesStr.isEmpty() ? contentType : "," + contentType;
        }
        decodedContentTypes = null;
    }

    @XmlTransient
    public Permissions getPermissions() {
        try {
            return catalogue.getPermissions(uid, ownerId, null);
        } catch (Exception ex) {
            Logger.getLogger(LogicalData.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }

    public void setPermissions(Permissions permissions) {
        try {
            catalogue.setPermissions(uid, permissions, null);
        } catch (Exception ex) {
            Logger.getLogger(LogicalData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean isFolder() {
        return datatype.equals(Constants.LOGICAL_FOLDER);
    }

    public String getOwner() {
        return ownerId;
    }

    public void setOwner(String owner) {
        ownerId = owner;
    }

    public void setChecksum(Long aLong) {
        this.checkSum = aLong;
    }

    public Long getChecksum() {
        return checkSum;
    }

    public void updateChecksum(Long aLong) {
        this.checkSum = aLong;
        try {
            catalogue.setFileChecksum(uid, aLong, null);
        } catch (Exception ex) {
            Logger.getLogger(LogicalData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setLastValidationDate(Long aLong) {
        this.lastValidationDate = aLong;
    }

    public Long getLastValidationDate() {
        return lastValidationDate;
    }

    public void updateLastValidationDate(Long aLong) {
        this.lastValidationDate = aLong;
        try {
            catalogue.setLastValidationDate(uid, aLong, null);
        } catch (Exception ex) {
            Logger.getLogger(LogicalData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
