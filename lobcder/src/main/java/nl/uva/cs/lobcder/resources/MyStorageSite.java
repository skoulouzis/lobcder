/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

/**
 *
 * @author S. Koulouzis
 */
public class MyStorageSite implements Cloneable{

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Long getCurrentNum() {
        return currentNum;
    }

    public void setCurrentNum(Long currentNum) {
        this.currentNum = currentNum;
    }

    public Long getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(Long currentSize) {
        this.currentSize = currentSize;
    }

    public Long getQuotaNum() {
        return quotaNum;
    }

    public void setQuotaNum(Long quotaNum) {
        this.quotaNum = quotaNum;
    }

    public Long getQuotaSize() {
        return quotaSize;
    }

    public void setQuotaSize(Long quotaSize) {
        this.quotaSize = quotaSize;
    }

    public String getResourceURI() {
        return resourceURI;
    }

    public void setResourceURI(String resourceURI) {
        this.resourceURI = resourceURI;
    }
        
    @Override
    public Object clone(){
        MyStorageSite clone = new MyStorageSite();
        clone.setCredential(credential);
        clone.setCurrentNum(new Long(currentNum));
        clone.setCurrentSize(new Long(currentSize));
        clone.setQuotaNum(new Long (quotaNum));
        clone.setQuotaSize(new Long(quotaSize));
        clone.setResourceURI(resourceURI);
        return clone;
    }
    
    private Long quotaSize;
    private Long quotaNum;
    private Long currentSize;
    private Long currentNum;
    private String resourceURI;
    private Credential credential;
}
