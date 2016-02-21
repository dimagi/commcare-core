package org.commcare.xml;

import org.commcare.suite.model.DetailTemplate;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DummyGraphParser extends GraphParser {
    public DummyGraphParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public DetailTemplate parse() throws InvalidStructureException, IOException, XmlPullParserException {
        skipBlock("graph");
        return new DummyGraphDetailTemplate();
    }

    public static class DummyGraphDetailTemplate implements  DetailTemplate, Externalizable {
        public DummyGraphDetailTemplate() {
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return "graph";
        }

        @Override
        public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {

        }

        @Override
        public void writeExternal(DataOutputStream out) throws IOException {

        }
    }
}
