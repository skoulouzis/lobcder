/*
 * Copyright 2014 S. Koulouzis.
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
package nl.uva.cs.lobcder.predictors;

import io.milton.http.Request.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author S. Koulouzis
 */
public class RandomPredictor extends DBMapPredictor {

    public RandomPredictor() throws NamingException {
    }

    @Override
    public LobState getNextState(LobState currentState) {
        try {
            String resource = getRandomResource();
            Method method = getRandomMethod();
            return new LobState(method, resource);
        } catch (SQLException ex) {
            Logger.getLogger(RandomPredictor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String getRandomResource() throws SQLException {
        try (Connection connection = getConnection()) {
            LogicalData ldata = getRandomLogicalData(connection);
            return getPathforLogicalData(ldata, connection);
        }
    }

    private Method getRandomMethod() {
        int index = new Random().nextInt(Method.values().length);
        return Method.values()[index];
    }

    private LogicalData getRandomLogicalData(@Nonnull Connection connection) throws SQLException {
        LogicalData res = null;
        if (res != null) {
            return res;
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT uid,parentRef, ownerId, datatype, ldName, "
                + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                + "FROM ldata_table ORDER BY RAND() LIMIT 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                res = new LogicalData();
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
                return res;
            } else {
                return null;
            }
        }
    }
}
