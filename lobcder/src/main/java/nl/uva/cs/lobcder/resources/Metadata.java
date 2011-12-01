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
    private ArrayList<String> mimeTypes;

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

    public ArrayList<String> getMimeTypes() {
        return this.mimeTypes;
    }

    public void addMimeType(String mimeType) {
        if (mimeTypes == null) {
            mimeTypes = new ArrayList<String>();
        }
        if (mimeType != null && !mimeTypes.contains(mimeType)) {
            this.mimeTypes.add(mimeType);
        }

    }
}
