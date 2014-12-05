/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import lombok.Setter;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.Constants;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;

/**
 *
 * @author skoulouz
 */
@Log
public class LocalDbAuth implements AuthI {

    @Setter
    private DataSource datasource = null;
    @Setter
    private PrincipalCacheI principalCache = null;
    private int attempts = 0;

    @Override
    public MyPrincipal checkToken(String uname,String token) {
//        Connection connection = null;
        MyPrincipal res = null;
        try {
            if (principalCache != null) {
                synchronized (principalCache) {
                    res = principalCache.getPrincipal(token);
                }
            }
            if (res == null) {
                
                int id;
                try (Connection connection = datasource.getConnection()) {
                    try (Statement s = connection.createStatement()) {
                        HashSet<String> roles = new HashSet<>();
                        //Fix this!!! Can use injection with % 
//                        fix
                        String query = "SELECT id, uname FROM auth_usernames_table WHERE token LIKE '" + token + "' AND uname LIKE '"+uname+"'";
                        try (ResultSet rs = s.executeQuery(query)) {
                            if (rs.next()) {
                                id = rs.getInt(1);
                                uname = rs.getString(2);
//                                roles.add("other");
                                roles.add(uname);
                            } else {
                                return null;
                            }
                            query = "SELECT roleName FROM auth_roles_tables WHERE unameRef = " + id;
                            try (ResultSet rs2 = s.executeQuery(query)) {
                                while (rs2.next()) {
                                    roles.add(rs2.getString(1));
                                }
                                res = new MyPrincipal(uname, roles, token);
                            }
                        }
                    }
                }
//                connection = datasource.getConnection();

            }
            if (principalCache != null) {
                synchronized (principalCache) {
                    principalCache.putPrincipal(token, res, new Date().getTime() + 10000);
                }
            }
            attempts = 0;
            return res;
        } catch (Exception ex) {
            if (ex instanceof SQLException
                    && attempts <= Constants.RECONNECT_NTRY) {
                attempts++;
                checkToken(uname, token);
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
