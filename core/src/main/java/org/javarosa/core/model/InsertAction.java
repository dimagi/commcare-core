package org.javarosa.core.model;

import org.javarosa.xform.parse.IElementHandler;
import org.javarosa.xform.parse.XFormParser;
import org.kxml2.kdom.Element;

/**
 * Created by amstone326 on 2/2/16.
 */
public class InsertAction extends Action {

    public static final String ELEMENT_NAME = "";

    public static IElementHandler getHandler() {
        return new IElementHandler() {
            @Override
            public void handle(XFormParser p, Element e, Object parent) {
                //TODO: implement
            }
        };
    }

}
