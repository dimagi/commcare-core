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
		return new EntityFilter<PatientReferral> () {
			public boolean matches(PatientReferral r) {
				if(filter.isEmpty()) {
					return r.isPending();
				} else {
					return true;
				}
			}
		};
	}
}
