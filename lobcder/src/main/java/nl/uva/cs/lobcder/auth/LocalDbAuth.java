/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.logging.Level;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.Constants;

/**
 *
 * @author skoulouz
 */
@Log
public class LocalDbAuth implements AuthI {

    private DataSource datasource = null;
    private PrincipalCacheI pc = null;
    private int attempts=0;

    public LocalDbAuth() throws NamingException {
        javax.naming.Context ctx = new InitialContext();
        javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
        pc = (PrincipalCacheI) envContext.lookup("bean/PrincipalCache");
        datasource = (DataSource) envContext.lookup("jdbc/lobcder");
    }

    @Override
    public MyPrincipal checkToken(String token) {
//        Connection connection = null;
        MyPrincipal res = null;
        try {
            if (pc != null) {
                synchronized (pc) {
                    res = pc.getPrincipal(token);
                }
            }
            if (res == null) {
                String uname;
                int id;
                try (Connection connection = datasource.getConnection()) {
                    try (Statement s = connection.createStatement()) {
                        HashSet<String> roles = new HashSet<>();
                        String query = "SELECT id, uname FROM auth_usernames_table WHERE token = '" + token + "'";
                        LocalDbAuth.log.fine(query);
                        try (ResultSet rs = s.executeQuery(query)) {
                            if (rs.next()) {
                                id = rs.getInt(1);
                                uname = rs.getString(2);
                                roles.add("other");
                                roles.add(uname);
                            } else {
                                return null;
                            }
                            query = "SELECT roleName FROM auth_roles_tables WHERE unameRef = " + id;
                            LocalDbAuth.log.fine(query);
                            try (ResultSet rs2 = s.executeQuery(query)) {
                                while (rs2.next()) {
                                    roles.add(rs2.getString(1));
                                }
                                res = new MyPrincipal(uname, roles);
                            }
                        }
                    }
                }
//                connection = datasource.getConnection();

            }
            if (pc != null) {
                synchronized (pc) {
                    pc.putPrincipal(token, res);
                }
            }
            attempts=0;
            return res;
        } catch (Exception ex) {
            if (ex instanceof SQLException 
                    && attempts <=Constants.RECONNECT_NTRY) {
                attempts++;
                checkToken(token);
            } else {
                LocalDbAuth.log.log(Level.SEVERE, null, ex);
                return null;
            }
        } finally {
//            try {
//                if (connection != null) {
//                    connection.close();
//                }
//            } catch (SQLException ex) {
//                LocalDbAuth.log.log(Level.SEVERE, null, ex);
//            }
        }
        return null;
    }
}
