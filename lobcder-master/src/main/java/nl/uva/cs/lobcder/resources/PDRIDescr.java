package nl.uva.cs.lobcder.resources;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * User: dvasunin Date: 01.03.13 Time: 13:57 To change this template use File |
 * Settings | File Templates.
 */


@XmlRootElement
public class PDRIDescr {

    private String name;
    private Long storageSiteId;
    private String resourceUrl;
    private String username;
    private String password;
    private Boolean encrypt;
    private BigInteger key;
    private Long pdriGroupRef;
    private Long id;
    private boolean isCashe;


    public PDRIDescr(String name, Long storageSiteId, String resourceUrl, String username, String password, Boolean encrypt, BigInteger key, Long pdriGroupRef, Long id, Boolean isCashe) {
        this.name = name;
        this.storageSiteId = storageSiteId;
                this.resourceUrl = resourceUrl;
                this.username = username;
                this.password = password;
                this.encrypt = encrypt;
                this.key = key;
                this.pdriGroupRef = pdriGroupRef;
                this.id = id;
                this.isCashe = isCashe;
                
                
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
     * @return the storageSiteId
     */
    public Long getStorageSiteId() {
        return storageSiteId;
    }

    /**
     * @param storageSiteId the storageSiteId to set
     */
    public void setStorageSiteId(Long storageSiteId) {
        this.storageSiteId = storageSiteId;
    }

    /**
     * @return the resourceUrl
     */
    public String getResourceUrl() {
        return resourceUrl;
    }

    /**
     * @param resourceUrl the resourceUrl to set
     */
    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
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
     * @return the key
     */
    public BigInteger getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(BigInteger key) {
        this.key = key;
    }

    /**
     * @return the pdriGroupRef
     */
    public Long getPdriGroupRef() {
        return pdriGroupRef;
    }

    /**
     * @param pdriGroupRef the pdriGroupRef to set
     */
    public void setPdriGroupRef(Long pdriGroupRef) {
        this.pdriGroupRef = pdriGroupRef;
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
     * @return the isCashe
     */
    public boolean isIsCashe() {
        return isCashe;
    }

    /**
     * @param isCashe the isCashe to set
     */
    public void setIsCashe(boolean isCashe) {
        this.isCashe = isCashe;
    }
}
