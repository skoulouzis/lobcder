package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.io.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class PartialEntity implements Response.Entity {

    private static final Logger log = LoggerFactory.getLogger(PartialEntity.class);

    private List<Range> ranges;
    private File temp;

    public PartialEntity(List<Range> ranges, File temp) {
        this.ranges = ranges;
        this.temp = temp;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public File getTemp() {
        return temp;
    }

    @Override
    public void write(Response response, OutputStream outputStream) throws Exception {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(temp);
            writeRanges(fin, ranges, outputStream);
        } finally {
            StreamUtils.close(fin);
        }
    }

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
        log.trace("sendBytes: " + length);
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

    public static void writeRange(InputStream in, Range r, OutputStream responseOut) throws IOException {
        long skip = r.getStart();
        in.skip(skip);
        long length = r.getFinish() - r.getStart();
        sendBytes(in, responseOut, length);
    }

}
