package nl.uva.cs.lobcder.catalogue.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

/**
 * Created by dvasunin on 26.02.15.
 */
@XmlRootElement(name = "pdri_group")
@XmlAccessorType(XmlAccessType.FIELD)
public class PdriGroupBean {

    private Long id;
    private Integer refCount;
    private Boolean needCheck;
    private Boolean bound;
    private Collection<PdriBean> pdri;
    private Collection<ItemBean> item;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PdriGroupBean)) {
            return false;
        }
        PdriGroupBean other = (PdriGroupBean) o;
        if (other.getId().equals(getId())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the refCount
     */
    public Integer getRefCount() {
        return refCount;
    }

    /**
     * @param refCount the refCount to set
     */
    public void setRefCount(Integer refCount) {
        this.refCount = refCount;
    }

    /**
     * @return the needCheck
     */
    public Boolean getNeedCheck() {
        return needCheck;
    }

    /**
     * @param needCheck the needCheck to set
     */
    public void setNeedCheck(Boolean needCheck) {
        this.needCheck = needCheck;
    }

    /**
     * @return the bound
     */
    public Boolean getBound() {
        return bound;
    }

    /**
     * @param bound the bound to set
     */
    public void setBound(Boolean bound) {
        this.bound = bound;
    }

    /**
     * @return the pdri
     */
    public Collection<PdriBean> getPdri() {
        return pdri;
    }

    /**
     * @param pdri the pdri to set
     */
    public void setPdri(Collection<PdriBean> pdri) {
        this.pdri = pdri;
    }

    /**
     * @return the item
     */
    public Collection<ItemBean> getItem() {
        return item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(Collection<ItemBean> item) {
        this.item = item;
    }
}
