/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.*;
import nl.uva.cs.lobcder.util.LobIOUtils;

/**
 *
 * @author dvasunin
 */
public class SimplePDRI implements PDRI{

    final private String baseLocation = "/tmp/lobcder/";
    final private String file_name;
    final private Long ssid;
    
    public SimplePDRI(Long ssid, String file_name) {
        this.ssid = ssid;
        this.file_name = file_name;
    }
    
    @Override
    public void delete() throws IOException{
        File fd = new File(baseLocation + file_name);
        fd.delete();
    }

    @Override
    public InputStream getData() throws IOException{
        File f = new File(baseLocation + file_name);
        return new BufferedInputStream(new FileInputStream(f));
    }

    @Override
    public void putData(InputStream data) throws IOException{
        File f = new File(baseLocation + file_name);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
        LobIOUtils.copy(data, bos);           
    }

    @Override
    public Long getStorageSiteId() {
        return ssid;
    }

    @Override
    public String getURL() {
        return file_name;
    }
    
}
