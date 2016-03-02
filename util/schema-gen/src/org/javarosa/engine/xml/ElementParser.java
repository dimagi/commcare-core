package org.javarosa.engine.xml;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Element Parser is the core parsing element
 * for CommCare XML files. It is implemented for
 * each CommCare data type to encapsulate all of
 * the parsing rules for that type's XML definition.
 * </p>
 *
 * <p>
 * A number of helper methods are provided in the
 * parser which are intended to standardize the
 * techniques used for validation and pull-parsing
 * through the XML Document.
 * </p>
 *
 * @author ctsims
 *
 */
public abstract class ElementParser<T> {
    protected final KXmlParser parser;

    int level = 0;

    /**
     * Produces a new element parser for the appropriate datatype.
     *
     * @param suiteStream A stream which is reading the XML content
     * of the document.
     *
     * @throws IOException If the stream cannot be read for any reason
     * other than invalid CommCare XML Structures.
     */
    public ElementParser(InputStream suiteStream) throws IOException{
        parser = new KXmlParser();
        try {
            parser.setInput(suiteStream,"UTF-8");
            parser.setFeature(KXmlParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.next();

        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Produces a new element parser for the appropriate datatype.
     *
     * @param parser An XML Pull Parser which is currently at the
     * position of the top level element that represents this
     * element's XML structure.
     */
    public ElementParser(KXmlParser parser) {
        this.parser = parser;
        level = parser.getDepth();
    }

    /**
     * Evaluates whether the current node is the appropriate name
     * and throws the proper exception if not.
     *
     * @param name The name of the element which is expected at this
     * step of parsing.
     * @throws InvalidStructureException If the node at the current
     * position is not the one expected.
     */
    protected void checkNode(String name) throws InvalidStructureException {
        if(!parser.getName().toLowerCase().equals(name)) {
            throw new InvalidStructureException("Expected <" + name + "> element <"+ parser.getName() + "> found instead",parser);
        }
    }

    /**
     * Retrieves the next tag in the XML document which is internal
     * to the tag identified by terminal. If there are no further tags
     * inside this tag, an invalid structure is detected and the proper
     * exception is thrown.
     *
     * @param terminal The name of the tag which the next tag expected
     * should be inside of.
     *
     * @throws InvalidStructureException If no further tags inside of terminal
     * are available or if any other invalid CommCare XML structures are
     * detected.
     * @throws IOException If the parser has a problem reading the document
     * @throws XmlPullParserException If the stream does not contain well-formed
     * XML.
     */
    protected void getNextTagInBlock(String terminal) throws InvalidStructureException, IOException, XmlPullParserException {
        if(!nextTagInBlock(terminal)) {throw new InvalidStructureException("Expected another node inside of element <" + terminal + ">.",parser);}
    }

    /**
     * Retrieves the next tag in the XML document which is internal
     * to the tag identified by terminal. If there are no further tags
     * inside this tag, false will be returned.
     *
     * @param terminal The name of the tag which the next tag expected
     * should be inside of.
     *
     * @throws InvalidStructureException If any invalid CommCare XML structures are
     * detected.
     * @throws IOException If the parser has a problem reading the document
     * @throws XmlPullParserException If the stream does not contain well-formed
     * XML.
     */
    protected boolean nextTagInBlock(String terminal) throws InvalidStructureException, IOException, XmlPullParserException {
            int eventType;

            //eventType = parser.nextTag();
            eventType = parser.next();
            if(eventType == KXmlParser.TEXT && parser.isWhitespace()) {   // skip whitespace
                 eventType = parser.next();
            }

            if(eventType == KXmlParser.START_DOCUMENT) {
                //
            } else if(eventType == KXmlParser.END_DOCUMENT) {
                return false;
            } else if(eventType == KXmlParser.START_TAG) {
                return true;
            } else if(eventType == KXmlParser.END_TAG) {
                //If we've reached the end of the current node path,
                //return false (signaling that the parsing action should end).
                if(parser.getName().toLowerCase().equals(terminal.toLowerCase())) { return false; }
                //Elsewise, as long as we haven't left the current context, keep diving
                else if(parser.getDepth() >= level) { return nextTagInBlock(terminal); }
                //if we're below the limit, get out.
                else { return false; }
            } else if(eventType == KXmlParser.TEXT) {
                return true;
            }
            return true;
    }

    /**
     * Retrieves the next tag in the XML document, assuming
     * that it is named the same as the provided parameter.
     * If there is no next tag in the current block, or
     * if the tag is named something else, an InvalidStructureException
     * is thrown.
     *
     * @param name The name which should match the next tag.
     * @throws InvalidStructureException
     * @throws IOException
     * @throws XmlPullParserException
     */
    protected void nextTag(String name) throws InvalidStructureException, IOException, XmlPullParserException {
        int depth = parser.getDepth();
        if(nextTagInBlock(null)) {
            if(parser.getDepth() == depth || parser.getDepth() == depth + 1) {
                if(parser.getName().toLowerCase().equals(name.toLowerCase())) {
                    return;
                }
                throw new InvalidStructureException("Expected tag " + name + " but got tag: " + parser.getName(), parser);
            }
            throw new InvalidStructureException("Expected tag " + name + " but reached end of block instead", parser);
        }

        throw new InvalidStructureException("Expected tag " + name + " but it wasn't found", parser);

    }

    /**
     * Retrieves the next tag in the XML document. If there are no further
     * tags in the document, an invalid structure is detected and the proper
     * exception is thrown.
     *
     * @throws InvalidStructureException If no further tags
     * are available or if any other invalid CommCare XML structures are
     * detected.
     * @throws IOException If the parser has a problem reading the document
     * @throws XmlPullParserException If the stream does not contain well-formed
     * XML.
     */
    protected boolean nextTagInBlock() throws InvalidStructureException, IOException, XmlPullParserException {
        return nextTagInBlock(null);
    }

    /**
     * Takes in a string which contains an integer value and returns the
     * integer which it represents, throwing an invalid structure exception
     * if the value is not an integer.
     *
     * @param value A string containing an integer value
     * @return The integer represented
     * @throws InvalidStructureException If the string does not contain
     * an integer.
     */
    protected int parseInt(String value) throws InvalidStructureException  {
        if(value == null) {
            throw new InvalidStructureException("Expected an integer value, found null text instead",parser);
        }
        try  {
            return Integer.parseInt(value);
        } catch(NumberFormatException nfe) {
            throw new InvalidStructureException("Expected an integer value, found " + value + " instead",parser);
        }
    }

    /**
     * Parses the XML document at the current level, returning the datatype
     * described by the document.
     *
     * @return The datatype which is described by the appropriate XML
     * definition.
     *
     * @throws InvalidStructureException If the XML does not contain properly
     * structured CommCare XML
     * @throws IOException If there is a problem retrieving the document
     * @throws XmlPullParserException If the document does not contain well-
     * formed XML.
     */
    public abstract T parse() throws InvalidStructureException, IOException, XmlPullParserException;


    public void skipBlock(String tag) throws XmlPullParserException, IOException {

        while(parser.getEventType() != KXmlParser.END_DOCUMENT) {
            int eventType;
            eventType = parser.next();

            if(eventType == KXmlParser.START_DOCUMENT) {

            } else if(eventType == KXmlParser.END_DOCUMENT) {
                return;
            } else if(eventType == KXmlParser.START_TAG) {

            } else if(eventType == KXmlParser.END_TAG) {
                if(parser.getName().equals(tag)) {
                    return;
                }
            } else if(eventType == KXmlParser.TEXT) {

            }
        }
    }

    protected int nextNonWhitespace() throws XmlPullParserException, IOException {
        int ret = parser.next();
        if(ret == KXmlParser.TEXT && parser.isWhitespace()) {
            ret = parser.next();
        }
        return ret;
    }
}
