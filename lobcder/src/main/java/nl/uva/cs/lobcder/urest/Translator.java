package nl.uva.cs.lobcder.urest;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.CatalogueHelper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.sql.*;

/**
 * User: dvasunin
 * Date: 18.11.13
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */

@Log
@Path("getshort/")
public class Translator extends CatalogueHelper {


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

    protected void setShortId(String id_short, String id_long, Connection cn) throws SQLException {
        try(PreparedStatement ps = cn.prepareStatement("INSERT INTO tokens_table(short_tkt, long_tkt, exp_date) VALUES (?, ?, ?)")) {
            ps.setString(1, id_short);
            ps.setString(2, id_long);
            ps.setTimestamp(3, new Timestamp(new java.util.Date().getTime() + 1000 * 3600 * 24 * 7)); //set exp date week after
            ps.executeUpdate();
        }
    }

    @Path("{id}/")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getShortWeb(@PathParam("id") String longId) throws SQLException {
        try (Connection cn = getCatalogue().getConnection()) {
            cn.setAutoCommit(true);
            String shortId = getShortId(longId, cn);
            if(shortId == null){
                shortId = org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(12);
                setShortId(shortId, longId, cn);
            }
            return shortId;
        }
    }


}
