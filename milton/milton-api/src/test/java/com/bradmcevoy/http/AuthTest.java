
package com.bradmcevoy.http;

import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class AuthTest extends TestCase {
    
    public void testBasic() {
        Auth auth = new Auth( "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        //Auth auth = new Auth( "Basic username=Aladdin,password=\"open sesame\"");
        assertEquals( "Aladdin", auth.getUser());
        assertEquals( "open sesame", auth.getPassword());
    }

    public void testDigest() {
        Auth auth = new Auth( "Digest username=\"Mufasa\",realm=\"testrealm@host.com\",nonce=\"ZTMyNmFmNDEtYWEwYy00MTc5LTk2OWEtZjMyOGRiOWI1NTg0\",uri=\"/webdav/secure\",cnonce=\"09683d5720f7e5e7dec2daeee585fe15\",nc=00000001,response=\"e6e7559f052bf75cdd8a979943197f40\",qop=\"auth\"");
        assertEquals( "Mufasa", auth.getUser());
        assertEquals( "testrealm@host.com", auth.getRealm());
        assertEquals( "ZTMyNmFmNDEtYWEwYy00MTc5LTk2OWEtZjMyOGRiOWI1NTg0", auth.getNonce());
        assertEquals( "/webdav/secure", auth.getUri());
        assertEquals( "09683d5720f7e5e7dec2daeee585fe15", auth.getCnonce());
        assertEquals( "e6e7559f052bf75cdd8a979943197f40", auth.getResponseDigest());
        assertEquals( "auth", auth.getQop());


    }
}
