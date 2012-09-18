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
public class SimplePDRI extends PDRI{

    final String baseLocation = "/tmp/";
    
    public SimplePDRI(String file_name, Long storageSiteId) {
        super(file_name, storageSiteId);
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
    
}
