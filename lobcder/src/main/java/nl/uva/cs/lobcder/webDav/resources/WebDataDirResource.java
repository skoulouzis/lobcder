/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.authdb.Permissions;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.catalogue.ResourceExistsException;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.vlet.exception.VlException;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataDirResource extends WebDataResource implements FolderResource, CollectionResource, DeletableCollectionResource  {

//    private ILogicalData entry;
//    private final IDLCatalogue catalogue;
    private boolean debug = true;

    public WebDataDirResource(JDBCatalogue catalogue, ILogicalData entry) throws IOException, Exception {
        super(catalogue, entry);
//        this.entry = entry;
        if (!getLogicalData().getType().equals(Constants.LOGICAL_FOLDER)) {
            throw new Exception("The logical data has the wonrg type: " + getLogicalData().getType());
        }
//        this.catalogue = catalogue;
        debug("Init. entry: " + getLogicalData().getLDRI());
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        debug("createCollection.");
        isWritable();
        try {            
            Path newCollectionPath = Path.path(getLogicalData().getLDRI(), newName);
            ILogicalData newFolderEntry = getCatalogue().getResourceEntryByLDRI(newCollectionPath, null);
            debug("\t newCollectionPath: " + newCollectionPath);
            if (newFolderEntry == null) {
                newFolderEntry = new LogicalData(newCollectionPath, Constants.LOGICAL_FOLDER, getCatalogue());             
                newFolderEntry.setCreateDate(System.currentTimeMillis());   
                newFolderEntry.setModifiedDate(System.currentTimeMillis());  
                WebDataDirResource res = new WebDataDirResource(getCatalogue(), newFolderEntry);
                newFolderEntry = getCatalogue().registerOrUpdateResourceEntry(newFolderEntry, null);
                getCatalogue().setPermissions(newFolderEntry.getUID(), new Permissions(getPrincipal()), null);
                return res;
            } else {
                throw new ConflictException(this, newName);
            }
        } 
        catch (Exception ex) {
            if(ex instanceof ConflictException) {
                throw (ConflictException)ex;
            } else {
                debug(ex.getMessage());
                throw new BadRequestException(this, ex.getMessage());
            }                
        }       
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException {
        debug("child.");
        isReadable();
        try {           
            ILogicalData child = getCatalogue().getResourceEntryByLDRI(getLogicalData().getLDRI().child(childName), null);
            if (child != null) {
                if (child.getType().equals(Constants.LOGICAL_FOLDER)) {
                    return new WebDataDirResource(getCatalogue(), child);
                }
                if (child.getType().equals(Constants.LOGICAL_FILE)) {
                    return new WebDataFileResource(getCatalogue(), child);
                }
                if (child.getType().equals(Constants.LOGICAL_DATA)) {
                    return new WebDataResource(getCatalogue(), child);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException {
        debug(getLogicalData().getLDRI().toPath() + " collection: getChildren.");
        isReadable();
        ArrayList<WebDataResource> children = new ArrayList<WebDataResource>() {
            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                for(WebDataResource r : this) {
                    sb.append(r.getLogicalData().getLDRI().toPath()).append("\n");
                }
                return sb.toString();
            }
        };      
        try {
            Collection<ILogicalData> childrenLD = getCatalogue().getChildren(getLogicalData().getLDRI().toPath(), null);
            if(childrenLD != null) {
                for(ILogicalData childLD : childrenLD) {
                    if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                        children.add(new WebDataDirResource(getCatalogue(), childLD));
                    }
                    if (childLD.getType().equals(Constants.LOGICAL_FILE)) {
                        children.add(new WebDataFileResource(getCatalogue(), childLD));
                    }
                    if (childLD.getType().equals(Constants.LOGICAL_DATA)) {
                        children.add(new WebDataResource(getCatalogue(), childLD));
                    }
                }
            }           
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        debug("Returning children: " + children);
        return children;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Resource resource;
        debug("createNew.");
        debug("\t newName: " + newName);
        debug("\t length: " + length);
        debug("\t contentType: " + contentType);
        try {
            Path newPath = Path.path(getLogicalData().getLDRI(), newName);
            ILogicalData newResource = getCatalogue().getResourceEntryByLDRI(newPath, null);
            if (newResource != null) { // Resource exists, update
                Permissions p = getCatalogue().getPermissions(newResource.getUID(), newResource.getOwner(), null);
                if (!getPrincipal().canWrite(p)) {
                    throw new NotAuthorizedException();
                }
                PDRI pdri = createPDRI(length);
                pdri.putData(inputStream);    
                newResource = getCatalogue().registerPdriForNewEntry(newResource, pdri, null);                                             
                newResource.setLength(length);
                newResource.setModifiedDate(System.currentTimeMillis());
                newResource.addContentType(contentType);                 
                getCatalogue().registerOrUpdateResourceEntry(newResource, null);
            } else { // Resource does not exists, create a new one
                isWritable(); // new need write prmissions for current collection
                newResource = new LogicalData(newPath, Constants.LOGICAL_FILE, getCatalogue());            
                newResource.setLength(length);
                newResource.setCreateDate(System.currentTimeMillis());
                newResource.setModifiedDate(System.currentTimeMillis());
                newResource.addContentType(contentType);
                           
                getCatalogue().registerOrUpdateResourceEntry(newResource, null);
                Permissions p = new Permissions(getPrincipal());
                getCatalogue().setPermissions(newResource.getUID(), p, null);
                PDRI pdri = createPDRI(length);
                pdri.putData(inputStream);   
                getCatalogue().registerPdriForNewEntry(newResource, pdri, null);             
            }

            return new WebDataFileResource(getCatalogue(), newResource);
        } catch (NotAuthorizedException e) {
            debug("NotAuthorizedException");
            throw e;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BadRequestException(this, ex.getMessage());           
        } 
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        throw new ConflictException("Not implemented");
        /*try {
            
            WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
            debug("copyTo.");
            debug("\t toCollection: " + toWDDR.getLogicalData().getLDRI().toPath());
            debug("\t name: " + name);
            isReadable();
            toWDDR.isWritable();
            Permissions p = new Permissions(getPrincipal());                 
            getCatalogue().copyEntry(getLogicalData().getUID(), p.getRolesPerm(), toWDDR, name);
//        } catch (NotAuthorizedException ex) {
//            throw ex;
//        } catch (BadRequestException ex) {
//            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(this, ex.getMessage());
            }
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
            debug(getLogicalData().getLDRI().toPath() + " folder delete.");
            Path parentPath = getLogicalData().getLDRI().getParent();
            ILogicalData parentLD = getCatalogue().getResourceEntryByLDRI(parentPath, null);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions p = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), null);
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException();
            }
            getCatalogue().removeResourceEntry(getLogicalData(), getPrincipal(), null);         

        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
//        try {
//            Permissions p = new Permissions(getLogicalData().getMetadata().getPermissionArray());
//            MyPrincipal principal = getPrincipal();
//            if (!p.canRead(principal)) {
//                throw new NotAuthorizedException();
//            }
//        } catch (Exception e) {
//            throw new NotAuthorizedException();
//        }

        //List contents. Fix this to work with browsers 
        debug("sendContent.");
//        StringTemplateGroup t = new StringTemplateGroup("page");
//        StringTemplate template = t.getInstanceOf("page");
//         template.setAttribute("path", getPath().toString());
//        t.setAttribute("static", _staticContentPath);
//        t.setAttribute("subject", new SubjectWrapper(getSubject()));
//        t.setAttribute("base", UrlPathWrapper.forEmptyPath());
//         template.write(new AutoIndentWriter(new OutputStreamWriter(out, "UTF-8")));

        for (Resource r : getChildren()) {
            String s = r.getName() + " - " + r.getUniqueId();
            out.write(s.getBytes());
        }
        out.flush();
        out.close();
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        debug("getMaxAgeSeconds.");
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);
        List<String> mimeTypes;
        if (accepts != null) {
            String[] acceptsTypes = accepts.split(",");
            if (getLogicalData().getContentTypes() != null) {
                mimeTypes = getLogicalData().getContentTypes();
                for (String accessType : acceptsTypes) {
                    for (String mimeType : mimeTypes) {
                        if (accessType.equals(mimeType)) {
                            return mimeType;
                        }
                    }
                }
                return mimeTypes.get(0);
            }
        }
        return null;
    }

    @Override
    public Long getContentLength() {
        debug("getContentLength.");
        return getLogicalData().getLength();
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        debug("#########################################moveTo: collection " + getLogicalData().getLDRI().toPath());
        WebDataDirResource rdst = (WebDataDirResource)rDest;
        debug("\t rDestgetName: " + rdst.getLogicalData().getLDRI().toPath() + " name: " + name);
        
        try {
            Path parentPath = getLogicalData().getLDRI().getParent(); //getPath().getParent();
            if (parentPath == null) {
                throw new NotAuthorizedException();
            }
            ILogicalData parentLD = getCatalogue().getResourceEntryByLDRI(getLogicalData().getLDRI().getParent(), null);
            if (parentLD == null) {
                throw new BadRequestException("Parent does not exist");
            }
            Permissions p = getCatalogue().getPermissions(parentLD.getUID(), parentLD.getOwner(), null);
                    
            if (!getPrincipal().canWrite(p)) {
                throw new NotAuthorizedException();
            }
            rdst.isWritable();
            getCatalogue().moveEntry(getLogicalData(), rdst.getLogicalData(), name, null);                        
        } catch (ResourceExistsException ex) {
            throw new ConflictException(rDest, ex.getMessage());
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (Exception ex) {
            throw new BadRequestException(this, ex.toString());
        }                
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        debug("\t entry.getCreateDate(): " + getLogicalData().getCreateDate());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + getLogicalData().getLDRI() + ": " + msg);
        }
    }

    @Override
    public boolean isLockedOutRecursive(Request rqst) {
        return false;
    }
}
