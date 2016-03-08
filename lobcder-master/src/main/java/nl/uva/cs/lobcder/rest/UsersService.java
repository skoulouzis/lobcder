/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import nl.uva.cs.lobcder.util.CatalogueHelper;

/**
 *
 * @author S. Koulouzis
 */

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
