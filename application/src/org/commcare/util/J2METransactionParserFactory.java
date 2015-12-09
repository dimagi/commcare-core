package org.commcare.util;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.AttachableCaseXMLParser;
import org.commcare.xml.LedgerXmlParsers;
import org.commcare.xml.FixtureXmlParserToDb;
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

import org.commcare.core.parse.CommCareTransactionParserFactory;

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
public class J2METransactionParserFactory extends CommCareTransactionParserFactory {

    private int[] caseTallies;
    private boolean tolerant;
    private String message;
    private OrderedHashtable<String, String> messages = new OrderedHashtable<String,String>();

    /**
     * Creates a new factory for processing incoming XML.
     * @param tolerant True if processing should fail in the event of conflicting data,
     * false if processing should proceed as long as it is possible.
     */
    public J2METransactionParserFactory(boolean tolerant) {
        super(new J2MESandbox());
        caseTallies = new int[3];
        this.tolerant = tolerant;
    }

    @Override
    public TransactionParser getParser(KXmlParser parser) {
        String namespace = parser.getNamespace();
        String name = parser.getName();

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
        // check for super AFTER so that we default to overridden case
        TransactionParser superParser = super.getParser(parser);
        if(superParser != null){
            return superParser;
        }

        return null;
    }

    @Override
    public void initCaseParser() {
        caseParser = new TransactionParserFactory() {
            CaseXmlParser created = null;

            public TransactionParser<Case> getParser(KXmlParser parser) {
                if(created == null) {
                    created = new AttachableCaseXMLParser(parser, caseTallies, tolerant, sandbox.getCaseStorage());
                }
                return created;
            }
        };
    }

    @Override
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

    @Override
    public void initFixtureParser() {
        fixtureParser = new TransactionParserFactory() {
            FixtureXmlParser created = null;

            @Override
            public TransactionParser getParser(KXmlParser parser) {
                if (created == null) {
                    created = new FixtureXmlParserToDb(parser) {
                        //TODO: store these on the file system instead of in DB?
                        private IStorageUtilityIndexed<FormInstance> fixtureStorage;

                        @Override
                        public IStorageUtilityIndexed<FormInstance> storage() {
                            if (fixtureStorage == null) {
                                fixtureStorage = CommCareTransactionParserFactory.this.sandbox.getUserFixtureStorage();
                            }
                            return fixtureStorage;
                        }
                    };
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
    @Override
    public int[] getCaseTallies() {
        return caseTallies;
    }

    /**
     * @return After processing is completed, if a message to the user was present, it
     * will be returned here.
     */
    @Override
    public String getResponseMessage() {
        return message;
    }

    @Override
    public OrderedHashtable<String,String> getResponseMessageMap() {
        return messages;
    }
}
