package nl.uva.cs.lobcder.rest.wrappers;

import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRIDescr;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * A wrapper for the logical data. It incudes all resource's properties
 *
 * @author dvasunin
 */
@XmlRootElement
public class LogicalDataWrapped {

    private String globalID;
    private LogicalData logicalData;
    private String path;
    private Permissions permissions;
    private List<PDRIDescr> pdriList = null;
    private Long uid;

    /**
     * @return the globalID
     */
    public String getGlobalID() {
        return globalID;
    }

    /**
     * @param globalID the globalID to set
     */
    public void setGlobalID(String globalID) {
        this.globalID = globalID;
    }

    /**
     * @return the logicalData
     */
    public LogicalData getLogicalData() {
        return logicalData;
    }

    /**
     * @param logicalData the logicalData to set
     */
    public void setLogicalData(LogicalData logicalData) {
        this.logicalData = logicalData;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the permissions
     */
    public Permissions getPermissions() {
        return permissions;
    }

    /**
     * @param permissions the permissions to set
     */
    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    /**
     * @return the pdriList
     */
    public List<PDRIDescr> getPdriList() {
        return pdriList;
    }

    /**
     * @param pdriList the pdriList to set
     */
    public void setPdriList(List<PDRIDescr> pdriList) {
        this.pdriList = pdriList;
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
}
