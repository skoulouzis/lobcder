/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.rest.wrappers.UsersWrapper;
import nl.uva.cs.lobcder.rest.wrappers.UsersWrapperList;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.vlet.exception.VlException;

/**
 *
 * @author S. Koulouzis
 */
@Log
@Path("users/")
public class UsersService extends CatalogueHelper {

    @Context
    UriInfo info;
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse servletResponse;

//    @Path("query/")
//    @GET
//    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
//    public UsersWrapperList getXml() throws FileNotFoundException, VlException, URISyntaxException, IOException, MalformedURLException, Exception {
//        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
//        if (mp.isAdmin()) {
//            try (Connection cn = getCatalogue().getConnection()) {
//                List<UsersWrapper> res = queryUsers(cn);
//                UsersWrapperList uwl = new UsersWrapperList();
//                uwl.setUsers(res);
//                return uwl;
//            } catch (SQLException ex) {
//                log.log(Level.SEVERE, null, ex);
//                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
//            }
//        }
//        return null;
//    }
//
//    private List<UsersWrapper> queryUsers(Connection cn) throws SQLException {
//        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
//        List<String> ids = queryParameters.get("id");
//        List<UsersWrapper> users = null;
//        if (ids != null && ids.size() > 0 && ids.get(0).equals("all")) {
//            users = getCatalogue().getUsers(cn);
//        }
//        return users;
//    }
}
