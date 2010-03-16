/**
 * 
 */
package org.commcare.entity;

import java.util.Hashtable;

import org.commcare.suite.model.Filter;
import org.javarosa.cases.model.Case;
import org.javarosa.cases.util.CasePreloadHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.EntityFilter;

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
	
	protected EntityFilter<Case> resolveFilter(final Filter filter, final FormInstance template) {
		return new EntityFilter<Case> () {
			public boolean matches(Case c) {
				if(filter.isEmpty()) {
					return !c.isClosed();
				} else {
					return true;
				}
			}
		};
	}

}
