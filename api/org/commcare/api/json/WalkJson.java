package org.commcare.api.json;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONArray;

/**
 * Created by willpride on 12/8/15.
 */
public class WalkJson {
    public static String walkToString(FormEntryModel fem, FormEntryController fec){
        try {
            JSONArray ret = new JSONArray();
            FormIndex formIndex = FormIndex.createBeginningOfFormIndex();
            Walker walker = new Walker(ret, formIndex, fec, fem);
            walker.walk();
            return ret.toString();
        } catch(Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static JSONArray walkToJSON(FormEntryModel fem, FormEntryController fec){
        try {
            JSONArray ret = new JSONArray();
            FormIndex formIndex = FormIndex.createBeginningOfFormIndex();
            Walker walker = new Walker(ret, formIndex, fec, fem);
            walker.walk();
            return ret;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
