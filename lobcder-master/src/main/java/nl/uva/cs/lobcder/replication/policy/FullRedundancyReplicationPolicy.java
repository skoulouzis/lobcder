package nl.uva.cs.lobcder.replication.policy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: dvasunin Date: 04.02.2015 Time: 18:24 To change this template use File
 * | Settings | File Templates.
 */
public class FullRedundancyReplicationPolicy implements ReplicationPolicy {

    private ArrayList<Long> queryResult = new ArrayList<>();

    @Override
    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception {
        if (queryResult.isEmpty()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT storageSiteId FROM storage_site_table "
                        + "WHERE private=FALSE AND removing=FALSE AND isCache=FALSE AND readOnly = FALSE");
                while (resultSet.next()) {
                    queryResult.add(resultSet.getLong(1));
                }
            }
//            number = PropertiesHelper.getNumberOfSites();
        }
        return queryResult;
    }
}
