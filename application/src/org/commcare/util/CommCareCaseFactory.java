/**
 * 
 */
package org.commcare.util;

import org.javarosa.cases.model.Case;

/**
 * All of the cases in the CommCare application require a specific set of meta
 * data that is not necessary in other cases. This class servers as a factory
 * that creates CommCare compatible cases.
 * 
 * @author ctsims
 *
 */
public class CommCareCaseFactory {
	private static final String CASE_HOUSEHOLD_ID_KEY = "external-id";
	public static final String CASE_HOUSEHOLD_NAME_KEY = "household_name";
	
	public static String getHouseIdFromCommCareCase(Case c) {
		Object o = c.getProperty(CASE_HOUSEHOLD_ID_KEY);
		if (o instanceof String) {
			return (String)o;
		} else {
			System.out.println("warning: expected case property [household_id] to be a String; got " + (o != null ? o.getClass().getName() : "null"));
			return null;
		}
	}
	
	public static String getHouseNameFromCommCareCase(Case c) {
		String hname = (String)c.getProperty(CASE_HOUSEHOLD_NAME_KEY);
		return (hname != null ? hname : c.getName());
	}
}
