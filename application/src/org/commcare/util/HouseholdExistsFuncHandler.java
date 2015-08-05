package org.commcare.util;

import org.commcare.cases.model.Case;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;

import java.util.Vector;

public class HouseholdExistsFuncHandler implements IFunctionHandler {

    public String getName() {
        return "household-exists";
    }

    public Vector getPrototypes() {
        Vector p = new Vector();
        p.addElement(new Class[] {String.class});
        return p;
    }

    public Object eval(Object[] args, EvaluationContext ec) {
        boolean exists = false;

//        if(args[0] instanceof Double) {
//            Double d = (Double)args[0];
//            if (d.doubleValue() >= 0 && d.doubleValue() <= 10000) {
//                exists = validateHouseholdID(String.valueOf(d.intValue()));
//            }
//        }
//        if(args[0] instanceof String) {
            exists = validateHouseholdID((String)args[0]);
        //}

        return new Boolean(exists);
    }

    public boolean validateHouseholdID (String hhid) {
        IStorageUtilityIndexed cases = (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY);
        Vector extidIDs = cases.getIDsForValue("external-id", hhid);
        Vector typeIDs = cases.getIDsForValue("case-type", "cc_case_house_visit");

        boolean found = false;
        //for each case with same ext id
        for (int i = 0; i < extidIDs.size(); i++) {
            int caseID = ((Integer)extidIDs.elementAt(i)).intValue();
            //if one of the cases is of the house_visit type, AND it's open, we have a dup
            if (typeIDs.contains(new Integer(caseID))) {
                Case c = (Case)cases.read(caseID);
                if (!c.isClosed()) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean rawArgs() {
        return false;
    }

    public boolean realTime() {
        return true;
    }

}
