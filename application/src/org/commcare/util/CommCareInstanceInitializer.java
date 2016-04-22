/**
 *
 */
package org.commcare.util;


import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.ledger.instance.LedgerInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionInstanceBuilder;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ConcreteTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.Interner;
import org.javarosa.core.model.User;
import org.commcare.cases.instance.CaseDataInstance;

import java.util.Date;
import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {
    CommCareSession session;
    CaseInstanceTreeElement casebase;
    LedgerInstanceTreeElement ledgerBase;
    Interner<String> stringCache;

    public CommCareInstanceInitializer(Interner<String> stringCache){
        this(null, null);
    }
    public CommCareInstanceInitializer(Interner<String> stringCache, CommCareSession session) {
        this.session = session;
    }

    @Override
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        if (CaseInstanceTreeElement.MODEL_NAME.equals(instance.getInstanceId())) {
            return new CaseDataInstance(instance);
        } else {
            return instance;
        }
    }

    /**
     *
     * This method's behavior depends on the following configuration properties:
     *
     * @see DeviceID - Needs to Exist
     * @see app-version - Needs to exist.
     */
    public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();

        if(ref.indexOf(LedgerInstanceTreeElement.MODEL_NAME) != -1) {

            //See if we already have a ledger model loaded
            if(ledgerBase == null) {
                //If not create one and attach our cache
                ledgerBase =  new LedgerInstanceTreeElement(instance.getBase(), (IStorageUtilityIndexed)StorageManager.getStorage(Ledger.STORAGE_KEY));
                if(stringCache != null ) {
                    ledgerBase.attachStringCache(stringCache);
                }
            } else {
                //re-use the existing model if it exists.
                ledgerBase.rebase(instance.getBase());
            }
            return ledgerBase;
        }
        //TODO: Clayton should feel bad about all of this. Man is it terrible
        else if(ref.indexOf(CaseInstanceTreeElement.MODEL_NAME) != -1) {
            Vector<String> data = DateUtils.split(ref, "/", true);
            if(ref.indexOf("report") != -1) {
                ConcreteTreeElement base = new ConcreteTreeElement("device_report");
                base.setNamespace("http://code.javarosa.org/devicereport");


                String deviceId = PropertyManager._().getSingularProperty(JavaRosaPropertyRules.DEVICE_ID_PROPERTY);
                String reportDate = DateUtils.formatDate(new Date(), DateUtils.FORMAT_HUMAN_READABLE_SHORT);
                String appVersion = PropertyManager._().getSingularProperty("app-version");

                ConcreteTreeElement did = new ConcreteTreeElement("device_id");
                did.setValue(new StringData(deviceId));
                base.addChild(did);

                ConcreteTreeElement rd = new ConcreteTreeElement("report_date");
                rd.setValue(new StringData(reportDate));
                base.addChild(rd);

                ConcreteTreeElement ap = new ConcreteTreeElement("app_version");
                ap.setValue(new StringData(appVersion));
                base.addChild(ap);

                ConcreteTreeElement logsr = new ConcreteTreeElement("log_subreport");

                ConcreteTreeElement log = new ConcreteTreeElement("log");
                log.setAttribute(null, "date", DateUtils.formatDate(new Date(), DateUtils.FORMAT_ISO8601));

                ConcreteTreeElement type = new ConcreteTreeElement("type");
                type.setValue(new StringData("casedb_dump"));

                ConcreteTreeElement msg = new ConcreteTreeElement("msg");


                CaseInstanceTreeElement reportBase = new CaseInstanceTreeElement(msg, (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY), true);
                if(stringCache != null ) {
                    reportBase.attachStringCache(stringCache);
                }

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
                if(stringCache != null ) {
                    casebase.attachStringCache(stringCache);
                }
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
            FormInstance sessionInstance = SessionInstanceBuilder.getSessionInstance(session.getFrame(), PropertyManager._().getSingularProperty(JavaRosaPropertyRules.DEVICE_ID_PROPERTY),
                    PropertyManager._().getSingularProperty(CommCareProperties.COMMCARE_VERSION),
                    CommCareContext._().getUser().getUsername(),
                    CommCareContext._().getUser().getUniqueId(),
                    CommCareContext._().getUser().getProperties());


            TreeElement root = sessionInstance.getRoot();
            root.setParent(instance.getBase());
            return root;
        }
        return null;
    }
}
