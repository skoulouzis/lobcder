package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.Response;
import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.StreamUtils;
import com.bradmcevoy.io.WritingException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamEntity implements Response.Entity {

    private static final Logger log = LoggerFactory.getLogger(InputStreamEntity.class);

    private InputStream inputStream;

    public InputStreamEntity(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void write(Response response, OutputStream outputStream) throws Exception {
        try {
            StreamUtils.readTo(inputStream, outputStream);
        } catch (ReadingException ex) {
            throw new RuntimeException(ex);
        } catch (WritingException ex) {
            log.warn("exception writing, client probably closed connection", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        log.trace("finished sending content");
    }
}
