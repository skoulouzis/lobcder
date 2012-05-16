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
import java.util.List;

/**
 *
 * Source from com.bradmcevoy.http.http11.PartialGetHelper
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
}
