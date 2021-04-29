package org.commcare.util;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.TreeReference;

import io.reactivex.annotations.Nullable;

/**
 * @author $|-|!˅@M
 */
public class FormMetaIndicatorUtil {

    public static final String CASE_NAME_DESCRIPTOR = "Pragma-Case-Name";

    @Nullable
    public static String getPragma(String key, FormDef formDef, TreeReference contextRef) {
        String value = formDef.getLocalizer().getText(key);
        if(value != null) {
            return formDef.fillTemplateString(value, contextRef);
        }
        return null;
    }

}
