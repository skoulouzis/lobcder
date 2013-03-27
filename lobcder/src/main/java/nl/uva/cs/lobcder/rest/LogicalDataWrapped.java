package nl.uva.cs.lobcder.rest;

import lombok.Data;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRIDescr;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * User: dvasunin
 * Date: 19.03.13
 * Time: 16:37
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement
@Data
public class LogicalDataWrapped {
    private LogicalData logicalData;
    private String path;
    private Permissions permissions;
    private List<PDRIDescr> pdriList = null;
}
