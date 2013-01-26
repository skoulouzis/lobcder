package com.bradmcevoy.http;

import java.io.OutputStream;

/**
 * Represents an item which has been uploaded in a form POST
 * 
 * @author brad
 */
public interface FileItem {

    String getContentType();

    /**
     * The name of the field which declared the file control
     * @return
     */
    String getFieldName();

    /**
     * To read the uploaded file
     *
     * @return
     */
    java.io.InputStream getInputStream();

    /**
     * The name of the uploaded file
     * @return
     */
    java.lang.String getName();

    /**
     * The size of the uploaded file
     * @return
     */
    long getSize();

    /**
     * To allow writing to the uploaded file. Not always supported
     * @return
     */
    OutputStream getOutputStream();
}
