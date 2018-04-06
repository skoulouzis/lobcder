package nl.uva.cs.lobcder.catalogue.repsweep.policy;

import nl.uva.cs.lobcder.catalogue.beans.CredentialBean;
import nl.uva.cs.lobcder.catalogue.beans.StorageSiteBean;
import nl.uva.cs.lobcder.util.MyDataSource;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * User: dvasunin Date: 04.02.2015 Time: 18:08 To change this template use File
 * | Settings | File Templates.
 */
public class RandomReplicationPolicyJDBC extends MyDataSource implements ReplicationPolicy {

    private int number;

    public RandomReplicationPolicyJDBC() throws NamingException {
    }


    @Override
    public Set<StorageSiteBean> getSitesToReplicate() throws Exception {
        try(Connection connection = getConnection()) {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT storageSiteId, resourceUri, encrypt, extra, username, password FROM storage_site_table JOIN credential_table ON credentialRef=credintialId WHERE private=FALSE AND removing=FALSE AND isCache=FALSE");
                ArrayList<StorageSiteBean> queryResult = new ArrayList<>(resultSet.getFetchSize());
                while (resultSet.next()) {
                    queryResult.add(
                            new StorageSiteBean(
                                    resultSet.getLong(1),
                                    resultSet.getString(2),
                                    resultSet.getBoolean(3),
                                    Boolean.FALSE,
                                    resultSet.getString(4),
                                    new CredentialBean(
                                            resultSet.getString(5),
                                            resultSet.getString(6)
                                    )
                            )
                    );
                }
                number = PropertiesHelper.getNumberOfSites();
                Collections.shuffle(queryResult);
                if (number > queryResult.size()) {
                    number = queryResult.size();
                }
                return new HashSet<>(queryResult.subList(0, number));
            }
        }
    }
}
