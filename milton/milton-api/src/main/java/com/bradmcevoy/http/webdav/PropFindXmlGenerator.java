package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.values.ValueWriters;
import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class PropFindXmlGenerator {

    private static final Logger log = LoggerFactory.getLogger( PropFindXmlGenerator.class );
    private final PropFindXmlGeneratorHelper helper;    

    public PropFindXmlGenerator( ValueWriters valueWriters ) {
        helper = new PropFindXmlGeneratorHelper(valueWriters);
    }

    PropFindXmlGenerator( PropFindXmlGeneratorHelper helper ) {
        this.helper = helper;
    }

    public String generate( List<PropFindResponse> propFindResponses ) {
        ByteArrayOutputStream responseOutput = new ByteArrayOutputStream();
        Map<String, String> mapOfNamespaces = helper.findNameSpaces( propFindResponses );
        ByteArrayOutputStream generatedXml = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter( generatedXml );
        writer.writeXMLHeader();
        writer.open(WebDavProtocol.NS_DAV.getPrefix() ,"multistatus" + helper.generateNamespaceDeclarations( mapOfNamespaces ) );
        writer.newLine();
        helper.appendResponses( writer, propFindResponses, mapOfNamespaces );
        writer.close(WebDavProtocol.NS_DAV.getPrefix(),"multistatus" );
        writer.flush();
		if(log.isTraceEnabled()) {
			log.trace("---- PROPFIND response START: " + HttpManager.request().getAbsolutePath() + " -----");
			log.trace( generatedXml.toString() );
			log.trace("---- PROPFIND response END -----");
		}
        helper.write( generatedXml, responseOutput );
        try {
            return responseOutput.toString( "UTF-8" );
        } catch( UnsupportedEncodingException ex ) {
            throw new RuntimeException( ex );
        }
    }
}
