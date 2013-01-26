package com.bradmcevoy.http.values;

import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.XmlWriter.Element;
import com.bradmcevoy.http.webdav.LockWriterHelper;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import java.util.Map;

public class LockTokenValueWriter implements ValueWriter {

    private LockWriterHelper lockWriterHelper = new LockWriterHelper();

    public LockWriterHelper getLockWriterHelper() {
        return lockWriterHelper;
    }

    public void setLockWriterHelper( LockWriterHelper lockWriterHelper ) {
        this.lockWriterHelper = lockWriterHelper;
    }

	@Override
    public boolean supports( String nsUri, String localName, Class c ) {
        return LockToken.class.isAssignableFrom( c );
    }

	@Override
    public void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes ) {
        LockToken token = (LockToken) val;
		String d = WebDavProtocol.DAV_PREFIX;
        Element lockDiscovery = writer.begin( d + ":lockdiscovery" ).open();		
        if( token != null ) {
			Element activeLock = writer.begin( d + ":activelock" ).open();
            LockInfo info = token.info;
            lockWriterHelper.appendType( writer, info.type );
            lockWriterHelper.appendScope( writer, info.scope );
            lockWriterHelper.appendDepth( writer, info.depth );
            lockWriterHelper.appendOwner( writer, info.lockedByUser );
            lockWriterHelper.appendTimeout( writer, token.timeout.getSeconds() );
            lockWriterHelper.appendTokenId( writer, token.tokenId );
            lockWriterHelper.appendRoot( writer, href );
			activeLock.close();
        }		
        lockDiscovery.close();
    }

	@Override
    public Object parse( String namespaceURI, String localPart, String value ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
