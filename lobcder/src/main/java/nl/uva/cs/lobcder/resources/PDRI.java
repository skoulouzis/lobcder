/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

/**
 *
 * @author dvasunin
 */
public interface PDRI {

    public Long getStorageSiteId();

    public String getFileName();

    public String getHost() throws UnknownHostException;

    public void delete() throws IOException;

    public InputStream getData() throws IOException;

    public void putData(InputStream data) throws IOException;

    public long getLength() throws IOException;

    public void reconnect() throws IOException;

    public Long getChecksum() throws IOException;

    public void replicate(PDRI source,boolean encrypt) throws IOException;

    public String getURI() throws IOException;
}
