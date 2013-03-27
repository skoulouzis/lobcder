package nl.uva.cs.lobcder.resources;

import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.util.Constants;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * User: dvasunin
 * Date: 26.02.13
 * Time: 18:38
 * To change this template use File | Settings | File Templates.
 */

@XmlRootElement
@Data
public class LogicalData implements Cloneable{
    private Long uid = Long.valueOf(0);
    @XmlTransient
    private String owner = "";
    private String type = "";
    private String name = "";
    private Long parentRef;
    private Long createDate = Long.valueOf(0);
    private Long modifiedDate = Long.valueOf(0);
    private Long length = Long.valueOf(0);
    @XmlTransient
    private String contentTypesAsString = "";
    @XmlTransient
    private Long pdriGroupId = Long.valueOf(0);
    private Boolean supervised = Boolean.FALSE;
    private Long checksum = Long.valueOf(0);
    private Long lastValidationDate = Long.valueOf(0);
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
    private String dataLocationPreference;


    @XmlElement
    public List<String> getContentTypes() {
        if(contentTypesAsString == null){
            return null;
        } else {
            return  Arrays.asList(contentTypesAsString.split(","));
        }
    }

    public void addContentType(String contentType) {
        String ct[] = contentTypesAsString.split(",");
        if (!Arrays.asList(ct).contains(contentType)) {
            contentTypesAsString += contentTypesAsString.isEmpty() ? contentType : ("," + contentType);
        }
    }

    public boolean isFolder() {
        return type.equals(Constants.LOGICAL_FOLDER);
    }

    public LogicalData clone() throws CloneNotSupportedException {
        return (LogicalData) super.clone();
    }
}