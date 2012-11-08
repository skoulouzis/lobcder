/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.*;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.util.LobIOUtils;

/**
 *
 * @author dvasunin
 */
public class SimplePDRI implements PDRI {

    final private String baseLocation = System.getProperty("user.home") + "/tmp/lobcder/";
    final private String file_name;
    final private Long ssid;

    public SimplePDRI(Long ssid, String file_name) {
        this.ssid = ssid;
        this.file_name = file_name;
    }

    @Override
    public void delete() throws IOException {
        File fd = new File(baseLocation + file_name);
        fd.delete();
    }

    @Override
    public InputStream getData() throws IOException {
        File f = new File(baseLocation + file_name);
        return new BufferedInputStream(new FileInputStream(f));
    }

    @Override
    public void putData(InputStream data) throws IOException {
        Runnable asyncPut = getAsyncPutData(baseLocation + file_name, data);
        asyncPut.run();
    }

    @Override
    public Long getStorageSiteId() {
        return ssid;
    }

    @Override
    public String getURL() {
        return file_name;
    }

    private Runnable getAsyncPutData(final String string, final InputStream data) {
        return new Runnable() {

            @Override
            public void run() {
                BufferedOutputStream bos = null;
                try {
                    File f = new File(string);
                    bos = new BufferedOutputStream(new FileOutputStream(f));
                    LobIOUtils u = new LobIOUtils();
                    u.fastCopy(data, bos);
                } catch (IOException ex) {
                    Logger.getLogger(SimplePDRI.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        bos.close();
                    } catch (IOException ex) {
                        Logger.getLogger(SimplePDRI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
    }
}
