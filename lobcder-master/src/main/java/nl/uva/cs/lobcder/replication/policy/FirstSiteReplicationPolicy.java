/*
 * Copyright 2015 S. Koulouzis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.uva.cs.lobcder.replication.policy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author S. Koulouzis
 */
public class FirstSiteReplicationPolicy implements ReplicationPolicy {

    private ArrayList<Long> queryResult = new ArrayList<>();

    @Override
    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception {
        if (queryResult.isEmpty()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT "
                        + "storageSiteId "
                        + "FROM storage_site_table "
                        + "WHERE private=FALSE "
                        + "AND removing=FALSE "
                        + "AND isCache=FALSE LIMIT 1");
                while (resultSet.next()) {
                    queryResult.add(resultSet.getLong(1));
                }
            }
        }
        return queryResult;

    }
}
