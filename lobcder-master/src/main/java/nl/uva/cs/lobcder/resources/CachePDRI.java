/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import io.milton.http.Range;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.Constants;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VChecksum;
import nl.uva.vlet.vfs.VFile;

/**
 *
 * @author dvasunin
 */
public class CachePDRI implements PDRI {

    private final static CatalogueHelper ch = new CatalogueHelper();
    private final static String baseLocation;

    static {
        baseLocation = "/tmp/LOBCDER-REPLICA-vTEST/";
    }
    
    final private String file_name;
    final private Long ssid;
    private final File file;
    private BigInteger key;
    private boolean encrypt;

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
    public void putData(InputStream data) throws IOException, FileNotFoundException {
//        Runnable asyncPut = getAsyncPutData(baseLocation + file_name, data);
//        asyncPut.run();
        if (!getEncrypted()) {
            try {
                setResourceContent(data);
            } catch (VlException ex) {
                throw new IOException(ex);
            }
        } else {
            try {
                OutputStream out = new FileOutputStream(file);
                DesEncrypter encrypter = new DesEncrypter(getKeyInt());
                encrypter.decrypt(getData(), out);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public Long getStorageSiteId() {
        return ssid;
    }

    @Override
    public String getFileName() {
        return file_name;
    }

    private void setResourceContent(InputStream is) throws FileNotFoundException, IOException, VlException {
        OutputStream os = new FileOutputStream(file);
        try {
            int read;
            byte[] copyBuffer = new byte[Constants.BUF_SIZE];
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
//        try {
//            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), is, os);
//            cBuff.startTransfer(new Long(-1));
//        } finally {
//            if (os != null) {
//                try {
//                    os.flush();
//                    os.close();
//                } catch (java.io.IOException ex) {
//                }
//            }
//            if (is != null) {
//                is.close();
//            }
//        }

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
//            byte[] dataBytes = new byte[BUF_SIZE];
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
    public void replicate(PDRI source) throws IOException {
        putData(source.getData());
    }

    @Override
    public String getURI() throws IOException {
        return file.toURI().toString();
    }

//    @Override
//    public void setKeyInt(BigInteger keyInt) {
//       this.key = keyInt;
//    }
//
    @Override
    public BigInteger getKeyInt() {
        return this.key;
    }

    @Override
    public boolean getEncrypted() {
        return this.encrypt;
    }

    @Override
    public void copyRange(Range range, OutputStream out) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setLength(long length) {
    }

    @Override
    public String getStringChecksum() throws IOException {
        return null;
    }

    @Override
    public boolean exists(String fileName) throws IOException {
        return new File(new File(this.file_name).getParentFile().getAbsoluteFile()+"/"+fileName).exists();
    }
}
