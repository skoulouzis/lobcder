/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public String getURL() {
        return file_name;
    }

    private void setResourceContent(String uri, InputStream is) throws FileNotFoundException, IOException {
        File file = new File(uri);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file), Constants.BUF_SIZE);
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
}
