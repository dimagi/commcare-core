package org.commcare.xml;

import org.commcare.suite.model.Text;
import org.commcare.suite.model.graph.Annotation;
import org.commcare.suite.model.graph.BubbleSeries;
import org.commcare.suite.model.graph.Configurable;
import org.commcare.suite.model.graph.Graph;
import org.commcare.suite.model.graph.XYSeries;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/*
 * Parser for a <graph> element, typically used as a detail field's template.
 * @author jschweers
 */
public class GraphParser extends ElementParser<Graph> {
    public GraphParser(KXmlParser parser) {
        super(parser);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.xml.ElementParser#parse()
     */
    public Graph parse() throws InvalidStructureException, IOException, XmlPullParserException {
        throw new InvalidStructureException("Unable to parse graph", parser);
    }
}
