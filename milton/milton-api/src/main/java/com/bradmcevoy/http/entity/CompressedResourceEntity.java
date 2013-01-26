package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.CompressedResource;
import com.bradmcevoy.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class CompressedResourceEntity implements Response.Entity {

    private static final Logger log = LoggerFactory.getLogger(CompressedResourceEntity.class);

    private CompressedResource resource;
    private Map<String, String> params;
    private String contentType;
    private String contentEncoding;

    public CompressedResourceEntity(CompressedResource resource, Map<String, String> params, String contentType, String contentEncoding) {
        this.resource = resource;
        this.params = params;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
    }

    public CompressedResource getResource() {
        return resource;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    @Override
    public void write(Response response, OutputStream outputStream) throws Exception {
        try {
            resource.sendCompressedContent(contentEncoding, outputStream, null, params, contentType);
        } catch (IOException ex) {
            log.warn("IOException sending compressed content", ex);
        }
    }

}
