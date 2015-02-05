package nl.uva.cs.lobcder.catalogue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * User: dvasunin
 * Date: 04.02.2015
 * Time: 18:08
 * To change this template use File | Settings | File Templates.
 */
public class RandomReplicationPolicy implements ReplicationPolicy {

    @Override
    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            ArrayList<Long> queryResult = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery("SELECT storageSiteId FROM storage_site_table WHERE private=FALSE AND removing=FALSE");
            while (resultSet.next()) {
                queryResult.add(resultSet.getLong(1));
            }
            Long[] queryResArr = queryResult.toArray(new Long[queryResult.size()]);
            ArrayList<Long> result = new ArrayList<>();
            result.add(queryResArr[new Random().nextInt(queryResArr.length)]);
            return result;
        }
    }
}
