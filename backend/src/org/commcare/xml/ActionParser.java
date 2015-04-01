package org.commcare.xml;

import java.io.IOException;
import java.util.Vector;

import org.commcare.suite.model.Action;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.StackOperation;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 */
public class ActionParser extends CommCareElementParser<Action> {

    public static final String NAME_ACTION = "action";

    public ActionParser(KXmlParser parser) {
        super(parser);
    }

    /* (non-Javadoc)
     * @see org.javarosa.xml.ElementParser#parse()
     */
    public Action parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode(NAME_ACTION);

        DisplayUnit display = null;
        Vector<StackOperation> stackOps = new Vector<StackOperation>();

        while (nextTagInBlock(NAME_ACTION)) {
            if (parser.getName().equals("display")) {
                display = parseDisplayBlock();
            } else if (parser.getName().equals("stack")) {
                StackOpParser sop = new StackOpParser(parser);
                while (this.nextTagInBlock("stack")) {
                    stackOps.addElement(sop.parse());
                }
            }
        }

        if (display == null) {
            throw new InvalidStructureException("<action> block must define a <display> element", parser);
        }
        if (stackOps.size() == 0) {
            throw new InvalidStructureException("An <action> block must define at least one stack operation", parser);
        }
        return new Action(display, stackOps);
    }
}
