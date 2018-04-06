/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue.beans;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author S. Koulouzis
 */

@XmlRootElement(name="site")
@XmlAccessorType(XmlAccessType.FIELD)
public class StorageSiteBean {
    private Long id;
    private String uri;
    private Boolean encrypt;
    private Boolean cache;
    private String extra;
    private CredentialBean credential;

    public StorageSiteBean(long id, String uri, Boolean encrypt, Boolean cache, String extra, CredentialBean credential) {
        this.id = id;
        this.uri = uri;
        this.encrypt = encrypt;
        this.cache = cache;
        this.extra = extra;
        this.credential = credential;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof StorageSiteBean)) return false;
        StorageSiteBean other = (StorageSiteBean) o;
        if (other.getId().equals(getId())) return true;
        else return false;
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
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the encrypt
     */
    public Boolean getEncrypt() {
        return encrypt;
    }

    /**
     * @param encrypt the encrypt to set
     */
    public void setEncrypt(Boolean encrypt) {
        this.encrypt = encrypt;
    }

    /**
     * @return the cache
     */
    public Boolean getCache() {
        return cache;
    }

    /**
     * @param cache the cache to set
     */
    public void setCache(Boolean cache) {
        this.cache = cache;
    }

    /**
     * @return the extra
     */
    public String getExtra() {
        return extra;
    }

    /**
     * @param extra the extra to set
     */
    public void setExtra(String extra) {
        this.extra = extra;
    }

    /**
     * @return the credential
     */
    public CredentialBean getCredential() {
        return credential;
    }

    /**
     * @param credential the credential to set
     */
    public void setCredential(CredentialBean credential) {
        this.credential = credential;
    }
}
