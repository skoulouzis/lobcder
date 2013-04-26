/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.Constants;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dvasunin
 */
public class CachePDRI implements PDRI {

    private final static CatalogueHelper ch = new CatalogueHelper();
    private final static String baseLocation;

    static {
        baseLocation ="/tmp/LOBCDER-REPLICA-vTEST/";
    }
    final private String file_name;
    final private Long ssid;
    private final File file;

    public CachePDRI(String file_name) {
        this.ssid = Long.valueOf(Constants.CACHE_STORAGE_SITE_ID);
        this.file_name = file_name;
        file = new File(baseLocation + file_name);
    }

    @Override
    public void delete() throws IOException {

        file.delete();
    }

    @Override
    public InputStream getData() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public void putData(InputStream data) throws IOException {
//        Runnable asyncPut = getAsyncPutData(baseLocation + file_name, data);
//        asyncPut.run();
        setResourceContent(data);
    }

    @Override
    public Long getStorageSiteId() {
        return ssid;
    }

    @Override
    public String getFileName() {
        return file_name;
    }

    private void setResourceContent(InputStream is) throws FileNotFoundException, IOException {
//        OutputStream os = new BufferedOutputStream(new FileOutputStream(file), Constants.BUF_SIZE);
        OutputStream os = new FileOutputStream(file);
        try {
            int read;
            byte[] copyBuffer = new byte[2 * 1024 * 1024];

            while ((read = is.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                os.write(copyBuffer, 0, read);
            }
        } finally {
            try {
                is.close();
            } finally {
                os.close();
            }
        }
    }

    @Override
    public long getLength() {
        return new File(baseLocation + file_name).length();
    }

    @Override
    public void reconnect() throws IOException {
        //there is nowhere to connect
    }

    @Override
    public Long getChecksum() throws IOException {
        return null;
//        try {
//            MessageDigest md = MessageDigest.getInstance("MD5");
//
//            byte[] dataBytes = new byte[1024];
//
//            int nread = 0;
//            while ((nread = new FileInputStream(new File(baseLocation + file_name)).read(dataBytes)) != -1) {
//                md.update(dataBytes, 0, nread);
//            }
//            byte[] mdbytes = md.digest();
//
//            //convert the byte to hex format method 1
//            StringBuffer sb = new StringBuffer();
//            for (int i = 0; i < mdbytes.length; i++) {
//                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
//            }
//            return Long.parseLong(sb.toString(), 16);
//        } catch (NoSuchAlgorithmException ex) {
//            throw new IOException(ex);
//        }
    }

    @Override
    public String getHost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    @Override
    public void replicate(PDRI source,boolean encrypt) throws IOException {
        putData(source.getData());
    }

    @Override
    public String getURI() throws IOException {
        return file.toURI().toString();
    }
}
