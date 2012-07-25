/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.vlet.exception.ResourceNotFoundException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouz
 */
class Worker implements Runnable {

    public static final int CREATE_PHYSICAL_FILE = 0;
    public static final int REGISTER_DATA = 1;
    static final int UPDATE_DATA = 2;
    private final int op;
    private LogicalData logicalData;
    private InputStream inputStream;
    private Exception exception;
    private String contentType;
    private Long length;
    private IDLCatalogue catalogue;

    public Worker(int op) {
        this.op = op;
    }

    @Override
    public void run() {
        try {
            switch (op) {
                case CREATE_PHYSICAL_FILE:

                    createPhysicalFile();
                    break;

                case REGISTER_DATA:
                    registerData();
                    break;
                case UPDATE_DATA:
                    updeateData();
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            this.exception = ex;
        }
    }

    private void createPhysicalFile() throws Exception {
        VFSNode node = null;
        try {
            node = logicalData.getVFSNode();
        } catch (Exception ex) {
            if (!(ex instanceof ResourceNotFoundException)) {
                throw ex;
            }
        }
        if (node == null) {
            node = logicalData.createPhysicalData();
        }
        if (node != null) {
            OutputStream out = ((VFile) node).getOutputStream();
            IOUtils.copy(inputStream, out);
            if (inputStream != null) {
                inputStream.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    void setLogicalData(LogicalData newResource) {
        this.logicalData = newResource;
    }

    void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    Exception getException() {
        return exception;
    }

    private void registerData() throws CatalogueException {
        Metadata meta = new Metadata();
        meta.setLength(length);
        meta.addContentType(contentType);
        meta.setCreateDate(System.currentTimeMillis());
        meta.setModifiedDate(System.currentTimeMillis());
        logicalData.setMetadata(meta);
        catalogue.registerResourceEntry(logicalData);
//        LogicalData relodedResource = (LogicalData) getCatalogue().getResourceEntryByLDRI(newResource.getLDRI());
    }

    void setContentType(String contentType) {
        this.contentType = contentType;
    }

    void setLength(Long length) {
        this.length = length;
    }

    void setCatalog(IDLCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    private void updeateData() throws CatalogueException {
        Metadata meta = logicalData.getMetadata();
        meta.setLength(length);
        meta.addContentType(contentType);
        meta.setModifiedDate(System.currentTimeMillis());
        logicalData.setMetadata(meta);

        catalogue.updateResourceEntry(logicalData);
    }
}
