/**
 * 
 */
package org.commcare.entity;

import java.util.Hashtable;

import org.javarosa.cases.model.Case;

/**
 * @author ctsims
 *
 */
public class CaseInstanceLoader extends FormInstanceLoader<Case> {
	Case c;
	
	public CaseInstanceLoader(Hashtable<String,String> references) {
		super(references);
	}
	
	public void prepare(Case c) {
		this.c = c;
	}
	
	protected Object resolveReferenceData(String reference, String key) {
		if(references.get(reference).toLowerCase().equals("case")) {
			return c.getProperty(key);
		} else {
			return null;
		}
	}

}
