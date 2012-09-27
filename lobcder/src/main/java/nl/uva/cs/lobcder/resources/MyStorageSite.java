/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.util.ArrayList;
import java.util.List;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.MyPrincipal.Exception;
import nl.uva.cs.lobcder.auth.test.MyAuth;

/**
 *
 * @author S. Koulouzis
 */
public class MyStorageSite implements Cloneable {

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
    public Object clone() {
        MyStorageSite clone = new MyStorageSite();
        clone.setCredential(credential);
        clone.setCurrentNum(new Long(currentNum));
        clone.setCurrentSize(new Long(currentSize));
        clone.setQuotaNum(new Long(quotaNum));
        clone.setQuotaSize(new Long(quotaSize));
        clone.setResourceURI(resourceURI);
        return clone;
    }

    public List<MyPrincipal> getAllowedUsers() throws Exception {
        ArrayList<MyPrincipal> allowedUsers = new ArrayList<MyPrincipal>();
//        for (String token : allowedUsersTokens) {
//            allowedUsers.add(new MyPrincipal(token, MyAuth.getInstance().checkToken(token)));
//        }
        return allowedUsers;
    }

    public void setAllowedUsers(List<MyPrincipal> users) {
        if (allowedUsersTokens == null) {
            allowedUsersTokens = new ArrayList<String>();
        }
        for (MyPrincipal p : users) {
            allowedUsersTokens.add(p.getToken());
        }
    }

    public void addAllowedUser(MyPrincipal user) {
        if (allowedUsersTokens == null) {
            allowedUsersTokens = new ArrayList<String>();
        }
        allowedUsersTokens.add(user.getToken());
    }
    
    
    
    private Long quotaSize;
    private Long quotaNum;
    private Long currentSize;
    private Long currentNum;
    private String resourceURI;
    private Credential credential;
    private List<String> allowedUsersTokens;
}
