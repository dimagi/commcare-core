package org.javarosa.xform.parse;

import org.kxml2.kdom.Element;

/**
 * An IElementHandler is responsible for handling the parsing of a particular
 * XForms node.
 *
 * @author Drew Roos
 */
public interface IElementHandler {
    void handle(XFormParser p, Element e, Object parent);
}
