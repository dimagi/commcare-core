/**
 *
 */
package org.commcare.util;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.AttachableCaseXMLParser;
import org.commcare.xml.FixtureXmlParser;
import org.commcare.xml.LedgerXmlParsers;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.J2MEUserXmlParser;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;
import org.commcare.util.J2MESandbox;

import java.io.IOException;

/**
 * The CommCare Transaction Parser Factory (whew!) wraps all of the current
 * transactions that CommCare knows about, and provides the appropriate hooks for
 * parsing through XML and dispatching the right handler for each transaction.
 *
 * It should be the central point of processing for transactions (eliminating the
 * use of the old datamodel based processors) and should be used in any situation where
 * a transaction is expected to be present.
 *
 * It is expected to behave more or less as a black box, in that it directly creates/modifies
 * the data models on the system, rather than producing them for another layer or processing.
 *
 * @author ctsims
 *
 */
public class CommCareTransactionParserFactory extends org.commcare.core.parse.CommCareTransactionParserFactory {

    private int[] caseTallies;
    private boolean tolerant;
    private String message;
    private OrderedHashtable<String, String> messages = new OrderedHashtable<String,String>();

    /**
     * Creates a new factory for processing incoming XML.
     * @param tolerant True if processing should fail in the event of conflicting data,
     * false if processing should proceed as long as it is possible.
     */
    public CommCareTransactionParserFactory(boolean tolerant) {
        super(new J2MESandbox());
        syncToken = null;
        caseTallies = new int[3];
        this.tolerant = tolerant;
    }

    /*
     * (non-Javadoc)
     * @see org.commcare.data.xml.TransactionParserFactory#getParser(org.kxml2.io.KXmlParser)
     */
    public TransactionParser getParser(KXmlParser parser) {
        String namespace = parser.getNamespace();
        String name = parser.getName();

        TransactionParser superParser = super.getParser(parser);
        if(superParser != null){
            return superParser;
        }

        if("message".equalsIgnoreCase(name)) {
            return new TransactionParser<String>(parser) {

            String nature = parser.getAttributeValue(null, "nature");

            public void commit(String parsed) throws IOException {

            }

            public String parse() throws InvalidStructureException,IOException, XmlPullParserException, UnfullfilledRequirementsException {

                    message = parser.nextText();
                    if(nature != null) {
                        if(message != null) {
                            messages.put(nature, message);
                        }
                    }
                    return message;
                }
            };

        }
        return null;
    }

    public void initCaseParser() {
        caseParser = new TransactionParserFactory() {
            CaseXmlParser created = null;

            public TransactionParser<Case> getParser(KXmlParser parser) {
                return new AttachableCaseXMLParser(parser, caseTallies, tolerant, sandbox.getCaseStorage());
                return created;
            }
        };
    }

    public void initUserParser() {
        userParser = new TransactionParserFactory() {
            J2MEUserXmlParser created = null;

            public TransactionParser getParser(KXmlParser parser) {
                if(created == null) {
                    created = new J2MEUserXmlParser(parser, sandbox.getUserStorage(), getSyncToken());
                }

                return created;
            }
        };
    }

    /**
     * @return An int[3] array containing a count of Cases
     * int[0]: created
     * int[1]: updated
     * int[2]: closed
     *
     * after processing has completed.
     */
    public int[] getCaseTallies() {
        return caseTallies;
    }

    /**
     * @return After processing is completed, if a message to the user was present, it
     * will be returned here.
     */
    public String getResponseMessage() {
        return message;
    }

    public OrderedHashtable<String,String> getResponseMessageMap() {
        return messages;
    }
}
