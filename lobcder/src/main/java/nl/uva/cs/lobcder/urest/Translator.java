package nl.uva.cs.lobcder.urest;

import lombok.extern.java.Log;
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

    protected String getShortId(String id, Connection cn) throws SQLException {
        String res = null;
        try(PreparedStatement ps = cn.prepareStatement("SELECT short_tkt FROM tokens_table WHERE long_tkt = ?")) {
            ps.setString(1, id);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next())   {
                    res = rs.getString(1);
                }
            }
        }
        return res;
    }

    protected void setShortId(String id_short, String id_long, Long exp, Connection cn) throws SQLException {
        try(PreparedStatement ps = cn.prepareStatement("INSERT INTO tokens_table(short_tkt, long_tkt, exp_date) VALUES (?, ?, ?)")) {
            ps.setString(1, id_short);
            ps.setString(2, id_long);
            ps.setTimestamp(3, new Timestamp(exp.longValue() * 1000));
            ps.executeUpdate();
        }
    }

    

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
            Long expDate = SingletonesHelper.getInstance().getTktAuth().checkToken(longTocken).getValidUntil();
            try (Connection cn = SingletonesHelper.getInstance().getDataSource().getConnection()) {
                String shortId = getShortId(longTocken, cn);
                if(shortId == null){
                    shortId = org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(12);
                    setShortId(shortId, longTocken, expDate, cn);
                }
                return Response.ok(shortId).build();
            }  catch (SQLException e) {
                return Response.serverError().entity(e).build();
            }
        } catch (Throwable th) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Ticket is not valid").build();
        }
    }
}
