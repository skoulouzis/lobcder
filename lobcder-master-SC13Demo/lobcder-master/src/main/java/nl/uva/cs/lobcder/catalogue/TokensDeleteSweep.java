package nl.uva.cs.lobcder.catalogue;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;

/**
 * User: dvasunin
 * Date: 18.11.13
 * Time: 18:16
 * To change this template use File | Settings | File Templates.
 */
@Log
public class TokensDeleteSweep implements Runnable {

    private final int loopsToSkip;
    private int count = 0;
    private final DataSource dataSource;

    public TokensDeleteSweep(DataSource dataSource) throws IOException {
        this.dataSource = dataSource;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(PropertiesHelper.propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        loopsToSkip = Integer.valueOf(properties.getProperty("tokens.deletesweep.count", "100"));
        count = 0;
    }

    @Override
    public void run() {
        try{
            if(++count == loopsToSkip){
                count = 0;
                try(Connection cn = dataSource.getConnection()){
                    cn.setAutoCommit(true);
                    try(Statement stmt = cn.createStatement()){
                        int res = stmt.executeUpdate("DELETE FROM tokens_table WHERE exp_date < NOW()");
                        TokensDeleteSweep.log.log(Level.FINE, "{0} items expired in the tokens_table, deleted", new Object[]{Integer.valueOf(res)});
                    }
                }
            }
        } catch (Exception e) {
            TokensDeleteSweep.log.log(Level.SEVERE, null, e);
        }
    }
}
