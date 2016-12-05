package org.javarosa.xform.util;

import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.model.FormDef;
import org.javarosa.xform.parse.QuestionExtensionParser;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.parse.XFormParserFactory;
import org.kxml2.kdom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Static Utility methods pertaining to XForms.
 *
 * @author Clayton Sims
 */
public class XFormUtils {
    private static XFormParserFactory _factory = new XFormParserFactory();

    public static FormDef getFormFromResource(String resource) throws XFormParseException {
        InputStream is = System.class.getResourceAsStream(resource);
        if (is == null) {
            System.err.println("Can't find form resource \"" + resource + "\". Is it in the JAR?");
            return null;
        }

        return getFormFromInputStream(is);
    }


    public static FormDef getFormRaw(InputStreamReader isr) throws XFormParseException, IOException {
        return _factory.getXFormParser(isr).parse();
    }

    public static FormDef getFormFromInputStream(InputStream is,
                                                 Vector<QuestionExtensionParser> extensionParsers)
            throws XFormParseException {
        InputStreamReader isr;

        //Buffer the incoming data, since it's coming from disk.
        is = new BufferedInputStream(is);

        try {
            isr = new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            System.out.println("UTF 8 encoding unavailable, trying default encoding");
            isr = new InputStreamReader(is);
        }

        try {
            try {
                XFormParser parser = _factory.getXFormParser(isr);
                for (QuestionExtensionParser p : extensionParsers) {
                    parser.registerExtensionParser(p);
                }
                return parser.parse();
                //TODO: Keep removing these, shouldn't be swallowing them
            } catch (IOException e) {
                throw new XFormParseException("IO Exception during parse! " + e.getMessage());
            }
        } finally {
            try {
                isr.close();
            } catch (IOException e) {
                System.err.println("IO Exception while closing stream.");
                e.printStackTrace();
            }
        }
    }

    public static FormDef getFormFromInputStream(InputStream is) throws XFormParseException {
        InputStreamReader isr;
        
        //Buffer the incoming data, since it's coming from disk.
        is = new BufferedInputStream(is);
        
        try {
            isr = new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            System.out.println("UTF 8 encoding unavailable, trying default encoding");
            isr = new InputStreamReader(is);
        }

        try {
            try {
                return _factory.getXFormParser(isr).parse();
                //TODO: Keep removing these, shouldn't be swallowing them
            } catch (IOException e) {
                throw new XFormParseException("IO Exception during parse! " + e.getMessage());
            }
        } finally {
            try {
                isr.close();
            } catch (IOException e) {
                System.err.println("IO Exception while closing stream.");
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------------
    // Attribute parsing validation functions
    // -------------------------------------------------

    /**
     * Get the list of attributes in an element
     */
    public static Vector<String> getAttributeList(Element e) {
        Vector<String> atts = new Vector<>();

        for (int i = 0; i < e.getAttributeCount(); i++) {
            atts.addElement(e.getAttributeName(i));
        }

        return atts;
    }

    /**
     * @return Vector of attributes from 'e' that aren't in 'usedAtts'
     */
    public static Vector<String> getUnusedAttributes(Element e, Vector<String> usedAtts) {
        Vector<String> unusedAtts = getAttributeList(e);
        for (int i = 0; i < usedAtts.size(); i++) {
            if (unusedAtts.contains(usedAtts.elementAt(i))) {
                unusedAtts.removeElement(usedAtts.elementAt(i));
            }
        }
        return unusedAtts;
    }

    /**
     * @return String warning about which attributes from 'e' aren't in 'usedAtts'
     */
    public static String unusedAttWarning(Element e, Vector<String> usedAtts) {
        String warning = "";
        Vector<String> unusedAtts = getUnusedAttributes(e, usedAtts);

        warning += unusedAtts.size() + " unrecognized attributes found in Element [" +
                e.getName() + "] and will be ignored: ";
        warning += "[";
        for (int i = 0; i < unusedAtts.size(); i++) {
            warning += unusedAtts.elementAt(i);
            if (i != unusedAtts.size() - 1) {
                warning += ",";
            }
        }
        warning += "] ";

        return warning;
    }

    /**
     * @return boolean representing whether there are any attributes in 'e' not
     * in 'usedAtts'
     */
    public static boolean showUnusedAttributeWarning(Element e, Vector usedAtts) {
        return getUnusedAttributes(e, usedAtts).size() > 0;
    }

    /**
     * Is this element an Output tag?
     */
    public static boolean isOutput(Element e) {
        return e.getName().toLowerCase().equals("output");
    }
}
