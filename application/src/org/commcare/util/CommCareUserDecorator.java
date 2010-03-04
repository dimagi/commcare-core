/**
 * 
 */
package org.commcare.util;

import org.javarosa.user.utility.IUserDecorator;

/**
 * @author Clayton Sims
 * @date Mar 3, 2009 
 *
 */
public class CommCareUserDecorator implements IUserDecorator {
	
	public static final String REGION = "region";
	public static final String DISTRICT = "district";
	public static final String WARD = "ward";
	public static final String SUPERVISOR_ID = "sid";
	public static final String HBCP_ID = "hcbpid";
	
	/* (non-Javadoc)
	 * @see org.javarosa.user.utility.IUserDecorator#getHumanName(java.lang.String)
	 */
	public String getHumanName(String property) {
		if(property.equals(REGION)) {
			return "Region";
		} else if(property.equals(DISTRICT)) {
			return "District";
		}else if(property.equals(WARD)) {
			return "Ward";
		} else if(property.equals(SUPERVISOR_ID)) {
			return "Supervisor ID";
		} else if(property.equals(HBCP_ID)) {
			return "HBCP id";
		}
		return property;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.user.utility.IUserDecorator#getPertinentProperties()
	 */
	public String[] getPertinentProperties() {
		return new String[] {REGION,DISTRICT, WARD, SUPERVISOR_ID, HBCP_ID};
	}

}
