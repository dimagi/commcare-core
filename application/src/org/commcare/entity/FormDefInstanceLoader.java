/**
 * 
 */
package org.commcare.entity;

import java.util.Hashtable;

import org.commcare.suite.model.Filter;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.EntityFilter;

/**
 * @author ctsims
 *
 */
public class FormDefInstanceLoader extends FormInstanceLoader<FormDef> {
	FormDef c;
	Hashtable metadata;
	
	public FormDefInstanceLoader(Hashtable<String,String> references) {
		super(references);
	}
	public void prepare(FormDef c) {
		this.c = c;
		metadata = c.getMetaData();
	}
	
	protected Object resolveReferenceData(String reference, String key) {
		if(references.get(reference).toLowerCase().equals("form")) {
			return (String)metadata.get(key);
		} else {
			return null;
		}
	}
	
	protected EntityFilter<FormDef> resolveFilter(final Filter filter, final FormInstance template) {
		
		return new EntityFilter<FormDef> () {
			
			public int preFilter(int id, Hashtable<String, Object> metaData) {
				return EntityFilter.PREFILTER_FILTER;
			}

			public boolean matches(FormDef c) {
				return true;
			}
		};
	}

}
