package com.bradmcevoy.http.webdav;

import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.StreamUtils;
import com.bradmcevoy.io.WritingException;
import java.io.ByteArrayInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author brad
 */
public class DefaultPropPatchParser implements PropPatchRequestParser {

    private final static Logger log = LoggerFactory.getLogger( DefaultPropPatchParser.class );

    public ParseResult getRequestedFields( InputStream in ) {
        log.debug( "getRequestedFields" );
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            StreamUtils.readTo( in, bout, false, true );
            byte[] arr = bout.toByteArray();
            return parseContent( arr );
        } catch( SAXException ex ) {
            throw new RuntimeException( ex );
        } catch( ReadingException ex ) {
            throw new RuntimeException( ex );
        } catch( WritingException ex ) {
            throw new RuntimeException( ex );
        } catch( IOException ex ) {
            throw new RuntimeException( ex );
        }
    }

    private ParseResult parseContent( byte[] arr ) throws IOException, SAXException {
        if( arr.length > 0 ) {
            log.debug( "processing content" );
            ByteArrayInputStream bin = new ByteArrayInputStream( arr );
            XMLReader reader = XMLReaderFactory.createXMLReader();
            PropPatchSaxHandler handler = new PropPatchSaxHandler();
            reader.setContentHandler( handler );
            reader.parse( new InputSource( bin ) );
            log.debug( "toset: " + handler.getAttributesToSet().size());
            return new ParseResult( handler.getAttributesToSet(), handler.getAttributesToRemove().keySet() );
        } else {
            log.debug( "empty content" );
            return new ParseResult( new HashMap<QName, String>(), new HashSet<QName>() );
        }

    }
}
