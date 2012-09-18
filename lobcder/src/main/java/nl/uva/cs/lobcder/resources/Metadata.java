/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author S. Koulouzis
 */
/**
 *
 * JDO 2.0 introduces a new way of handling this situation, by detaching an
 * object from the persistence graph, allowing it to be worked on in the users
 * application. It can then be attached to the persistence graph later. The
 * first thing to do to use a class with this facility is to tag it as
 * "detachable". This is done by adding the attribute
 */
public class Metadata implements Cloneable {

    private Long createDate = System.currentTimeMillis();
    private Long modifiedDate = System.currentTimeMillis();
    private Long length = (long)0;
    private String contentTypesStr = "";  
    private String permissionArrayStr = "";
    
    private List<String> decodedContentTypes = null;
    private List<Integer> decodedPermissionArray = null;

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

    public List<String> getContentTypes() {
        if(decodedContentTypes == null){
            decodedContentTypes = Arrays.asList(contentTypesStr.split(","));
        }
        return decodedContentTypes; 
    }

    public void addContentType(String contentType) {
        String ct[] = contentTypesStr.split(",");
        if (!Arrays.asList(ct).contains(contentType)) {
            contentTypesStr += contentTypesStr.isEmpty() ? contentType : "," + contentType;
        }
        decodedContentTypes = null;
    }

    public List<Integer> getPermissionArray() {
        if(decodedPermissionArray == null){
            String permStr[] = permissionArrayStr.split(",");
            decodedPermissionArray = new ArrayList<Integer>(permStr.length);
            for(String myInt : permStr){
                decodedPermissionArray.add(Integer.decode(myInt));
            }
        }
        return decodedPermissionArray;
    }

    public void setPermissionArray(List<Integer> permissionArray) {
        StringBuilder sb = new StringBuilder();
        boolean firstLoop = true;
        for(Integer p : permissionArray){
            if(firstLoop){
                sb.append(p);
                firstLoop = false;
            } else {
                sb.append(",").append(p);
            }
        }
        this.permissionArrayStr = sb.toString();
        this.decodedPermissionArray = permissionArray;
    }
    
    @Override
    public Object clone(){
        Metadata clone = new Metadata();
        clone.createDate = new Long(createDate);
        clone.modifiedDate = new Long(modifiedDate);
        clone.length = new Long(length);        
        clone.contentTypesStr = contentTypesStr;       
        clone.permissionArrayStr = permissionArrayStr;
        return clone;
    }
}
