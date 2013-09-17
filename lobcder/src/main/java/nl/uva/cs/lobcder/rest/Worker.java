/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 *
 * @author alogo
 */
@XmlRootElement
@Data
public class Worker {

    private String hostName;
    private String status;
}
