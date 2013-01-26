package com.ettrema.http.fck;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.io.FileUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alienware1
 */
public class FckQuickUploaderResource extends FckCommon {

    private static final Logger log = LoggerFactory.getLogger(FckQuickUploaderResource.class);
    public final static String UPLOAD_RESPONSE_TEMPLATE_NORMAL = ""
        + "<script type=\"text/javascript\">\n"
        + "window.parent.frames['frmUpload'].OnUploadCompleted([code],'[name]') ;\n"
        + "</script>\n";
    public final static Path URL = Path.path( "/fck_upload" );
    //public final static Path URL = Path.path("/editor/filemanager/upload/ettrema/upload.ettrema");
    private int code;
    private String filename;

    public FckQuickUploaderResource( CollectionResource host ) {
        super( host, URL );
    }

    @Override
    public String getUniqueId() {
        return "fckquickuploader";
    }

    @Override
    public String processForm( Map<String, String> params, Map<String, FileItem> files ) throws BadRequestException, NotAuthorizedException {
        if( files == null || files.isEmpty() ) {
            log.warn( "no files to upload" );
            return null;
        }
        for( FileItem f : files.values() ) {
            processFileUpload( f, params );
        }
        return null;
    }

    private void processFileUpload( FileItem f, Map<String, String> params ) throws BadRequestException, NotAuthorizedException {
        CollectionResource target = null;
        if( wrappedResource == null ) {
            throw new BadRequestException(this, "collection not found" );
        }
        target = (CollectionResource) wrappedResource.child( "uploads" );
        if( target == null ) {
            try {
                if( wrappedResource instanceof MakeCollectionableResource ) {
                    MakeCollectionableResource mk = (MakeCollectionableResource) wrappedResource;
                    target = mk.createCollection( "uploads" );
                } else {
                    throw new BadRequestException( target, "Cant create subfolder" );
                }
            } catch( ConflictException ex ) {
                throw new RuntimeException( ex );
            } catch( NotAuthorizedException ex ) {
                throw new RuntimeException( ex );
            } catch( BadRequestException ex ) {
                throw new RuntimeException( ex );
            }
        }

        String name = FileUtils.sanitiseName(f.getName() );
        log.debug( "processFileUpload: " + name );
        boolean isFirst = true;
        String newName = null;
        while( target.child( name ) != null ) {
            name = FileUtils.incrementFileName( name, isFirst );
            newName = name;
            isFirst = false;
        }

        long size = f.getSize();
        try {
            if( target instanceof PutableResource ) {
                PutableResource putable = (PutableResource) target;
                Resource newRes = putable.createNew( name, f.getInputStream(), size, null );
                if( newRes != null ) {
                    log.trace( "created: " + newRes.getName() + " of type: " + newRes.getClass() );
                } else {
                    log.trace( "createNew returned null" );
                }
            } else {
                throw new BadRequestException(target, "Does not implement PutableResource");
            }
        } catch( ConflictException ex ) {
            throw new RuntimeException( ex );
        } catch( NotAuthorizedException ex ) {
            throw new RuntimeException( ex );
        } catch( BadRequestException ex ) {
            throw new RuntimeException( ex );
        } catch( IOException ex ) {
            throw new RuntimeException( ex );
        }

        try {
            if( newName != null ) { // we renamed the file
                uploadResponseOk( name );
            } else {
                uploadResponseOk();
            }
        } catch( Throwable ex ) {
            log.error( "Exception saving new file", ex );
            uploadResponseFailed( ex.getMessage() );
        }
    }

    private void uploadResponseOk() {
        uploadResponse( 0, null );

    }

    private void uploadResponseOk( String newName ) {
        uploadResponse( 201, newName );
    }

    private void uploadResponseFailed( String reason ) {
        uploadResponse( 1, reason );
    }

    private void uploadResponse( int code, String filename ) {
        this.code = code;
        this.filename = filename;
    }

    @Override
    public void sendContent( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException {
        String s = UPLOAD_RESPONSE_TEMPLATE_NORMAL;
        s = s.replace( "[code]", code + "" );
        String f = filename == null ? "" : filename;
        s = s.replace( "[name]", f );
        out.write( s.getBytes() );
    }

    @Override
    public String getContentType( String accepts ) {
        return "text/html";
    }
}
