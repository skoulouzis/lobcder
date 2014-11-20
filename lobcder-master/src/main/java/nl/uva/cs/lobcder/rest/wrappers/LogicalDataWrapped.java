package nl.uva.cs.lobcder.rest.wrappers;

import lombok.Data;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRIDescr;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * A wrapper for the logical data. It incudes all resource's properties
 *
 * @author dvasunin
 */
@XmlRootElement
@Data
public class LogicalDataWrapped {

    private String globalID;
    private LogicalData logicalData;
    private String path;
    private Permissions permissions;
    private List<PDRIDescr> pdriList = null;
}