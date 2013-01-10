/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author dvasunin
 */
public interface PDRI {

    public Long getStorageSiteId();
    
    public String getURL();
    
    public void delete() throws IOException;

    public InputStream getData() throws IOException;

    public void putData(InputStream data) throws IOException;

    public long getLength() throws IOException;
    
    public void reconnect() throws IOException;

    public Long getChecksum() throws IOException;
}
