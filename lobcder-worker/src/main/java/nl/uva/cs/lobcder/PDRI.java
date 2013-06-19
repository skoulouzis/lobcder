/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import io.milton.http.Range;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
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

    public void replicate(PDRI source) throws IOException;

    public String getURI() throws IOException;

//    public void setKeyInt(BigInteger keyInt);
//
    public BigInteger getKeyInt();

    public boolean getEncrypted();

    public void copyRange(Range range, OutputStream out,boolean decrypt) throws IOException;
}
