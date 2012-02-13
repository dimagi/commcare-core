/**
 * 
 */
package org.commcare.util;



import java.util.Date;
import java.util.Vector;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ConcreteTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.user.model.User;

/**
 * @author ctsims
 *
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {
	CommCareSessionController session;
	CaseInstanceTreeElement casebase;
	
	public CommCareInstanceInitializer(){ 
		this(null);
	}
	public CommCareInstanceInitializer(CommCareSessionController session) {
		this.session = session;
	}
	
	public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
		String ref = instance.getReference();
		
		//TODO: Clayton should feel bad about all of this. Man is it terrible
		if(ref.indexOf("casedb") != -1) {
			Vector<String> data = DateUtils.split(ref, "/", true);
			if(ref.indexOf("report") != -1) {
				ConcreteTreeElement base = new ConcreteTreeElement("device_report");
				base.setNamespace("http://code.javarosa.org/devicereport");
				ConcreteTreeElement logsr = new ConcreteTreeElement("log_subreport");
				
				ConcreteTreeElement log = new ConcreteTreeElement("log");
				log.setAttribute(null, "date", DateUtils.formatDate(new Date(), DateUtils.FORMAT_ISO8601));
				
				ConcreteTreeElement type = new ConcreteTreeElement("type");
				type.setValue(new StringData("casedb_dump"));
				
				ConcreteTreeElement msg = new ConcreteTreeElement("msg");
				
				
				CaseInstanceTreeElement reportBase = new CaseInstanceTreeElement(msg, (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY), true);
				
				//jr: | instance | casedb | report | (sync | state)?
				if(data.size() == 6) {
					reportBase.setState(data.elementAt(4), data.elementAt(5));
				}
				msg.addChild(reportBase);
				log.addChild(type);
				log.addChild(msg);
				logsr.addChild(log);
				base.addChild(logsr);
				
				return base;
			}
			if(casebase == null) {
				casebase =  new CaseInstanceTreeElement(instance.getBase(), (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY), false);
			} else {
				casebase.rebase(instance.getBase());
			}
			return casebase;
		}
		if(instance.getReference().indexOf("fixture") != -1) {
			String userId = "";
			User u = CommCareContext._().getUser();
			if(u != null) {
				userId = u.getUniqueId();
			}
			FormInstance fixture = CommCareUtil.loadFixtureForUser(ref.substring(ref.lastIndexOf('/') + 1, ref.length()), userId);
			if(fixture == null) {
				throw new RuntimeException("Could not find an appropriate fixture for src: " + ref);
			}
			
			//FormInstance fixture = (FormInstance)storage.getRecordForValue(FormInstance.META_ID, refId);
			TreeElement root = fixture.getRoot();
			root.setParent(instance.getBase());
			return root;
		}
		if(instance.getReference().indexOf("session") != -1) {
			TreeElement root = session.getSessionInstance().getRoot();
			root.setParent(instance.getBase());
			return root;
		}
		return null;
	}
}
