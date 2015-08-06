package org.javarosa.test_utils;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Commonly used form loading utilities for testing.
 *
 * @author Phillip Mates
 */

public class FormLoadingUtils {

    /**
     * Load and parse an XML file into a form instance.
     *
     * @param formPath form resource filename that will be loaded at compile
     *                 time.
     */
    public static FormInstance loadFormInstance(String formPath) throws InvalidStructureException, IOException {
        // read in xml
        InputStream is = System.class.getResourceAsStream(formPath);
        TreeElementParser parser = new TreeElementParser(ElementParser.instantiateParser(is), 0, "instance");

        // turn parsed xml into a form instance
        TreeElement root = null;
        try {
            root = parser.parse();
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage());
        } catch (UnfullfilledRequirementsException e) {
            // TODO: this error will eventually be removed from the base abstract
            // class, and then can be removed here
            throw new IOException(e.getMessage());
        }

        return new FormInstance(root, null);
    }
}
