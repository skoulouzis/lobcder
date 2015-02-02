package com.bradmcevoy.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class LockInfo implements Serializable{

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger( LockInfo.class );

    public enum LockScope {

        NONE,
        SHARED,
        EXCLUSIVE
    }

    public enum LockType {

        READ,
        WRITE
    }

    public enum LockDepth {

        ZERO,
        INFINITY
    }

    public static LockInfo parseLockInfo( Request request ) throws IOException, FileNotFoundException, SAXException {
        InputStream in = request.getInputStream();

        XMLReader reader = XMLReaderFactory.createXMLReader();
        LockInfoSaxHandler handler = new LockInfoSaxHandler();
        reader.setContentHandler( handler );
        reader.parse( new InputSource( in ) );
        LockInfo info = handler.getInfo();
        info.depth = LockDepth.INFINITY; // todo
        info.lockedByUser = null;
        if( request.getAuthorization() != null ) {
            info.lockedByUser = request.getAuthorization().getUser();
        }
        if( info.lockedByUser == null ) {
            log.warn( "resource is being locked with a null user. This won't really be locked at all..." );
        }
        log.debug( "parsed lock info: " + info );
        return info;

    }
    public LockScope scope;
    public LockType type;

    /**
     * The name of the user who has locked this resource.
     */
    public String lockedByUser;
    public LockDepth depth;

    /**
     *
     * @param scope
     * @param type
     * @param lockedByUser - the identifier of the user, such as a href
     * @param depth
     */
    public LockInfo( LockScope scope, LockType type, String lockedByUser, LockDepth depth ) {
        this.scope = scope;
        this.type = type;
        this.lockedByUser = lockedByUser;
        this.depth = depth;
    }

    public LockInfo() {
    }

    @Override
    public String toString() {
        return "scope: " + scope.name() + ", type: " + type.name() + ", owner: " + lockedByUser + ", depth:" + depth;
    }
}
