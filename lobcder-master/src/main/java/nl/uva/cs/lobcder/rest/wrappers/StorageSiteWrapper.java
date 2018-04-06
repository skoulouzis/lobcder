/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest.wrappers;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
public class StorageSiteWrapper {

    private Long storageSiteId;
    private Long quotaSize;
    private Long quotaNum;
    private Long currentSize;
    private Long currentNum;
    private String resourceURI;
    private CredentialWrapped credential;
    private boolean encrypt;
    private boolean isCache;
    private boolean saveFilesOnDelete;

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
     * @return the quotaSize
     */
    public Long getQuotaSize() {
        return quotaSize;
    }

    /**
     * @param quotaSize the quotaSize to set
     */
    public void setQuotaSize(Long quotaSize) {
        this.quotaSize = quotaSize;
    }

    /**
     * @return the quotaNum
     */
    public Long getQuotaNum() {
        return quotaNum;
    }

    /**
     * @param quotaNum the quotaNum to set
     */
    public void setQuotaNum(Long quotaNum) {
        this.quotaNum = quotaNum;
    }

    /**
     * @return the currentSize
     */
    public Long getCurrentSize() {
        return currentSize;
    }

    /**
     * @param currentSize the currentSize to set
     */
    public void setCurrentSize(Long currentSize) {
        this.currentSize = currentSize;
    }

    /**
     * @return the currentNum
     */
    public Long getCurrentNum() {
        return currentNum;
    }

    /**
     * @param currentNum the currentNum to set
     */
    public void setCurrentNum(Long currentNum) {
        this.currentNum = currentNum;
    }

    /**
     * @return the resourceURI
     */
    public String getResourceURI() {
        return resourceURI;
    }

    /**
     * @param resourceURI the resourceURI to set
     */
    public void setResourceURI(String resourceURI) {
        this.resourceURI = resourceURI;
    }

    /**
     * @return the credential
     */
    public CredentialWrapped getCredential() {
        return credential;
    }

    /**
     * @param credential the credential to set
     */
    public void setCredential(CredentialWrapped credential) {
        this.credential = credential;
    }

    /**
     * @return the encrypt
     */
    public boolean isEncrypt() {
        return encrypt;
    }

    /**
     * @param encrypt the encrypt to set
     */
    public void setEncrypt(boolean encrypt) {
        this.encrypt = encrypt;
    }

    /**
     * @return the isCache
     */
    public boolean isIsCache() {
        return isCache;
    }

    /**
     * @param isCache the isCache to set
     */
    public void setIsCache(boolean isCache) {
        this.isCache = isCache;
    }

    /**
     * @return the saveFilesOnDelete
     */
    public boolean isSaveFilesOnDelete() {
        return saveFilesOnDelete;
    }

    /**
     * @param saveFilesOnDelete the saveFilesOnDelete to set
     */
    public void setSaveFilesOnDelete(boolean saveFilesOnDelete) {
        this.saveFilesOnDelete = saveFilesOnDelete;
    }

}
