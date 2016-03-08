/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest.wrappers;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
public class ReservationInfo {

    private String communicationID;
    private int storageHostIndex;
    private String storageHost;
    private String workerDataAccessURL;
    private String hostName;
}
