/*
 * Copyright 2014 S. Koulouzis
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
package nl.uva.cs.lobcder.optimization;

import io.milton.common.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import lombok.extern.java.Log;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.MyDataSource;
import org.apache.commons.dbcp.BasicDataSource;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class LDClustering implements Runnable {

    private static DefaultDataset fileDataset;
    private static Dataset[] fileClusters;
    private final BasicDataSource dataSource;

    public LDClustering() throws NamingException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        dataSource = new BasicDataSource();

        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUsername("user");
        dataSource.setPassword("pass");
        String url = "jdbc:mysql://localhost:3306/DB";
        dataSource.setUrl(url);
        dataSource.setMaxActive(10);
        dataSource.setMaxIdle(5);
        dataSource.setInitialSize(5);
        dataSource.setValidationQuery("SELECT 1");

//        DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
//        
//        datasource2 = DriverManager.getConnection(url);
//        datasource2.setAutoCommit(false);

        fileDataset = new DefaultDataset();
    }

    public Connection getConnection() throws SQLException {
        Connection cn = dataSource.getConnection();
        cn.setAutoCommit(false);
        return cn;
    }

    public static void main(String args[]) throws Exception {



        LDClustering c = new LDClustering();
        c.run();
    }

    @Override
    public void run() {
        try {
            buildOrUpdateDataset();
//            cluster();
//            printClusters();
        } catch (SQLException ex) {
            Logger.getLogger(LDClustering.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void buildOrUpdateDataset() throws SQLException {
        try (Connection connection = getConnection()) {
            LogicalData root = getLogicalDataByUid(Long.valueOf(1), connection);
            addFeatures(Path.root, root, connection, null);
        }
    }

    private LogicalData getLogicalDataByUid(Long UID, @Nonnull Connection connection) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement("SELECT parentRef, ownerId, datatype, ldName, "
                + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                + "FROM ldata_table WHERE ldata_table.uid = ?")) {
            ps.setLong(1, UID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(UID);
                res.setParentRef(rs.getLong(1));
                res.setOwner(rs.getString(2));
                res.setType(rs.getString(3));
                res.setName(rs.getString(4));
                res.setCreateDate(rs.getTimestamp(5).getTime());
                res.setModifiedDate(rs.getTimestamp(6).getTime());
                res.setLength(rs.getLong(7));
                res.setContentTypesAsString(rs.getString(8));
                res.setPdriGroupId(rs.getLong(9));
                res.setSupervised(rs.getBoolean(10));
                res.setChecksum(rs.getString(11));
                res.setLastValidationDate(rs.getLong(12));
                res.setLockTokenID(rs.getString(13));
                res.setLockScope(rs.getString(14));
                res.setLockType(rs.getString(15));
                res.setLockedByUser(rs.getString(16));
                res.setLockDepth(rs.getString(17));
                res.setLockTimeout(rs.getLong(18));
                res.setDescription(rs.getString(19));
                res.setDataLocationPreference(rs.getString(20));
                res.setStatus(rs.getString(21));
                return res;
            } else {
                return null;
            }
        }
    }

    private void addFeatures(Path p, LogicalData node, Connection connection, ArrayList<Path> nodes) throws SQLException {
        Collection<LogicalData> children = getChildrenByParentRef(node.getUid(), connection);
        for (LogicalData n : children) {
            ArrayList<Double> featuresList = new ArrayList<>();

            Long cDate = n.getCreateDate();
            if (cDate != null) {
                featuresList.add(Double.valueOf(cDate));
            }
            Long vDate = n.getLastValidationDate();
            if (vDate != null) {
                featuresList.add(Double.valueOf(vDate));
            }
            Long len = n.getLength();
            if (len != null) {
                featuresList.add(Double.valueOf(len));
            }
            Long lTimeout = n.getLockTimeout();
            if (lTimeout != null) {
                featuresList.add(Double.valueOf(lTimeout));
            }
            Long mDate = n.getModifiedDate();
            if (mDate != null) {
                featuresList.add(Double.valueOf(mDate));
            }

            featuresList.add(Double.valueOf(n.getParentRef()));
            featuresList.add(Double.valueOf(n.getPdriGroupId()));
            featuresList.add(Double.valueOf(n.getUid()));


            double[] featuresArray = new double[featuresList.size()];
            for (int i = 0; i < featuresArray.length; i++) {
                featuresArray[i] = featuresList.get(i);
            }
            Instance instance = new DenseInstance(featuresArray, node.getName());
            log.log(Level.INFO, "Add instance: " + n.getName());
            fileDataset.add(instance);

            if (n.getUid() != node.getUid()) {
//                log.log(Level.INFO, "children: " + ld.getName());
                Path nextPath = Path.path(p, n.getName());
//                log.log(Level.INFO, "node: " + nextPath);
                if (n.isFolder()) {
                    addFeatures(nextPath, n, connection, nodes);
                }
            }
        }
    }

    private Collection<LogicalData> getChildrenByParentRef(Long parentRef, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid, ownerId, datatype, ldName, createDate, modifiedDate, ldLength, "
                + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                + "description, locationPreference "
                + "FROM ldata_table WHERE ldata_table.parentRef = ?")) {
            preparedStatement.setLong(1, parentRef);
            ResultSet rs = preparedStatement.executeQuery();
            LinkedList<LogicalData> res = new LinkedList<>();
            while (rs.next()) {
                LogicalData element = new LogicalData();
                element.setUid(rs.getLong(1));
                element.setParentRef(parentRef);
                element.setOwner(rs.getString(2));
                element.setType(rs.getString(3));
                element.setName(rs.getString(4));
                element.setCreateDate(rs.getTimestamp(5).getTime());
                element.setModifiedDate(rs.getTimestamp(6).getTime());
                element.setLength(rs.getLong(7));
                element.setContentTypesAsString(rs.getString(8));
                element.setPdriGroupId(rs.getLong(9));
                element.setSupervised(rs.getBoolean(10));
                element.setChecksum(rs.getString(11));
                element.setLastValidationDate(rs.getLong(12));
                element.setLockTokenID(rs.getString(13));
                element.setLockScope(rs.getString(14));
                element.setLockType(rs.getString(15));
                element.setLockedByUser(rs.getString(16));
                element.setLockDepth(rs.getString(17));
                element.setLockTimeout(rs.getLong(18));
                element.setDescription(rs.getString(19));
                element.setDataLocationPreference(rs.getString(20));
                res.add(element);
            }
            return res;
        }
    }

    private static void cluster() {
        Clusterer km = new KMeans();
        fileClusters = km.cluster(fileDataset);
    }

    private static void printClusters() {
        for (int i = 0; i < fileClusters.length; i++) {
            System.out.println("Cluster" + i + ": ");
            Dataset ds = fileClusters[i];
            Iterator<Instance> iter = ds.iterator();

            while (iter.hasNext()) {
                Instance inst = iter.next();
                int id = inst.getID();
                System.out.println("------------------ ");
                System.out.println("ID: " + id);
                Object classValue = inst.classValue();
                if (classValue != null) {
                    System.out.println("classValue: " + classValue.getClass().getName());
                    System.out.println("classValue: " + classValue);
                }
//                keys = inst.keySet();
//                kIter = keys.iterator();
//                while (kIter.hasNext()) {
//                    Integer key = kIter.next();
//                    Double val = inst.get(key);
//                    System.out.println("key: " + key + " val: " + val);
//                }
                System.out.println("------------------ ");
            }
        }
    }
}
