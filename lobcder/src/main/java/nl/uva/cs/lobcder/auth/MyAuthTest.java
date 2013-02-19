/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.util.Debug;
import org.slf4j.LoggerFactory;

/**
 *
 * @author skoulouz
 */
public class MyAuthTest extends Debug implements AuthI {

    private DataSource datasource = null;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MyAuthTest.class);

    @Override
    protected boolean debug() {
        return true;
    }

    @Override
    protected void debug(String msg) {
        if (debug()) {
            log.debug(msg);
        }
    }

    public Connection getConnection() throws Exception {
        if (datasource == null) {
            String jndiName = "jdbc/lobcder";
            Context ctx = new InitialContext();
            if (ctx == null) {
                throw new Exception("JNDI could not create InitalContext ");
            }
            Context envContext = (Context) ctx.lookup("java:/comp/env");
            datasource = (DataSource) envContext.lookup(jndiName);
        }
        return datasource.getConnection();
    }

    @Override
    public MyPrincipal checkToken(String token) {
        Connection connection = null;
        try {
            String uname;
            int id;
            connection = getConnection();
            Statement s = connection.createStatement();
            HashSet<String> roles = new HashSet<String>();
            String query = "SELECT id, uname FROM auth_usernames_table WHERE token = '" + token + "'";
            //String query = "SELECT role_name FROM auth_roles_tables JOIN auth_usernames_table ON auth_usernames_table.id = auth_roles_tables.uname_id WHERE auth_usernames_table.uname = '" + token + "'";
            debug(query);
            ResultSet rs = s.executeQuery(query);
            if (rs.next()) {
                id = rs.getInt(1);
                uname = rs.getString(2);
            } else {
                return null;
            }
            query = "SELECT role_name FROM auth_roles_tables WHERE uname_id = " + id;
            debug(query);
            rs = s.executeQuery(query);
            while (rs.next()) {
                roles.add(rs.getString(1));
            }
            if (roles.isEmpty()) {
                return null;
            } else {
                return new MyPrincipal(uname, roles);
            }
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(MyAuthTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
