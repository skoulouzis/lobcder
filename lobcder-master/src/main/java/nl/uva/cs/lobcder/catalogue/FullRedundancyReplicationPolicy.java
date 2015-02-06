package nl.uva.cs.lobcder.catalogue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: dvasunin
 * Date: 04.02.2015
 * Time: 18:24
 * To change this template use File | Settings | File Templates.
 */
public class FullRedundancyReplicationPolicy implements ReplicationPolicy {

    @Override
    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            ArrayList<Long> result = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery("SELECT storageSiteId FROM storage_site_table WHERE private=FALSE AND removing=FALSE AND isCache=FALSE");
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }
            return result;
        }
    }
}
