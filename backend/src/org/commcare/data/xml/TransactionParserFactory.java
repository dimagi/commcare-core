package org.commcare.data.xml;

import org.kxml2.io.KXmlParser;

public interface TransactionParserFactory {
    TransactionParser getParser(KXmlParser parser);
}
