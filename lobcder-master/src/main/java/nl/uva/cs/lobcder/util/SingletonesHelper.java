package nl.uva.cs.lobcder.util;

import nl.uva.cs.lobcder.auth.*;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: dvasunin Date: 20.11.13 Time: 6:30 To change this template use File |
 * Settings | File Templates.
 */
public class SingletonesHelper {

    private static SingletonesHelper singletonesHelper;

    static {
        try {
            singletonesHelper = new SingletonesHelper();
        } catch (Exception e) {
            Logger.getLogger(SingletonesHelper.class.getName()).log(Level.SEVERE, "Could not create SingletonesHelper", e);
        }
    }

    public static SingletonesHelper getInstance() {
        return singletonesHelper;
    }

    private PrincipalCacheI principalCache = new PrincipalCache();
    private AuthTicket authTicket = new AuthTicket();
    private LocalDbAuth localDbAuth = new LocalDbAuth();
    private MyDataSource myDatasource = new MyDataSource();

    public SingletonesHelper() throws Exception {
        authTicket.setPrincipalCache(principalCache);
        authTicket.setDataSource(getDataSource());
        localDbAuth.setPrincipalCache(principalCache);
        localDbAuth.setDatasource(getDataSource());
    }

    public DataSource getDataSource() {
        return myDatasource.getDatasource();
    }

    public List<AuthI> getAuth() {
        List<AuthI> res = new ArrayList<>(2);
        res.add(localDbAuth);
        res.add(authTicket);
        return res;
    }

    public AuthI getTktAuth() {
        return authTicket;
    }

}
