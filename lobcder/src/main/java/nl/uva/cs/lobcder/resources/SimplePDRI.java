/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOException;
import nl.uva.cs.lobcder.util.Constants;

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
//        Runnable asyncPut = getAsyncPutData(baseLocation + file_name, data);
//        asyncPut.run();
        setResourceContent(baseLocation + file_name, data);
    }

    @Override
    public Long getStorageSiteId() {
        return ssid;
    }

    @Override
    public String getURI() {
        return file_name;
    }

    private void setResourceContent(String uri, InputStream is) throws FileNotFoundException, IOException {
        File file = new File(uri);
//        OutputStream os = new BufferedOutputStream(new FileOutputStream(file), Constants.BUF_SIZE);
        OutputStream os = new FileOutputStream(file);
        try {
            int read;
            byte[] copyBuffer = new byte[10 * 1024 * 1024];

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

    private Runnable getAsyncPutData(final String uri, final InputStream is) {

        return new Runnable() {

            @Override
            public void run() {
                OutputStream dest = null;
                try {
                    dest = new FileOutputStream(new File(uri));
                    if ((dest instanceof FileOutputStream) && (is instanceof FileInputStream)) {
                        try {
                            FileChannel target = ((FileOutputStream) dest).getChannel();
                            FileChannel source = ((FileInputStream) is).getChannel();
                            source.transferTo(0, source.size(), target);
                            source.close();
                            target.close();

                            return;
                        } catch (Exception e) { /*
                             * failover to byte stream version
                             */

                        }
                    }
                    final ReadableByteChannel inputChannel = Channels.newChannel(is);
                    final WritableByteChannel outputChannel = Channels.newChannel(dest);
                    fastCopy(inputChannel, outputChannel);
                } catch (IOException ex) {
                    Logger.getLogger(SimplePDRI.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        dest.close();
                    } catch (IOException ex) {
                        Logger.getLogger(SimplePDRI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            private void fastCopy(ReadableByteChannel inputChannel, WritableByteChannel outputChannel) throws IOException {
                final ByteBuffer buffer = ByteBuffer.allocateDirect(Constants.BUF_SIZE);
                while (inputChannel.read(buffer) != -1) {
                    buffer.flip();
                    outputChannel.write(buffer);
                    buffer.compact();
                }
                buffer.flip();

                while (buffer.hasRemaining()) {
                    outputChannel.write(buffer);
                }
            }
        };


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
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] dataBytes = new byte[1024];

            int nread = 0;
            while ((nread = new FileInputStream(new File(baseLocation + file_name)).read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            byte[] mdbytes = md.digest();

            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return Long.parseLong(sb.toString(), 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String getHost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
}
