/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest.wrappers;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
@Data
public class IDWrapperList {
     public List<Long> ids;
}
