package nl.uva.cs.lobcder.replication.policy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 * User: dvasunin Date: 04.02.2015 Time: 18:08 To change this template use File
 * | Settings | File Templates.
 */
public class RandomReplicationPolicy implements ReplicationPolicy {

    private ArrayList<Long> queryResult = new ArrayList<>();
    private int number;

    @Override
    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception {
        if (queryResult.isEmpty()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT count(storageSiteId) FROM storage_site_table WHERE private=FALSE AND removing=FALSE AND isCache=FALSE");
                while (resultSet.next()) {
                    queryResult.add(resultSet.getLong(1));
                }
            }
            number = PropertiesHelper.getNumberOfSites();
        }
//        Long[] queryResArr = queryResult.toArray(new Long[queryResult.size()]);
//        ArrayList<Long> result = new ArrayList<>();
//        result.add(queryResArr[new Random().nextInt(queryResArr.length)]);


        Collections.shuffle(queryResult);
        if (number > queryResult.size()) {
            number = queryResult.size();
        }
        List<Long> rand = queryResult.subList(0, number);
        return rand;

    }
//    @Override
//    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception {
//        try (Statement statement = connection.createStatement()) {
//            ResultSet resultSet = statement.executeQuery("SELECT storageSiteId  "
//                    + "FROM storage_site_table "
//                    + "WHERE private=FALSE "
//                    + "AND removing=FALSE "
//                    + "AND isCache=FALSE ORDER BY RAND() LIMIT " + PropertiesHelper.getNumberOfSites());
//            while (resultSet.next()) {
//                cache.add(resultSet.getLong(1));
//            }
//            return cache;
//        }
//    }
}
