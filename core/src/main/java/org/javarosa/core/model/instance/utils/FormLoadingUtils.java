package org.javarosa.core.model.instance.utils;

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
 * Collection of static form loading methods
 *
 * @author Phillip Mates
 */
public class FormLoadingUtils {

    public static FormInstance loadFormInstance(String formFilepath)
            throws InvalidStructureException, IOException {
        TreeElement root = xmlToTreeElement(formFilepath);

        return new FormInstance(root, null);
    }

    public static TreeElement xmlToTreeElement(String xmlFilepath)
            throws InvalidStructureException, IOException {
        InputStream is = System.class.getResourceAsStream(xmlFilepath);
        TreeElementParser parser = new TreeElementParser(ElementParser.instantiateParser(is), 0, "instance");

        try {
            return parser.parse();
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage());
        } catch (UnfullfilledRequirementsException e) {
            throw new IOException(e.getMessage());
        }
    }
}
