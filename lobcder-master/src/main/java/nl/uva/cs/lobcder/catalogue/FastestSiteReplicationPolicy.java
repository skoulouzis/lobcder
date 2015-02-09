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
package nl.uva.cs.lobcder.catalogue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

/**
 *
 * @author S. Koulouzis
 */
class FastestSiteReplicationPolicy implements ReplicationPolicy {

    private Map<String, StorageSite> availableStorage = null;
    private int number;

    @Override
    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception {

        if (availableStorage == null || availableStorage.isEmpty()) {
            availableStorage = getStorageSites(connection);
            number = PropertiesHelper.getNumberOfSites();
        }
        Map<String, StorageSite> fastestMap = getFastestSites(connection, number);
        Collection<Long> fastest = new ArrayList<>();
        for (StorageSite s : fastestMap.values()) {
            fastest.add(s.getStorageSiteId());
        }

        return fastest;
    }

    private Map<String, StorageSite> getFastestSites(Connection connection, int number) throws SQLException {
        //Every now and then (20/80) go random
        int[] numsToGenerate = new int[]{1, 2};
        double[] discreteProbabilities = new double[]{0.85, 0.15};
        EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(numsToGenerate, discreteProbabilities);
        int random = distribution.sample();
        Map<String, StorageSite> res = new HashMap<>();
        if (random <= 1) {
            try (Statement s = connection.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT src, dst FROM speed_table "
                        + "GROUP BY averageSpeed DESC LIMIT " + number);
                Map<String, StorageSite> tmp = new HashMap<>();

                while (rs.next()) {
                    String src = rs.getString(1);
                    String dst = rs.getString(2);
                    Set<String> keys = availableStorage.keySet();
                    for (String k : keys) {
                        if (k.contains(src) || k.contains(dst)) {
                            tmp.put(k, availableStorage.get(k));
                        }
                    }
                }
                if (tmp.isEmpty()) {
                    ArrayList<String> keysAsArray = new ArrayList<>(availableStorage.keySet());
                    Random r = new Random();
                    StorageSite randomValue = availableStorage.get(keysAsArray.get(r.nextInt(keysAsArray.size())));
                    res.put(randomValue.getResourceURI(), randomValue);
                } else {
                    ArrayList<String> keysAsArray = new ArrayList<>(tmp.keySet());
                    Random r = new Random();
                    StorageSite randomValue = tmp.get(keysAsArray.get(r.nextInt(keysAsArray.size())));
                    res.put(randomValue.getResourceURI(), randomValue);
                }
            }
        } else {
            return getRandomStorageSite(number);
        }
        return res;
    }

    private Map<String, StorageSite> getRandomStorageSite(int number) {
        Map<String, StorageSite> res = new HashMap<>();
        List<StorageSite> copy = new LinkedList<>(availableStorage.values());
        Collections.shuffle(copy);
        if (number > availableStorage.size()) {
            number = availableStorage.size();
        }
        List<StorageSite> rand = copy.subList(0, number);
        for (StorageSite s : rand) {
            res.put(s.getResourceURI(), s);
        }
        return res;
    }

    private Map<String, StorageSite> getStorageSites(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT storageSiteId, resourceURI, "
                    + "currentNum, currentSize, quotaNum, quotaSize, username, "
                    + "password, encrypt FROM storage_site_table JOIN credential_table ON "
                    + "credentialRef = credintialId WHERE isCache != TRUE");
            Map<String, StorageSite> res = new HashMap<>();
            while (rs.next()) {
                Credential c = new Credential();
                c.setStorageSiteUsername(rs.getString(7));
                c.setStorageSitePassword(rs.getString(8));
                StorageSite ss = new StorageSite();
                ss.setStorageSiteId(rs.getLong(1));
                ss.setCredential(c);
                String uri = rs.getString(2);
                ss.setResourceURI(uri);
                ss.setCurrentNum(rs.getLong(3));
                ss.setCurrentSize(rs.getLong(4));
                ss.setQuotaNum(rs.getLong(5));
                ss.setQuotaSize(rs.getLong(6));
                ss.setEncrypt(rs.getBoolean(9));
                res.put(uri, ss);
            }
            return res;
        }
    }
}
