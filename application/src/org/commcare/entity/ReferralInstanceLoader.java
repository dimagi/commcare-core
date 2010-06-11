package org.commcare.entity;

import java.util.Hashtable;

import org.commcare.suite.model.Filter;
import org.commcare.util.CommCareUtil;
import org.javarosa.cases.model.Case;
import org.javarosa.cases.util.CasePreloadHandler;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.chsreferral.util.PatientReferralPreloader;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.entity.util.StackedEntityFilter;

public class ReferralInstanceLoader extends FormInstanceLoader<PatientReferral> {

	private PatientReferral r;
	private Case c;
	private PatientReferralPreloader preloader;
	private CasePreloadHandler casePreloader;
	
	public ReferralInstanceLoader(Hashtable<String,String> references) {
		super(references);
	}
	
	public void prepare(PatientReferral r) {
		this.r = r;
		preloader = new PatientReferralPreloader(r);
		this.c = null;
	}
	
	protected Object resolveReferenceData(String reference, String key) {
		String refType = references.get(reference).toLowerCase();
		if(refType.equals("referral")) {
			return preloader.handlePreload(key);
		} else if(refType.equals("case")){
			return getCasePreloader().handlePreload(key);
		}
		return null;
	}
	
	private CasePreloadHandler getCasePreloader() {
		if(c == null) {
			c = CommCareUtil.getCase(r.getLinkedId());
			casePreloader = new CasePreloadHandler(c);
		}
		return casePreloader;
	}
	
	
	protected EntityFilter<PatientReferral> resolveFilter(final Filter filter, final FormInstance template) {
		EntityFilter<PatientReferral> patientRefFilter = new EntityFilter<PatientReferral> () {
			
			public boolean matches(PatientReferral r) {
				
				if(filter.isEmpty()) {
					return r.isPending();
				} else {
					
					//Apparently meta data isn't supported yet, so we have to do this here, too...
					if(filter.paramSet(Filter.REFERRAL_TYPE)) {
						if(!r.getType().equals(filter.getParam(Filter.REFERRAL_TYPE))) {
							return false;
						}
					}
					
					if(!r.isPending()) {
						if(filter.paramSet(Filter.SHOW_RESOLVED)) {
							return new Boolean(true).toString().equals(filter.getParam(Filter.SHOW_RESOLVED));
						} else {
							return false;
						}
					}
					
					//TODO: Only admin or user
					
					return true;
				}
			}

		};
		
		//This filter also wants to filter on a case basis
		if(filter.paramSet(Filter.FILTER_CASE)) {
			//Create the applicable case filter
			final EntityFilter<Case> caseFilter = new CaseInstanceLoader(references).resolveFilter(filter, template);
			
			EntityFilter<PatientReferral> wrapper = new EntityFilter<PatientReferral>() {
				Hashtable<String,Case> cache = new Hashtable<String,Case>(); 
				
				public boolean matches(PatientReferral r) {
					Case c;
					String caseId = r.getLinkedId();
					if(!cache.containsKey(caseId)) {
						c = CommCareUtil.getCase(caseId);
						cache.put(caseId, c);
					} else {
						c = cache.get(caseId);
					}
					
					//Maybe if the case can't be found we should return something automatically?
					
					return caseFilter.matches(c);
				}
			};
			return new StackedEntityFilter<PatientReferral>(patientRefFilter, wrapper);
		}
		return patientRefFilter;
	}
}
