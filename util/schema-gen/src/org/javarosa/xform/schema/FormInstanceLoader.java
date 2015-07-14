package org.javarosa.xform.schema;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;

import java.io.InputStream;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FormInstanceLoader {
    private static InstanceInitializationFactory iif;


    public static FormDef loadInstance(InputStream formInput, InputStream instanceInput) throws Exception {
        FormDef formDef;
        FormInstance savedModel;
        FormEntryModel entryModel;

        formDef = XFormUtils.getFormFromInputStream(formInput);

        savedModel = XFormParser.restoreDataModel(instanceInput, null);

        // get the root of the saved and template instances
        TreeElement savedRoot = savedModel.getRoot();
        TreeElement templateRoot = formDef.getInstance().getRoot().deepCopy(true);

        entryModel = new FormEntryModel(formDef);

        // weak check for matching forms
        if (!savedRoot.getName().equals(templateRoot.getName()) || savedRoot.getMult() != 0) {
            System.out.println("Saved form instance does not match template form definition");
            return null;
        } else {
            // populate the data model
            TreeReference tr = TreeReference.rootRef();
            tr.add(templateRoot.getName(), TreeReference.INDEX_UNBOUND);
            templateRoot.populate(savedRoot, formDef);

            // populated model to current form
            formDef.getInstance().setRoot(templateRoot);

            if (entryModel.getLanguages() != null) {
                formDef.localeChanged(entryModel.getLanguage(), formDef.getLocalizer());
            }
        }

        formDef.initialize(false, iif);
        return formDef;
    }
}
