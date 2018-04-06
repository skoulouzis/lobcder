/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;


/**
 *
 * @author S. Koulouzis
 */
public class StorageSite implements Cloneable {

    @Override
    public Object clone() {
        StorageSite clone = new StorageSite();
        clone.setStorageSiteId(getStorageSiteId());
        clone.setCredential(getCredential());
        clone.setCurrentNum((getCurrentNum()));
        clone.setCurrentSize((getCurrentSize()));
        clone.setQuotaNum((getQuotaNum()));
        clone.setQuotaSize((getQuotaSize()));
        clone.setResourceURI(getResourceURI());
        clone.setEncrypt(isEncrypt());
        clone.setCache(isCache());
        clone.setPrivateSite(isPrivateSite());
        clone.setReadOnly(isReadOnly());
        clone.setRemoving(isRemoving());
        return clone;
    }
    
    private Long storageSiteId;
    private Long quotaSize;
    private Long quotaNum;
    private Long currentSize;
    private Long currentNum;
    private String resourceURI;
    private Credential credential;
    private boolean encrypt;
    private boolean cache;
    private boolean privateSite;
    private boolean readOnly;
    private boolean removing;

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
    public Credential getCredential() {
        return credential;
    }

    /**
     * @param credential the credential to set
     */
    public void setCredential(Credential credential) {
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
     * @return the cache
     */
    public boolean isCache() {
        return cache;
    }

    /**
     * @param cache the cache to set
     */
    public void setCache(boolean cache) {
        this.cache = cache;
    }

    /**
     * @return the privateSite
     */
    public boolean isPrivateSite() {
        return privateSite;
    }

    /**
     * @param privateSite the privateSite to set
     */
    public void setPrivateSite(boolean privateSite) {
        this.privateSite = privateSite;
    }

    /**
     * @return the readOnly
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @param readOnly the readOnly to set
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * @return the removing
     */
    public boolean isRemoving() {
        return removing;
    }

    /**
     * @param removing the removing to set
     */
    public void setRemoving(boolean removing) {
        this.removing = removing;
    }
}
