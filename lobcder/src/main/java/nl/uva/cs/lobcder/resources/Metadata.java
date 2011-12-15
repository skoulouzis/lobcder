/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.Serializable;
import java.util.ArrayList;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

/**
 *
 * @author S. Koulouzis
 */
@PersistenceCapable
public class Metadata implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -7617145607817495167L;
    @Persistent
    private Long createDate = null;
    @Persistent
    private Long modifiedDate;
    @Persistent
    private Long length;
    @Persistent
    private ArrayList<String> contentTypes;

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
        return this.length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public ArrayList<String> getContentTypes() {
        return this.contentTypes;
    }

    public void addContentType(String contentType) {
        if (contentTypes == null) {
            contentTypes = new ArrayList<String>();
        }
        if (contentType != null && !contentTypes.contains(contentType)) {
            this.contentTypes.add(contentType);
        }

    }
}
