package nl.uva.cs.lobcder.urest;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.util.SingletonesHelper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;

/**
 * This service translates long authentication tokens to short so they can be used by WebDAV clients 
 * @author dvasunin
 */

@Log
@Path("getshort/")
public class Translator {

    /**
     * Gets short token 
     * @param longTocken the long token 
     * @return the short token 
     * @throws SQLException 
     */
    @Path("{longTocken}/")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Response getShortWeb(@PathParam("longTocken") String longTocken) throws SQLException {
        try{
            MyPrincipal principal = SingletonesHelper.getInstance().getTktAuth().checkToken("from_translator", longTocken);
            Long expDate = principal.getValidUntil();
            String userId = principal.getUserId();
            try (Connection cn = SingletonesHelper.getInstance().getDataSource().getConnection()) {
                String shortId = null;
                String longId = null;
                try(PreparedStatement ps = cn.prepareStatement("SELECT short_tkt, "
                        + "userId, long_tkt, exp_date FROM tokens_table WHERE userId = ?", 
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                    ps.setString(1, userId);
                    try(ResultSet rs = ps.executeQuery()){
                        if(rs.next())   {
                            shortId = rs.getString(1);
                            longId =  rs.getString(3);
                            if(!longId.equals(longTocken)){
                                rs.updateString(3, longTocken);
                                rs.updateTimestamp(4, new Timestamp(expDate.longValue() * 1000));
                                rs.updateRow();
                            }
                        } else {
                            rs.moveToInsertRow();
                            shortId = org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(12);
                            rs.updateString(1, shortId);
                            rs.updateString(2, userId);
                            rs.updateString(3, longTocken);
                            rs.updateTimestamp(4, new Timestamp(expDate.longValue() * 1000));
                            rs.insertRow();
                        }
                    }
                };
//                deleteOld(cn);
                return Response.ok(shortId).build();
            }  catch (SQLException e) {
                return Response.serverError().entity(e).build();
            }
        } catch (Throwable th) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Ticket is not valid").build();
        }
    }

    public void deleteOld(Connection cn) throws SQLException {
        try(Statement stmt = cn.createStatement()){
            stmt.executeUpdate("DELETE FROM tokens_table WHERE exp_date < NOW()");
        }
    }
}
