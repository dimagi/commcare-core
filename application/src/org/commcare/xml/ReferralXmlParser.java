/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Date;

import org.commcare.data.xml.TransactionParser;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class ReferralXmlParser extends TransactionParser<PatientReferral> {
	String caseId;
	Date created;
	
	IStorageUtilityIndexed storage;

	public ReferralXmlParser(KXmlParser parser, String caseId, Date created) {
		super(parser, "referral", null);
		this.caseId = caseId;
		this.created = created;
	}
	
	public PatientReferral parse() throws InvalidStructureException, IOException, XmlPullParserException {
		this.checkNode("referral");
		
		//parse (with verification) the next tag
		this.nextTag("referral_id");
		String refId = parser.nextText().trim();
		
		//Now look for actions
		while(this.nextTagInBlock("referral")) {
			
			String action = parser.getName().toLowerCase();
			//As a temporary thing.
			Date followup = created;
			
			//this should always happen before the below....
			if(action.equals("followup_date")) {
				String followupDate = parser.nextText();
				followup = DateUtils.parseDate(followupDate);
			}
			
			if(action.equals("open")) {
				this.getNextTagInBlock("open");
				checkNode("referral_types");
				String referralTypes = parser.nextText();
				for(Object s : DateUtils.split(referralTypes, " ", true)) {
					PatientReferral pr = new PatientReferral((String)s, created, refId, caseId, followup);
					commit(pr);
				}
				if(this.nextTagInBlock("open")) {
					throw new InvalidStructureException("Expected </open>, found " + parser.getName(), parser);
				}
			} else if(action.equals("update")) {
				this.getNextTagInBlock("update");
				checkNode("referral_type");
				String refType = parser.nextText().trim();
				PatientReferral r = retrieve(refId, refType);
				if(r == null) {
					//There's no referral to be updated on the system. It is likely that the server
					//is missing data.
					throw new InvalidStructureException("No existing referral for update. Skipping ID: " + refId, parser);
				}
				r.setDateDue(followup);
				if(this.nextTagInBlock("update")) {
					String dateClosed = parser.nextText();
					//TODO: Hmmm, see if we need to do anything here.
					//Date closed = DateUtils.parseDate(dateClosed);
					r.close();
					commit(r);
					
					if(this.nextTagInBlock("update")) {
						throw new InvalidStructureException("Expected </update>, found " + parser.getName(), parser);
					}
				}
			}
		}
		
		//This parser doesn't just deal with one object....
		return null;
	}

	public void commit(PatientReferral parsed) throws IOException {
		try {
			storage().write(parsed);
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new IOException("Storage full while trying to write patient referral!");
		}
	}

	public PatientReferral retrieve(String entityId, String type) {
		IStorageUtilityIndexed storage = (IStorageUtilityIndexed)StorageManager.getStorage(PatientReferral.STORAGE_KEY);
		for(Object i : storage.getIDsForValue("referral-id", entityId)) {
			PatientReferral r = (PatientReferral)storage.read(((Integer)i).intValue());
			if(r.getType().equals(type)) {
				return r;
			}
		}
		return null;
	}
	
	private IStorageUtilityIndexed storage() {
		if(storage == null) {
			storage = (IStorageUtilityIndexed)StorageManager.getStorage(PatientReferral.STORAGE_KEY);
		}
		return storage;
	}
}
