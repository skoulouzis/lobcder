/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;


import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used by the DRI service to supervise resources for corrupted files etc.
 *
 * @author dvasunin
 */

public class DRIDataResource {

    private JDBCatalogue catalogue = null;
    private HttpServletRequest request;

    public DRIDataResource(JDBCatalogue catalogue, HttpServletRequest request) {
        this.catalogue = catalogue;
        this.request = request;
    }

    /**
     * Sets supervised flag for a resource.
     *
     * @param uid the resource's id
     * @param flag the flag
     */
    @Path("{uid}/supervised/{flag}/")
    @PUT
    public void setSupervised(@PathParam("uid") Long uid, @PathParam("flag") Boolean flag) {
        try (Connection cn = catalogue.getConnection()) {
            try {
                LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
                if (res == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
                Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
                if (!mp.canWrite(p)) {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
                catalogue.setLogicalDataSupervised(uid, flag, cn);
                cn.commit();
            } catch (SQLException ex) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
                cn.rollback();
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gets supervised flag for a resource.
     *
     * @param uid the resource's id
     * @return the supervised flag for a resource.
     */
    @Path("{uid}/supervised/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public JAXBElement<Boolean> getSupervised(@PathParam("uid") Long uid) {
        try (Connection cn = catalogue.getConnection()) {
            LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
            if (!mp.canRead(p)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new JAXBElement<Boolean>(new QName("supervised"), Boolean.class, res.getSupervised());
        } catch (SQLException ex) {
            Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Sets checksum property for an item
     *
     * @param uid the resource's id
     * @param checksum the checksum. This value is not check if it's correct by
     * lobcder
     */
    @Path("{uid}/checksum/{checksum}/")
    @PUT
    public void setChecksum(@PathParam("uid") Long uid, @PathParam("checksum") String checksum) {
        try (Connection cn = catalogue.getConnection()) {
            try {
                LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
                if (res == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
                Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
                if (!mp.canWrite(p)) {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
                catalogue.setFileChecksum(uid, checksum, cn);
                cn.commit();
            } catch (SQLException ex) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
                cn.rollback();
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gets checksum property for an item.
     *
     * @param uid the resource's id
     * @return the checksum. This value is not check if it's correct by lobcder
     */
    @Path("{uid}/checksum/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public JAXBElement<String> getChecksum(@PathParam("uid") Long uid) {
        try (Connection cn = catalogue.getConnection()) {
            LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
            if (!mp.canRead(p)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new JAXBElement<String>(new QName("checksum"), String.class, res.getChecksum());
        } catch (SQLException ex) {
            Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Sets lastvalidationdate property for a resource
     *
     * @param uid the resource's id
     * @param lastValidationDate the date last validated
     */
    @Path("{uid}/lastValidationDate/{lastValidationDate}/")
    @PUT
    public void setLastValidationDate(@PathParam("uid") Long uid, @PathParam("lastValidationDate") Long lastValidationDate) {
        try (Connection cn = catalogue.getConnection()) {
            try {
                LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
                if (res == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
                Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
                if (!mp.canWrite(p)) {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
                catalogue.setLastValidationDate(uid, lastValidationDate, cn);
                cn.commit();
            } catch (SQLException ex) {
                Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
                cn.rollback();
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gets lastvalidationdate property for a resource
     *
     * @param uid the resource's id
     * @return the date last validated
     */
    @Path("{uid}/lastValidationDate/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public JAXBElement<Long> getLastValidationDate(@PathParam("uid") Long uid) {
        try (Connection cn = catalogue.getConnection()) {
            LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
            if (!mp.canRead(p)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new JAXBElement<Long>(new QName("lastValidationDate"), Long.class, res.getLastValidationDate());
        } catch (SQLException ex) {
            Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gets lastvalidationdate property for a resource in text format
     *
     * @param uid the resource's id
     * @return the date last validated in text format
     */
    @Path("{uid}/lastValidationDate/")
    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.TEXT_HTML})
    public String getLastValidationDateDate(@PathParam("uid") Long uid) {
        try (Connection cn = catalogue.getConnection()) {
            LogicalData res = catalogue.getLogicalDataByUid(uid, cn);
            if (res == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
            Permissions p = catalogue.getPermissions(uid, res.getOwner(), cn);
            if (!mp.canRead(p)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return new Date(res.getLastValidationDate() * 1000).toString();
        } catch (SQLException ex) {
            Logger.getLogger(DRIDataResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
