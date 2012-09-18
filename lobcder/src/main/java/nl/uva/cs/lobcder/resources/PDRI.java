/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author dvasunin
 */
public abstract class PDRI implements Cloneable{
    protected Long pdriId;
    protected String url;
    protected String file_name;
    protected Long storageSiteId;
   
    private static AtomicLong count = new AtomicLong();
 
    
    public PDRI(String file_name, Long storageSiteId){
        this.storageSiteId = storageSiteId;
        this.file_name = file_name;
 
       /* if(storageSiteId != null) {
            url = ss.getResourceURI();
            if(!url.endsWith("/"))
                url +=  "/" + file_name;
            else 
                url += file_name;
        }
        
        */
        this.pdriId =  count.incrementAndGet();
    }
        
    public abstract void delete() throws IOException;
    
    public abstract InputStream getData() throws IOException;
    
    public abstract void putData(InputStream data) throws IOException;


    public Long getPdriId() {
        return pdriId;
    }    
        
}
