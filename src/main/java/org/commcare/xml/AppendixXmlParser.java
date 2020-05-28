package org.commcare.xml;

import org.commcare.cases.appendix.Appendix;
import org.commcare.cases.ledger.Ledger;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Parsers for appendix XML models
 *
 * @author ctsims
 */
public class AppendixXmlParser extends TransactionParser<Appendix> {
    public static final String TAG_APPENDIX = "appendix";
    private static final String TAG_CASE = "case";
    private static final String TAG_INDEX = "index";


    public static final String APPENDIX_XML_NAMESPACE = "http://commcarehq.org/appendix/v1";

    final IStorageUtilityIndexed<Appendix> storage;

    /**
     * Creates a Parser for case blocks in the XML stream provided.
     *
     * @param parser The parser for incoming XML.
     */
    public AppendixXmlParser(KXmlParser parser, IStorageUtilityIndexed<Appendix> storage) {
        super(parser);
        this.storage = storage;
    }

    @Override
    public Appendix parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode(TAG_APPENDIX);

        String id = parser.getAttributeValue(null, "id");
        String hash = parser.getAttributeValue(null, "hash");

        if (id == null || hash == null) {
            throw new InvalidStructureException("<appendix> requires an @id attribute and a @hash attribute", parser);
        }

        Vector<String> indexedKeys = new Vector();

        while (nextTagInBlock(TAG_APPENDIX)) {
            if (TAG_CASE.equals(parser.getName().toLowerCase())) {
                while (this.nextTagInBlock(TAG_CASE)) {
                    while (nextTagInBlock(TAG_INDEX)) {
                        indexedKeys.add(parser.getAttributeValue(null, "property"));
                    }
                }
            }
        }

        Appendix appendix = new Appendix(id, hash, indexedKeys);

        Appendix existingRecord = Appendix.getAppendix(storage(), id, hash);

        if (existingRecord != null) {
             return null;
        } else {
            commit(appendix);
            return appendix;
        }
    }

    @Override
    protected void commit(Appendix parsed) throws IOException {
        storage().write(parsed);
    }


    public IStorageUtilityIndexed<Appendix> storage() {
        return storage;
    }

}
