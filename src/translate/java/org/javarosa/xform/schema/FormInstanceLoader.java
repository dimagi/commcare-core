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

        try {
            formDef = XFormUtils.getFormFromInputStream(formInput);
            return loadInstance(formDef, instanceInput);
        } catch (XFormParseException e) {
            throw new IOException(e.getMessage());
        }
    }

    public static FormDef loadInstance(FormDef formDef,
                                       InputStream instanceInput) throws IOException {
        FormInstance formInstance = XFormParser.restoreDataModel(instanceInput, null);
        try {
            return XFormParser.loadXmlInstance(formDef, formInstance);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
