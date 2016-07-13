package org.commcare.data.xml;

import org.javarosa.xml.ElementParser;
import org.kxml2.io.KXmlParser;

import java.io.IOException;

/**
 * @author ctsims
 */
public abstract class TransactionParser<T> extends ElementParser<T> {
    public TransactionParser(KXmlParser parser) {
        super(parser);
    }

    protected abstract void commit(T parsed) throws IOException;
}
