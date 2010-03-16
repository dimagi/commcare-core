/**
 * 
 */
package org.commcare.entity;

import java.util.Hashtable;

import org.javarosa.cases.model.Case;
import org.javarosa.cases.util.CasePreloadHandler;

/**
 * @author ctsims
 *
 */
public class CaseInstanceLoader extends FormInstanceLoader<Case> {
	Case c;
	CasePreloadHandler p;
	
	public CaseInstanceLoader(Hashtable<String,String> references) {
		super(references);
	}
	public void prepare(Case c) {
		this.c = c;
		p = new CasePreloadHandler(c);
	}
	
	protected Object resolveReferenceData(String reference, String key) {
		if(references.get(reference).toLowerCase().equals("case")) {
			return p.handlePreload(key);
		} else {
			return null;
		}
	}

}
