package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.PropPatchableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.http.values.ValueAndType;
import com.bradmcevoy.http.webdav.PropPatchHandler.Field;
import com.bradmcevoy.http.webdav.PropPatchHandler.Fields;
import com.bradmcevoy.http.webdav.PropPatchRequestParser.ParseResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

/**
 * Performs PROPPATCH updates on resources implementing PropPatchable
 *
 * @author brad
 */
public class PropPatchableSetter implements PropPatchSetter {

    private final Helper helper;

    PropPatchableSetter( Helper helper ) {
        this.helper = helper;
    }

    public PropPatchableSetter() {
        this.helper = new Helper();
    }

    public boolean supports(Resource r) {
        return (r instanceof PropPatchableResource);
    }

    public PropFindResponse setProperties( String href, ParseResult parseResult, Resource r ) {
        PropPatchableResource ppr = (PropPatchableResource) r;
        Fields fields = helper.buildFields(parseResult);
        ppr.setProperties( fields );
        List<PropFindResponse> list = helper.buildResult(href, parseResult);
        return list.get(0);
    }

    class Helper {

        private Fields buildFields( ParseResult parseResult ) {
            Fields fields = new Fields();
            List<Field> removeFields = fields.removeFields;
            for( QName p : parseResult.getFieldsToRemove()) {
                removeFields.add( new Field( p.getLocalPart()));
            }
            List<PropPatchHandler.SetField> setFields = fields.setFields;
            for( Entry<QName, String> entry : parseResult.getFieldsToSet().entrySet()) {
                setFields.add( new PropPatchHandler.SetField( entry.getKey().getLocalPart(), entry.getValue()));
            }
            return fields;
        }

        private List<PropFindResponse> buildResult( String href, ParseResult parseResult ) {
            LinkedHashMap<QName,ValueAndType> knownProps = new LinkedHashMap<QName, ValueAndType>();
            for( QName p : parseResult.getFieldsToRemove()) {
                knownProps.put( p, null);
            }
            for( Entry<QName, String> entry : parseResult.getFieldsToSet().entrySet()) {
                knownProps.put( entry.getKey(), null);
            }
            
            PropFindResponse resp = new PropFindResponse( href, knownProps, new HashMap<Status, List<PropFindResponse.NameAndError>>());
            List<PropFindResponse> responses = new ArrayList<PropFindResponse>();
            responses.add( resp );
            return responses;
        }

    }

}
