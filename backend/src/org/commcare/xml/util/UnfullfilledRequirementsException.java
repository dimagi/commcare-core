/**
 * 
 */
package org.commcare.xml.util;

/**
 * @author ctsims
 *
 */
public class UnfullfilledRequirementsException extends Exception {
	
	/** This requirement may be ignorable, but the user should be prompted **/
	public static int SEVERITY_PROMPT = 1;
	/** Something is missing from the environment, but it should be able to be provided **/
	public static int SEVERITY_ENVIRONMENT = 2;
	/** It isn't clear how to correct the problem, and probably is a programmer error **/
	public static int SEVERITY_UNKOWN = 4;
	
	public static int REQUIREMENT_MAJOR_APP_VERSION = 1;
	public static int REQUIREMENT_MINOR_APP_VERSION = 2;
	
	private int severity;
	private int requirement;

	public String required;
	public String available;
	public boolean majorIsProblem = false;
	
	public UnfullfilledRequirementsException(String message, int severity) {
		super(message);
		this.severity = severity;	
	}
	
	public UnfullfilledRequirementsException(String message, int severity, int requirement) {
		super(message);
		this.severity = severity;
		this.requirement = requirement;
	}
	
	public UnfullfilledRequirementsException(String message, int severity, int maR, int miR, int maA, int miA, boolean majorIsProblem){
		super(message);
		this.severity = severity;
		required=maR+"."+miR;
		available=maA+"."+miA;
		this.majorIsProblem=majorIsProblem;
	}
	
	public UnfullfilledRequirementsException(String message, int severity, int requirement, int maR, int miR, int maA, int miA, boolean majorIsProblem){
		super(message);
		this.severity = severity;
		this.requirement = requirement;
		required=maR+"."+miR;
		available=maA+"."+miA;
		this.majorIsProblem=majorIsProblem;
	}
	
	public int getSeverity() {
		return severity;
	}
	
	public int getRequirementCode() {
		return requirement;
	}
	
	public String getAvailableVesionString(){
		return available;
	}
	
	public String getRequiredVersionString(){
		return required;
	}
	
	
}
