
package nl.uva.cs.lobcder.rest.wrappers;

public class AttachmentPoint{
   	private String errorStatus;
   	private Number port;
   	private String switchDPID;

 	public String getErrorStatus(){
		return this.errorStatus;
	}
	public void setErrorStatus(String errorStatus){
		this.errorStatus = errorStatus;
	}
 	public Number getPort(){
		return this.port;
	}
	public void setPort(Number port){
		this.port = port;
	}
 	public String getSwitchDPID(){
		return this.switchDPID;
	}
	public void setSwitchDPID(String switchDPID){
		this.switchDPID = switchDPID;
	}
}
