/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.xform.util;

import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xform.parse.IXFormParserFactory;
import org.javarosa.xform.parse.QuestionExtensionParser;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.parse.XFormParserFactory;
import org.kxml2.kdom.Element;

import java.io.DataInputStream;
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
    private static IXFormParserFactory _factory = new XFormParserFactory();

    public static IXFormParserFactory setXFormParserFactory(IXFormParserFactory factory) {
        IXFormParserFactory oldFactory = _factory;
        _factory = factory;
        return oldFactory;
    }

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

    /*
     * This method throws XFormParseException when the form has errors.
     */
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

    public static FormDef getFormFromSerializedResource(String resource) throws XFormParseException {
        FormDef returnForm = null;
        InputStream is = System.class.getResourceAsStream(resource);
        try {
            if (is != null) {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
                returnForm = (FormDef)ExtUtil.read(dis, FormDef.class);
                dis.close();
                is.close();
            } else {
                //#if debug.output==verbose
                System.out.println("ResourceStream NULL");
                //#endif
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DeserializationException e) {
            e.printStackTrace();
        }
        return returnForm;
    }


    // -------------------------------------------------
    // Attribute parsing validation functions
    // -------------------------------------------------

    /**
     * Get the list of attributes in an element
     *
     * @param e Element that potentially has attributes in it
     * @return Vector (of String) attributes inside of element
     */
    public static Vector getAttributeList(Element e) {
        Vector atts = new Vector();

        for (int i = 0; i < e.getAttributeCount(); i++) {
            atts.addElement(e.getAttributeName(i));
        }

        return atts;
    }

    /**
     * @param e        an Element with attributes
     * @param usedAtts Vector (of String) attributes
     * @return Vector (of String) attributes from 'e' that aren't in 'usedAtts'
     */
    public static Vector getUnusedAttributes(Element e, Vector usedAtts) {
        Vector unusedAtts = getAttributeList(e);
        for (int i = 0; i < usedAtts.size(); i++) {
            if (unusedAtts.contains(usedAtts.elementAt(i))) {
                unusedAtts.removeElement(usedAtts.elementAt(i));
            }
        }

        return unusedAtts;
    }

    /**
     * @param e        an Element with attributes
     * @param usedAtts Vector (of String) attributes
     * @return String warning about which attributes from 'e' aren't in 'usedAtts'
     */
    public static String unusedAttWarning(Element e, Vector usedAtts) {
        String warning = "";
        Vector unusedAtts = getUnusedAttributes(e, usedAtts);

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
     * @param e        an Element with attributes
     * @param usedAtts Vector (of String) attributes
     * @return boolean representing whether there are any attributes in 'e' not
     * in 'usedAtts'
     */
    public static boolean showUnusedAttributeWarning(Element e, Vector usedAtts) {
        return getUnusedAttributes(e, usedAtts).size() > 0;
    }

    /**
     * Is this element an Output tag?
     *
     * @param e Element
     * @return boolean
     */
    public static boolean isOutput(Element e) {
        return e.getName().toLowerCase().equals("output");
    }
}
