/**
 *
 */
package org.commcare.xml.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.commcare.cases.model.Case;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.suite.model.Text;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.FixtureXmlParser;
import org.commcare.xml.TextParser;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * @author ctsims
 */
public class SnippetParser<T> {
    TransactionParserFactory tf;

    Object o;

    public SnippetParser() {
        tf = new TransactionParserFactory() {

            public TransactionParser getParser(String name, String namespace, KXmlParser parser) {
                if (name.toLowerCase().equals("case")) {
                    return new CaseXmlParser(parser, null) {
                        public void commit(Case parsed) throws IOException {
                            o = parsed;
                        }

                        public Case retrieve(String entityId) {
                            return null;
                        }
                    };
                } else if (name.toLowerCase().equals("registration")) {
                    //unsupported for now
                } else if (name.toLowerCase().equals("message")) {
                    return new TransactionParser<String>(parser, "message", null) {

                        String nature = parser.getAttributeValue(null, "nature");

                        public void commit(String parsed) throws IOException {
                            o = parsed;
                        }

                        public String parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
                            String message = parser.nextText();
                            commit(message);
                            //anything?
                            return message;
                        }
                    };

                } else if (name.equalsIgnoreCase("Sync")) {
                    return new TransactionParser<String>(parser, "Sync", null) {
                        public void commit(String parsed) throws IOException {
                            o = parsed;
                        }

                        public String parse() throws XmlPullParserException, IOException, InvalidStructureException {
                            if (this.nextTagInBlock("Sync")) {
                                this.checkNode("restore_id");
                                String newId = parser.nextText().trim();
                                //Yo, do we want to do anything with this ID?

                                commit(newId);
                                return newId;
                            } else {
                                throw new InvalidStructureException("<Sync> block missing <restore_id>", this.parser);
                            }
                        }
                    };
                } else if (name.toLowerCase().equals("fixture")) {
                    return new FixtureXmlParser(parser) {
                        public void commit(FormInstance parsed) throws IOException {
                            //We want this, right?
                            o = parsed;
                        }

                        public IStorageUtilityIndexed storage() {
                            return null;
                        }
                    };
                } else if (name.toLowerCase().equals("text")) {
                    return new TransactionParser<Text>(parser, name, namespace) {

                        public Text parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
                            Text t = new TextParser(parser).parse();
                            commit(t);
                            return t;
                        }


                        public void commit(Text parsed) throws IOException {
                            //We want this, right?
                            o = parsed;
                        }
                    };
                }
                return null;
            }
        };
    }

    public T parseSomething(String input) {
        try {
            input = "<wrapper>" + input + "</wrapper>";
            ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes("UTF-8"));
            DataModelPullParser p = new DataModelPullParser(bais, tf);
            o = null;
            p.parse();
            return (T)o;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
