package org.javarosa.engine.xml;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.engine.models.Mockup;
import org.javarosa.engine.models.Mockup.MockupEditor;
import org.javarosa.engine.models.Session;
import org.javarosa.xml.util.InvalidStructureException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * @author ctsims
 */
public class MockupParser extends ElementParser<Mockup> {

    public MockupParser(InputStream suiteStream) throws IOException {
        super(suiteStream);
    }

    @Override
    public Mockup parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode("mockup");

        Mockup m = new Mockup();
        MockupEditor editor = m.getEditor();

        while(this.nextTagInBlock("mockup")) {
            String tag = this.parser.getName();
            if(tag.equals("context")) {

                //parse out the relevant context
                while(this.nextTagInBlock("context")) {
                    String intag = this.parser.getName();
                    if(intag.equals("date")) {
                        String dateText = this.parser.nextText();
                        if(dateText == null || "".equals(dateText)) {
                            //nothing, use the current date
                        } else {
                            try {
                                Date d = DateUtils.parseDate(dateText);
                                if(d == null) {
                                    throw new InvalidStructureException("Bad <date> in context: '" + dateText + "'", parser);
                                }
                                editor.setDate(d);
                            } catch(Exception e){
                                throw new InvalidStructureException("Bad <date> in context: '" + dateText + "'", parser);
                            }
                        }
                    }

                    else if(intag.equals("instance")) {
                        FormInstanceParser fip = new FormInstanceParser(parser);
                        FormInstance instance = fip.parse();
                        editor.addInstance(instance);
                        //TODO: Somehow we need to make sure that there are no more siblings
                    } else {
                        throw new InvalidStructureException("Unrecognized context element: <" + tag + ">", parser);
                    }
                }

            } else if(tag.equals("session")) {
                Session s = new SessionParser(parser).parse();
                editor.addSession(s);
            }
        }
        editor.commit();
        return m;
    }
}
