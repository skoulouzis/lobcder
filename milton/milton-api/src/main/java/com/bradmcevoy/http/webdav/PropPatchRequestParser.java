package com.bradmcevoy.http.webdav;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 *
 * @author brad
 */
public interface PropPatchRequestParser {

    ParseResult getRequestedFields( InputStream in );

    class ParseResult {
        private final Map<QName,String> fieldsToSet;
        private final Set<QName> fieldsToRemove;

        public ParseResult( Map<QName, String> fieldsToSet, Set<QName> fieldsToRemove ) {
            this.fieldsToSet = fieldsToSet;
            this.fieldsToRemove = fieldsToRemove;
        }

        public Set<QName> getFieldsToRemove() {
            return fieldsToRemove;
        }

        public Map<QName, String> getFieldsToSet() {
            return fieldsToSet;
        }
    }
}
