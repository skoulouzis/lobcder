/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.rest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author dvasunin
 */
public class DRIDataResource {

    JDBCatalogue catalogue = null;
    HttpServletRequest request;

    public DRIDataResource(JDBCatalogue catalogue, HttpServletRequest request) {
        this.catalogue = catalogue;
        this.request = request;             
    }

    @Path("{uid}/supervised/{flag}/")
    @PUT
    public void setSupervised(@PathParam("uid") Long uid, @PathParam("flag") Boolean flag) {
        Connection cn = null;
        try {
            cn = catalogue.getConnection();
            LogicalData res = catalogue.getResourceEntryByUID(uid, cn);
            if(res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canWrite(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            catalogue.setFileSupervised(uid, flag, cn);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (cn != null && !cn.isClosed()) {
                    cn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Path("{uid}/supervised/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public JAXBElement<Boolean> getSupervised(@PathParam("uid") Long uid) {
        try {
            LogicalData res = catalogue.getResourceEntryByUID(uid, null);
            if(res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }            
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canRead(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new JAXBElement<Boolean>(new QName("supervised"), Boolean.class, res.getSupervised());
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("{uid}/checksum/{checksum}/")
    @PUT
    public void setChecksum(@PathParam("uid") Long uid, @PathParam("checksum") Long checksum) {
        Connection cn = null;
        try {
            cn = catalogue.getConnection();
            LogicalData res = catalogue.getResourceEntryByUID(uid, cn);
            if(res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }            
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canWrite(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            catalogue.setFileChecksum(uid, checksum, cn);
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (cn != null && !cn.isClosed()) {
                    cn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Path("{uid}/checksum/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public JAXBElement<Long> getChecksum(@PathParam("uid") Long uid) {
        try {
            LogicalData res = catalogue.getResourceEntryByUID(uid, null);
            if(res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }            
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canRead(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new JAXBElement<Long>(new QName("checksum"), Long.class, res.getChecksum());
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("{uid}/lastValidationDate/{lastValidationDate}/")
    @PUT
    public void setLastValidationDate(@PathParam("uid") Long uid, @PathParam("lastValidationDate") Long lastValidationDate) {
        Connection cn = null;
        try {
            cn = catalogue.getConnection();
            LogicalData res = catalogue.getResourceEntryByUID(uid, cn);
            if(res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }            
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canWrite(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            catalogue.setLastValidationDate(uid, lastValidationDate, cn);
        } catch (CatalogueException ex) {          
            try {
                Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
                if(cn != null) {
                    cn.rollback();
                }
            } catch (SQLException ex1) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex1);
            } finally {
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            try {
                if (cn != null && !cn.isClosed()) {
                    cn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Path("{uid}/lastValidationDate/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public JAXBElement<Long> getLastValidationDate(@PathParam("uid") Long uid) {
        try {
            LogicalData res = catalogue.getResourceEntryByUID(uid, null);
            if(res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }            
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canRead(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new JAXBElement<Long>(new QName("lastValidationDate"), Long.class, res.getLastValidationDate());        
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Path("{uid}/lastValidationDate/")
    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.TEXT_HTML})
    public String getLastValidationDateDate(@PathParam("uid") Long uid) {
        try {
            LogicalData res = catalogue.getResourceEntryByUID(uid, null);
            if(res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }            
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            if (!mp.canRead(res.getPermissions())) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new Date(res.getLastValidationDate() * 1000).toString();
        } catch (CatalogueException ex) {
            Logger.getLogger(DRItemsResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }    
}
