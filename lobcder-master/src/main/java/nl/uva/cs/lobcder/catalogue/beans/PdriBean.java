package nl.uva.cs.lobcder.catalogue.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigInteger;

/**
 * User: dvasunin Date: 01.03.13 Time: 13:57 To change this template use File |
 * Settings | File Templates.
 */
@XmlRootElement(name = "pdri")
@XmlAccessorType(XmlAccessType.FIELD)
public class PdriBean {

    private Long id;
    private String name;
    private BigInteger encryptionKey;
    private StorageSiteBean storage;

    public PdriBean(Long id, String name, BigInteger encryptionKey, StorageSiteBean storage) {
        this.id = id;
        this.name = name;
        this.encryptionKey = encryptionKey;
        this.storage = storage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PdriBean)) {
            return false;
        }
        PdriBean other = (PdriBean) o;
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
     * @return the encryptionKey
     */
    public BigInteger getEncryptionKey() {
        return encryptionKey;
    }

    /**
     * @param encryptionKey the encryptionKey to set
     */
    public void setEncryptionKey(BigInteger encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    /**
     * @return the storage
     */
    public StorageSiteBean getStorage() {
        return storage;
    }

    /**
     * @param storage the storage to set
     */
    public void setStorage(StorageSiteBean storage) {
        this.storage = storage;
    }
}
