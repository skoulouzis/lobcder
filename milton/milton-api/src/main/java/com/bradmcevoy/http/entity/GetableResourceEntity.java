package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class GetableResourceEntity implements Response.Entity {

    private static final Logger log = LoggerFactory.getLogger(GetableResourceEntity.class);

    private GetableResource resource;
    private Range range;
    private Map<String, String> params;
    private String contentType;

    public GetableResourceEntity(GetableResource resource, Map<String, String> params, String contentType) {
        this(resource, null, params, contentType);
    }

    public GetableResourceEntity(GetableResource resource, Range range, Map<String, String> params, String contentType) {
        this.resource = resource;
        this.range = range;
        this.params = params;
        this.contentType = contentType;
    }

    public GetableResource getResource() {
        return resource;
    }

    public Range getRange() {
        return range;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void write(Response response, OutputStream outputStream) throws Exception {
        long l = System.currentTimeMillis();
        log.trace("sendContent");
        try {
            resource.sendContent(outputStream, range, params, contentType);
            // TODO: The original code didn't flush for partial responses, not sure why...
            outputStream.flush();
            if (log.isTraceEnabled()) {
                l = System.currentTimeMillis() - l;
                log.trace("sendContent finished in " + l + "ms");
            }
        } catch (IOException ex) {
            log.warn("IOException writing to output, probably client terminated connection", ex);
        }
    }

}
