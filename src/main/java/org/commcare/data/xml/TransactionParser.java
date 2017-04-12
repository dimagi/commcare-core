package org.commcare.data.xml;

import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * @author ctsims
 */
public abstract class TransactionParser<T> extends ElementParser<T> {
    public TransactionParser(KXmlParser parser) {
        super(parser);
    }

    protected abstract void commit(T parsed) throws IOException, InvalidStructureException;

    /**
     *  Notifies the parser that the end-to-end parse has been completed and allows it to
     *  clean up any state it may have reserved.
     */
    protected void flush() throws IOException, XmlPullParserException,
            InvalidStructureException {

    }
}
