/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author dvasunin
 */
public class PDRIGroup {

    private Long groupId;
    private String pdriIds="";
    protected Integer refCount=0;
    
    static public class Accessor {
        public static Integer getRefCount(PDRIGroup pdriGroup) {
            synchronized(pdriGroup){
                return pdriGroup.refCount;
            }
        }
        public static void setRefCount(PDRIGroup pdriGroup, Integer value){
            synchronized(pdriGroup){
                pdriGroup.refCount = value;
            }
        }
    }
    
    private static AtomicLong count = new AtomicLong();
    private Set<Long> decodedPdriIds = null;
    
    public PDRIGroup(){
        groupId = count.incrementAndGet();
    }
    
    
    public Long getGroupId(){
        return groupId;
    }

    public Set<Long> getPdriIds() {
        if (decodedPdriIds == null) {
            decodedPdriIds = new HashSet<Long>();
            for (String pdriStr : pdriIds.split(",")) {
                decodedPdriIds.add(Long.decode(pdriStr));
            }
        }
        return decodedPdriIds;
    }

    public void setPdriIds(Set<Long> pdriIds) {
        StringBuilder sb = new StringBuilder();
        boolean firstLoop = true;
        for (Long prdiId : pdriIds) {
            if (firstLoop) {
                sb.append(prdiId);
                firstLoop = false;
            } else {
                sb.append(",").append(prdiId);
            }
        }
        this.pdriIds = sb.toString();
        decodedPdriIds = pdriIds;
    }
    
    public void addPdriRef(Long pdriId){
        if(pdriIds.isEmpty()){
            pdriIds = pdriId.toString();            
        } else {
            pdriIds += "," + pdriId;
        }
        decodedPdriIds = null;        
    }
}
