/*
 * Copyright 2014 alogo.
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
import io.milton.http.Request;
import io.milton.http.Request.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.frontend.RequestWapper;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.MyDataSource;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class LDClustering extends MyDataSource implements Runnable {

    private FastVector metdataAttributes;
    private int k;

    public LDClustering() throws NamingException, ParseException, Exception {
        initAttributes();
        k = 50;
//        buildOrUpdateDataset();
    }

    private void initAttributes() throws ParseException, Exception {
        int index = 0;
        Attribute uidAttribute = new Attribute("uid", index++);
        // Declare a nominal attribute along with its values
        FastVector verbVector = new FastVector(Request.Method.values().length);
        for (Request.Method m : Request.Method.values()) {
            verbVector.addElement(m.code);
        }

        Attribute verbAttribute = new Attribute("verb", verbVector, index++);
        Attribute checksumAttribute = new Attribute("checksum", (FastVector) null, index++);

        Attribute contentTypeAttribute = new Attribute("contentType", (FastVector) null, index++);

        Attribute createDateAttribute = new Attribute("createDate", "yyyy-MM-dd HH:mm:ss", index++);
        Attribute locationPreferenceAttribute = new Attribute("locationPreference", (FastVector) null, index++);
        Attribute descriptionAttribute = new Attribute("description", (FastVector) null, index++);
        Attribute validationDateAttribute = new Attribute("validationDate", "yyyy-MM-dd HH:mm:ss", index++);

        Attribute lengthAttribute = new Attribute("length", index++);
        Attribute modifiedDateAttribute = new Attribute("modifiedDate", "yyyy-MM-dd HH:mm:ss", index++);
        Attribute pathAttribute = new Attribute("name", (FastVector) null, index++);
        Attribute parentRefAttribute = new Attribute("parentRef", index++);
        Attribute statusAttribute = new Attribute("status", (FastVector) null, index++);
        FastVector typeVector = new FastVector(3);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_DATA);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FILE);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FOLDER);
        Attribute typeAttribute = new Attribute("type", typeVector, index++);

        // Declare the class attribute along with its values
        FastVector supervisedVector = new FastVector(2);
        supervisedVector.addElement("true");
        supervisedVector.addElement("false");
        Attribute supervisedAttribute = new Attribute("supervised", supervisedVector, index++);
        Attribute ownerAttribute = new Attribute("owner", (FastVector) null, index++);


        // Declare the feature vector
        metdataAttributes = new FastVector();
        metdataAttributes.addElement(uidAttribute);//0
        metdataAttributes.addElement(verbAttribute);//1
        metdataAttributes.addElement(checksumAttribute);//2
        metdataAttributes.addElement(contentTypeAttribute);//3
        metdataAttributes.addElement(createDateAttribute);//4
        metdataAttributes.addElement(locationPreferenceAttribute);//5
        metdataAttributes.addElement(descriptionAttribute);//6
        metdataAttributes.addElement(validationDateAttribute);//7
        metdataAttributes.addElement(lengthAttribute);//8
        metdataAttributes.addElement(modifiedDateAttribute);//9
        metdataAttributes.addElement(pathAttribute);//10
        metdataAttributes.addElement(parentRefAttribute);//11
        metdataAttributes.addElement(statusAttribute);//12
        metdataAttributes.addElement(typeAttribute);//13
        metdataAttributes.addElement(supervisedAttribute);//14
        metdataAttributes.addElement(ownerAttribute);//15
    }

    private void buildOrUpdateDataset() throws SQLException, Exception {
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT uid, parentRef, ownerId, datatype, ldName, "
                    + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                    + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                    + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                    + "FROM ldata_table")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    LogicalData res = new LogicalData();
                    res.setUid(rs.getLong(1));
                    res.setParentRef(rs.getLong(2));
                    res.setOwner(rs.getString(3));
                    res.setType(rs.getString(4));
                    res.setName(rs.getString(5));
                    res.setCreateDate(rs.getTimestamp(6).getTime());
                    res.setModifiedDate(rs.getTimestamp(7).getTime());
                    res.setLength(rs.getLong(8));
                    res.setContentTypesAsString(rs.getString(9));
                    res.setPdriGroupId(rs.getLong(10));
                    res.setSupervised(rs.getBoolean(11));
                    res.setChecksum(rs.getString(12));
                    res.setLastValidationDate(rs.getLong(13));
                    res.setLockTokenID(rs.getString(14));
                    res.setLockScope(rs.getString(15));
                    res.setLockType(rs.getString(16));
                    res.setLockedByUser(rs.getString(17));
                    res.setLockDepth(rs.getString(18));
                    res.setLockTimeout(rs.getLong(19));
                    res.setDescription(rs.getString(20));
                    res.setDataLocationPreference(rs.getString(21));
                    res.setStatus(rs.getString(22));
                    ArrayList<MyInstance> ins = getInstances(res, null);
                    for (MyInstance i : ins) {
                        addFeatures(connection, i, res.getUid());
                    }
                }
            }
        }
    }

    @Override
    public void run() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public LobState getNextState(LobState currentState) throws SQLException {
        ArrayList<LobState> states = new ArrayList<>();
        String rName = currentState.getResourceName();
        if (!rName.endsWith("/")) {
            rName += "/";
        }
        rName = rName.replaceFirst("/lobcder/dav/", "");
        try (Connection connection = getConnection()) {
            LogicalData data = getLogicalDataByPath(Path.path(rName), connection);
            Instance instance = getInstances(data, currentState.getMethod()).get(0);
            double[] features = instance.toDoubleArray();
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT ldataRef, methodName, "
                    + "POW((f1 - ?), 2) + POW((f2 - ?), 2) + POW((f3 - ?), 2) + "
                    + "POW((f4 - ?), 2) + POW((f5 - ?), 2) + POW((f6 - ?), 2) + "
                    + "POW((f7 - ?), 2)+ POW((f8 - ?), 2)+ POW((f9 - ?), 2)+ "
                    + "POW((f10 - ?), 2)+ POW((f11 - ?), 2)+ POW((f12 - ?), 2)+ "
                    + "POW((f13 - ?), 2)+ POW((f14 - ?), 2)+ POW((f15 - ?), 2)+ "
                    + "POW((f16 - ?), 2)"
                    + "AS dist FROM features_table  ORDER BY dist ASC LIMIT ?")) {
                for (int i = 0; i < features.length; i++) {
                    int index = i + 1;
                    preparedStatement.setDouble(index, features[i]);
                }
                preparedStatement.setInt(features.length + 1, k);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    String path = getPathforLogicalData(getLogicalDataByUid(rs.getLong(1), connection), connection);
                    LobState state = new LobState(Method.valueOf(rs.getString(2)), path);
                    log.log(Level.INFO, "State: {0}", state.getID());
                }
            }
        }
        return null;
    }

    private void addFeatures(Connection connection, MyInstance inst, Long uid) throws SQLException {
        boolean exists = false;
        try (PreparedStatement ps = connection.prepareStatement("select uid "
                + "from features_table WHERE methodName = ? AND ldataRef = ?")) {
            ps.setString(1, inst.getMethod().code);
            ps.setLong(2, uid);
            ResultSet rs = ps.executeQuery();
            exists = rs.next();
        }
        if (!exists) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO "
                    + "features_table (methodName, ldataRef, f1, f2, f3, f4, f5, "
                    + "f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16) "
                    + "VALUES (?, ?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, inst.getMethod().name());
                ps.setLong(2, uid);
                double[] features = inst.toDoubleArray();
                for (int i = 0; i < features.length; i++) {
                    int index = i + 3;
                    ps.setDouble(index, features[i]);
                }
                ps.executeUpdate();
                connection.commit();
            }
        }
    }

    private ArrayList<MyInstance> getInstances(LogicalData n, Method method) {
        ArrayList<MyInstance> inst = new ArrayList<>();

        if (method == null) {
            for (Request.Method m : Request.Method.values()) {
                int index = 0;
                MyInstance instance = new MyInstance(metdataAttributes.size());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getUid());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), m.code);
                instance.setMethod(m);
                String att = n.getChecksum();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                att = n.getContentTypesAsString();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getCreateDate());
                att = n.getDataLocationPreference();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                att = n.getDescription();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLastValidationDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLength());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getModifiedDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getParentRef());
                att = n.getStatus();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getType());

                instance.setValue((Attribute) metdataAttributes.elementAt(index++), String.valueOf(n.getSupervised()));
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getOwner());
                inst.add(instance);
            }
        } else {
            int index = 0;
            MyInstance instance = new MyInstance(metdataAttributes.size());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getUid());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), method.code);
            instance.setMethod(method);
            String att = n.getChecksum();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            att = n.getContentTypesAsString();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getCreateDate());
            att = n.getDataLocationPreference();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            att = n.getDescription();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLastValidationDate());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLength());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getModifiedDate());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getParentRef());
            att = n.getStatus();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getType());

            instance.setValue((Attribute) metdataAttributes.elementAt(index++), String.valueOf(n.getSupervised()));
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getOwner());
            inst.add(instance);
        }
        return inst;
    }

    private LogicalData getLogicalDataByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException {
        LogicalData res = null;
        if (res != null) {
            return res;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            long parent = 1;
            String parts[] = logicalResourceName.getParts();
            if (parts.length == 0) {
                parts = new String[]{""};
            }
            for (int i = 0; i != parts.length; ++i) {
                String p = parts[i];
                if (i == (parts.length - 1)) {
                    try (PreparedStatement preparedStatement1 = connection.prepareStatement(
                            "SELECT uid, ownerId, datatype, createDate, modifiedDate, ldLength, "
                            + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                            + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                            + "description, locationPreference, status "
                            + "FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
                        preparedStatement1.setLong(1, parent);
                        preparedStatement1.setString(2, p);
                        ResultSet rs = preparedStatement1.executeQuery();
                        if (rs.next()) {
                            res = new LogicalData();
                            res.setUid(rs.getLong(1));
                            res.setParentRef(parent);
                            res.setOwner(rs.getString(2));
                            res.setType(rs.getString(3));
                            res.setName(p);
                            res.setCreateDate(rs.getTimestamp(4).getTime());
                            res.setModifiedDate(rs.getTimestamp(5).getTime());
                            res.setLength(rs.getLong(6));
                            res.setContentTypesAsString(rs.getString(7));
                            res.setPdriGroupId(rs.getLong(8));
                            res.setSupervised(rs.getBoolean(9));
                            res.setChecksum(rs.getString(10));
                            res.setLastValidationDate(rs.getLong(11));
                            res.setLockTokenID(rs.getString(12));
                            res.setLockScope(rs.getString(13));
                            res.setLockType(rs.getString(14));
                            res.setLockedByUser(rs.getString(15));
                            res.setLockDepth(rs.getString(16));
                            res.setLockTimeout(rs.getLong(17));
                            res.setDescription(rs.getString(18));
                            res.setDataLocationPreference(rs.getString(19));
                            res.setStatus(rs.getString(20));
                            return res;
                        } else {
                            return null;
                        }
                    }
                } else {
                    preparedStatement.setLong(1, parent);
                    preparedStatement.setString(2, p);
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        parent = rs.getLong(1);
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    private LogicalData getLogicalDataByUid(Long UID, @Nonnull Connection connection) throws SQLException {
        LogicalData res = null;
        if (res != null) {
            return res;
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT parentRef, ownerId, datatype, ldName, "
                + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                + "FROM ldata_table WHERE ldata_table.uid = ?")) {
            ps.setLong(1, UID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                res = new LogicalData();
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

    private String getPathforLogicalData(LogicalData ld, @Nonnull Connection connection) throws SQLException {
        String res = null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ldName, parentRef FROM ldata_table WHERE uid = ?")) {
            PathInfo pi = new PathInfo(ld.getName(), ld.getParentRef());
            List<PathInfo> pil = new ArrayList<>();
            getPathforLogicalData(pi, pil, ps);
            res = "";
            Collections.reverse(pil);
            for (PathInfo pi1 : pil) {
                res = res + "/" + pi1.getName();
            }
            return res;
        }
    }

    private void getPathforLogicalData(PathInfo pi, List<PathInfo> pil, PreparedStatement ps) throws SQLException {
        pil.add(pi);
        if (pi != null && pi.getParentRef() != 1) {
            ps.setLong(1, pi.getParentRef());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pi = new PathInfo(rs.getString(1), rs.getLong(2));
                    getPathforLogicalData(pi, pil, ps);
                }
            }
        }
    }

    private class MyInstance extends Instance {

        private Method method;

        private MyInstance(int size) {
            super(size);
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Method getMethod() {
            return this.method;
        }
    }

    @Data
    @AllArgsConstructor
    public class PathInfo {

        private String name;
        private Long parentRef;
    }
}
