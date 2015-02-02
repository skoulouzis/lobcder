package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.XmlWriter.Element;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import java.util.Map;

/**
 *
 * @author alex
 */
public class SupportedReportSetWriter  implements ValueWriter {

	@Override
    public boolean supports( String nsUri, String localName, Class c ) {
        return SupportedReportSetList.class.isAssignableFrom( c );
    }

	@Override
    public void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes ) {
        
      
        SupportedReportSetList list = (SupportedReportSetList) val;
        Element reportSet = writer.begin( WebDavProtocol.DAV_PREFIX + ":supported-report-set" ).open();
        if( list != null ) {
            for( String s : list) {
                Element supportedReport = writer.begin( WebDavProtocol.DAV_PREFIX + ":supported-report" ).open();
                Element report = writer.begin( WebDavProtocol.DAV_PREFIX + ":report" ).open();
                writer.writeProperty( WebDavProtocol.DAV_PREFIX + ":" + s );
                report.close();
                supportedReport.close();
            }
        }
        reportSet.close();
    }

	@Override
    public Object parse( String namespaceURI, String localPart, String value ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
