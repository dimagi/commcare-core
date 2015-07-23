package org.javarosa.test_utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.XPathUnhandledException;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathNumericLiteral;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

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
