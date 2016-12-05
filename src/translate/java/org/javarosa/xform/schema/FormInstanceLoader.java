package org.javarosa.xform.schema;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Logic for loading a particular xml form instance into a FormDef.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FormInstanceLoader {

    /**
     * Build a form definition and load a particular form instance into it.
     * The FormDef object returned isn't initialized, and hence will not have
     * 'instance(...)' data set.
     *
     * @param formInput     XML stream of the form definition
     * @param instanceInput XML stream of an instance of the form
     * @return The form definition with the given instance loaded. Returns null
     * if the instance doesn't match the form provided.
     * @throws IOException thrown when XML input streams aren't successfully
     *                     parsed
     */
    public static FormDef loadInstance(InputStream formInput,
                                       InputStream instanceInput)
            throws IOException {
        FormDef formDef;
        FormInstance savedModel;

        try {
            formDef = XFormUtils.getFormFromInputStream(formInput);
        } catch (XFormParseException e) {
            throw new IOException(e.getMessage());
        }

        savedModel = XFormParser.restoreDataModel(instanceInput, null);

        // get the root of the saved and template instances
        TreeElement savedRoot = savedModel.getRoot();
        TreeElement templateRoot =
                formDef.getInstance().getRoot().deepCopy(true);

        // weak check for matching forms
        if (!savedRoot.getName().equals(templateRoot.getName()) ||
                savedRoot.getMult() != 0) {
            System.out.println("Instance and template form definition don't match");
            return null;
        } else {
            // populate the data model
            TreeReference tr = TreeReference.rootRef();
            tr.add(templateRoot.getName(), TreeReference.INDEX_UNBOUND);
            templateRoot.populate(savedRoot);

            // populated model to current form
            formDef.getInstance().setRoot(templateRoot);
        }

        return formDef;
    }
}
