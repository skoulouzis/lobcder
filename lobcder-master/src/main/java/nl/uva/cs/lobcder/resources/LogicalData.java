package nl.uva.cs.lobcder.resources;

import nl.uva.cs.lobcder.util.Constants;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Arrays;
import java.util.List;

/**
 * User: dvasunin Date: 26.02.13 Time: 18:38 To change this template use File |
 * Settings | File Templates.
 */
@XmlRootElement
public class LogicalData implements Cloneable {

    private Long uid = Long.valueOf(0);
    @XmlTransient
    private String owner = "";
    private String type = "";
    private String name = "";
    private Long parentRef;
    private Long createDate = Long.valueOf(0);
    private Long modifiedDate = Long.valueOf(0);
    private Long lastAccessDate;
    private Integer ttlSec;
    private Long length = Long.valueOf(0);
    @XmlTransient
    private String contentTypesAsString = "";
    @XmlTransient
    private Long pdriGroupId = Long.valueOf(0);
    private Boolean supervised = Boolean.FALSE;
    private String checksum = "";
    private Long lastValidationDate = Long.valueOf(0);
    private String status;
    @XmlTransient
    private String lockTokenID;
    @XmlTransient
    private String lockScope;
    @XmlTransient
    private String lockType;
    @XmlTransient
    private String lockedByUser;
    @XmlTransient
    private String lockDepth;
    @XmlTransient
    private Long lockTimeout = Long.valueOf(0);
    private String description;
    private List<String> dataLocationPreferences;

//    @XmlElement
//    public List<String> getContentTypes() {
//        if(contentTypesAsString == null){
//            return null;
//        } else {
//            return  Arrays.asList(contentTypesAsString.split(","));
//        }
//    }
    public void addContentType(String contentType) {
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        if (getContentTypesAsString() == null) {
            setContentTypesAsString(new String());
        }
        String ct[] = getContentTypesAsString().split(",");
        if (!Arrays.asList(ct).contains(contentType)) {
            String cont;
            if (getContentTypesAsString().isEmpty()) {
                cont = ("," + contentType);
            } else {
                cont = getContentTypesAsString();
            }
            setContentTypesAsString(cont);
        }
    }

    public boolean isFolder() {
        return getType().equals(Constants.LOGICAL_FOLDER);
    }

    @Override
    public LogicalData clone() throws CloneNotSupportedException {
        return (LogicalData) super.clone();
    }

    /**
     * @return the uid
     */
    public Long getUid() {
        return uid;
    }

    /**
     * @param uid the uid to set
     */
    public void setUid(Long uid) {
        this.uid = uid;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the parentRef
     */
    public Long getParentRef() {
        return parentRef;
    }

    /**
     * @param parentRef the parentRef to set
     */
    public void setParentRef(Long parentRef) {
        this.parentRef = parentRef;
    }

    /**
     * @return the createDate
     */
    public Long getCreateDate() {
        return createDate;
    }

    /**
     * @param createDate the createDate to set
     */
    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    /**
     * @return the modifiedDate
     */
    public Long getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @param modifiedDate the modifiedDate to set
     */
    public void setModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @return the lastAccessDate
     */
    public Long getLastAccessDate() {
        return lastAccessDate;
    }

    /**
     * @param lastAccessDate the lastAccessDate to set
     */
    public void setLastAccessDate(Long lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    /**
     * @return the ttlSec
     */
    public Integer getTtlSec() {
        return ttlSec;
    }

    /**
     * @param ttlSec the ttlSec to set
     */
    public void setTtlSec(Integer ttlSec) {
        this.ttlSec = ttlSec;
    }

    /**
     * @return the length
     */
    public Long getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(Long length) {
        this.length = length;
    }

    /**
     * @return the contentTypesAsString
     */
    public String getContentTypesAsString() {
        return contentTypesAsString;
    }

    /**
     * @param contentTypesAsString the contentTypesAsString to set
     */
    public void setContentTypesAsString(String contentTypesAsString) {
        this.contentTypesAsString = contentTypesAsString;
    }

    /**
     * @return the pdriGroupId
     */
    public Long getPdriGroupId() {
        return pdriGroupId;
    }

    /**
     * @param pdriGroupId the pdriGroupId to set
     */
    public void setPdriGroupId(Long pdriGroupId) {
        this.pdriGroupId = pdriGroupId;
    }

    /**
     * @return the supervised
     */
    public Boolean getSupervised() {
        return supervised;
    }

    /**
     * @param supervised the supervised to set
     */
    public void setSupervised(Boolean supervised) {
        this.supervised = supervised;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the lastValidationDate
     */
    public Long getLastValidationDate() {
        return lastValidationDate;
    }

    /**
     * @param lastValidationDate the lastValidationDate to set
     */
    public void setLastValidationDate(Long lastValidationDate) {
        this.lastValidationDate = lastValidationDate;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the lockTokenID
     */
    public String getLockTokenID() {
        return lockTokenID;
    }

    /**
     * @param lockTokenID the lockTokenID to set
     */
    public void setLockTokenID(String lockTokenID) {
        this.lockTokenID = lockTokenID;
    }

    /**
     * @return the lockScope
     */
    public String getLockScope() {
        return lockScope;
    }

    /**
     * @param lockScope the lockScope to set
     */
    public void setLockScope(String lockScope) {
        this.lockScope = lockScope;
    }

    /**
     * @return the lockType
     */
    public String getLockType() {
        return lockType;
    }

    /**
     * @param lockType the lockType to set
     */
    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    /**
     * @return the lockedByUser
     */
    public String getLockedByUser() {
        return lockedByUser;
    }

    /**
     * @param lockedByUser the lockedByUser to set
     */
    public void setLockedByUser(String lockedByUser) {
        this.lockedByUser = lockedByUser;
    }

    /**
     * @return the lockDepth
     */
    public String getLockDepth() {
        return lockDepth;
    }

    /**
     * @param lockDepth the lockDepth to set
     */
    public void setLockDepth(String lockDepth) {
        this.lockDepth = lockDepth;
    }

    /**
     * @return the lockTimeout
     */
    public Long getLockTimeout() {
        return lockTimeout;
    }

    /**
     * @param lockTimeout the lockTimeout to set
     */
    public void setLockTimeout(Long lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the dataLocationPreferences
     */
    public List<String> getDataLocationPreferences() {
        return dataLocationPreferences;
    }

    /**
     * @param dataLocationPreferences the dataLocationPreferences to set
     */
    public void setDataLocationPreferences(List<String> dataLocationPreferences) {
        this.dataLocationPreferences = dataLocationPreferences;
    }
}
