package com.ettrema.http.report;

import com.bradmcevoy.http.ExistingEntityHandler;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceHandlerHelper;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.WritingException;
import com.bradmcevoy.http.ReportableResource;
import com.bradmcevoy.http.entity.ByteArrayEntity;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alex
 */
public class ReportHandler implements ExistingEntityHandler {
    private Logger log = LoggerFactory.getLogger(ReportHandler.class);

    private final WebDavResponseHandler responseHandler;
    private final ResourceHandlerHelper resourceHandlerHelper;
    private final Map<String,Report> reports;

    public ReportHandler( WebDavResponseHandler responseHandler, ResourceHandlerHelper resourceHandlerHelper, Map<String,Report> reports ) {
        this.responseHandler = responseHandler;
        this.resourceHandlerHelper = resourceHandlerHelper;
        this.reports = reports;
    }


	@Override
    public String[] getMethods() {
        return new String[]{Method.REPORT.code};
    }
	@Override
    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        resourceHandlerHelper.process( httpManager, request, response, this );
    }

	@Override
    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.processResource( manager, request, response, r, this );
    }

	@Override
    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException {
        try {
            org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
            org.jdom.Document doc = builder.build(request.getInputStream());
            String reportName = doc.getRootElement().getName();
            Report r = reports.get( reportName);
            if( r == null ) {
                log.error( "report not known: " + reportName );
                throw new BadRequestException( resource );
            } else {
                String xml = r.process( request.getHostHeader(), request.getAbsolutePath(), resource, doc );
                response.setStatus( Response.Status.SC_MULTI_STATUS );
                response.setEntity(new ByteArrayEntity(xml.getBytes()));
            }
        } catch( JDOMException ex ) {
            java.util.logging.Logger.getLogger( ReportHandler.class.getName() ).log( Level.SEVERE, null, ex );
        } catch( ReadingException ex ) {
            throw new RuntimeException( ex );
        } catch( WritingException ex ) {
            throw new RuntimeException( ex );
        } catch(IOException ex) {
            throw new RuntimeException( ex );
        }
    }

	@Override
    public boolean isCompatible( Resource res ) {
        return ( res instanceof ReportableResource );
    }
}
