package org.commcare.api.json;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONArray;

/**
 * Created by willpride on 12/8/15.
 */
public class WalkJson {
    public static String walkToJson(FormEntryModel fem, FormEntryController fec){
        JSONArray ret = new JSONArray();
        FormIndex formIndex = FormIndex.createBeginningOfFormIndex();
        Walker walker = new Walker(ret, formIndex, fec, fem);
        walker.walk();
        return ret.toString();
    }
}
