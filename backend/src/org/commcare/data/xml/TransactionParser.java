package org.commcare.data.xml;

import java.io.IOException;

import org.javarosa.xml.ElementParser;
import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 */
public abstract class TransactionParser<T> extends ElementParser<T> {
    public TransactionParser(KXmlParser parser) {
        super(parser);
    }

    public abstract void commit(T parsed) throws IOException;
}
