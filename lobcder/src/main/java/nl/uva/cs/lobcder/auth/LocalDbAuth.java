/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import lombok.extern.java.Log;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.logging.Level;

/**
 *
 * @author skoulouz
 */
@Log
public class LocalDbAuth implements AuthI {

    private DataSource datasource = null;
    private PrincipalCacheI pc = null;

    public LocalDbAuth() throws NamingException {
            javax.naming.Context ctx = new InitialContext();
            javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
            pc = (PrincipalCacheI) envContext.lookup("bean/PrincipalCache");
            datasource = (DataSource) envContext.lookup("jdbc/lobcder");
    }

    @Override
    public MyPrincipal checkToken(String token) {
        Connection connection = null;
        MyPrincipal res = null;
        try {
            if (pc != null) {
                res = pc.getPrincipal(token);
            }
            if (res == null) {
                String uname;
                int id;
                connection = datasource.getConnection();
                Statement s = connection.createStatement();
                HashSet<String> roles = new HashSet<String>();
                String query = "SELECT id, uname FROM auth_usernames_table WHERE token = '" + token + "'";
                LocalDbAuth.log.fine(query);
                ResultSet rs = s.executeQuery(query);
                if (rs.next()) {
                    id = rs.getInt(1);
                    uname = rs.getString(2);
                    roles.add("other");
                } else {
                    return null;
                }
                query = "SELECT roleName FROM auth_roles_tables WHERE unameRef = " + id;
                LocalDbAuth.log.fine(query);
                rs = s.executeQuery(query);
                while (rs.next()) {
                    roles.add(rs.getString(1));
                }
                res = new MyPrincipal(uname, roles);
            }
            if (pc != null) {
                pc.putPrincipal(token, res);
            }
            return res;
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                LocalDbAuth.log.log(Level.SEVERE, null, ex);
            }
        }
    }
}
