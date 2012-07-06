/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.io.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 *
 * Source from com.bradmcevoy.http.http11.PartialGetHelper
 *
 * @author brad
 */
public class LobIOUtils {

    public static void writeRanges(InputStream in, List<Range> ranges, OutputStream responseOut) throws IOException {
        try {
            InputStream bufIn = in; //new BufferedInputStream(in);
            long pos = 0;
            for (Range r : ranges) {
                long skip = r.getStart() - pos;
                bufIn.skip(skip);
                long length = r.getFinish() - r.getStart();
                sendBytes(bufIn, responseOut, length);
                pos = r.getFinish();
            }
        } finally {
            StreamUtils.close(in);
        }
    }

    public static void sendBytes(InputStream in, OutputStream out, long length) throws IOException {
        long numRead = 0;
        byte[] b = new byte[1024];
        while (numRead < length) {
            long remainingBytes = length - numRead;
            int maxLength = remainingBytes > 1024 ? 1024 : (int) remainingBytes;
            int s = in.read(b, 0, maxLength);
            if (s < 0) {
                break;
            }
            numRead += s;
            out.write(b, 0, s);
        }

    }

    public static void writeRange(InputStream in, Range range, OutputStream out) throws IOException {
        try {
            InputStream bufIn = in; //new BufferedInputStream(in);
            long pos = 0;
//            for (Range r : ranges) {
            long skip = range.getStart() - pos;
            bufIn.skip(skip);
            long length = range.getFinish() - range.getStart();
            sendBytes(bufIn, out, length);
            pos = range.getFinish();
//            }
        } finally {
            StreamUtils.close(in);
        }
    }

    public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024 * 1024);
        while (src.read(buffer) != -1) {
            // prepare the buffer to be drained
            buffer.flip();
            // write to the channel, may block
            dest.write(buffer);
            // If partial transfer, shift remainder down
            // If buffer is empty, same as doing clear()
            buffer.compact();
        }
        // EOF will leave buffer in fill state
        buffer.flip();
        // make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            int length = in.available(); // danger!
            if (length != 0) {
                byte[] bytes = new byte[length];
                in.read(bytes);
                out.write(bytes);
            } else {
                IOUtils.copy(in, out);
            }

        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
