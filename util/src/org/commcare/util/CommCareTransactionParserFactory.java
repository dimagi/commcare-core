package org.commcare.util;

import java.io.IOException;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.UserXmlParser;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.FixtureXmlParser;
import org.commcare.xml.LedgerXmlParsers;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A factory for covering all of the basic transactions expected in userspace
 * against the mock data sandbox provided.
 *
 * This set of transactions should almost certainly be made uniform between
 * different platform implementations, but the different platforms require some
 * subtle difference right now which make that challenging.
 *
 * @author ctsims
 */
public class CommCareTransactionParserFactory implements TransactionParserFactory {

    private TransactionParserFactory userParser;
    private TransactionParserFactory caseParser;
    private TransactionParserFactory stockParser;
    private TransactionParserFactory fixtureParser;

    private final MockUserDataSandbox sandbox;

    private int requests = 0;
    private String syncToken;

    public CommCareTransactionParserFactory(MockUserDataSandbox sandbox) {
        this.sandbox = sandbox;

        this.initFixtureParser();
        this.initUserParser();
        this.initCaseParser();
        this.initStockParser();
    }

    @Override
    public TransactionParser getParser(KXmlParser parser) {
        String namespace = parser.getNamespace();
        String name = parser.getName();
        if (LedgerXmlParsers.STOCK_XML_NAMESPACE.matches(namespace)) {
            if (stockParser == null) {
                throw new RuntimeException("Couldn't process Stock transaction without initialization!");
            }
            req();
            return stockParser.getParser(parser);
        } else if ("case".equalsIgnoreCase(name)) {
            if (caseParser == null) {
                throw new RuntimeException("Couldn't receive Case transaction without initialization!");
            }
            req();
            return caseParser.getParser(parser);
        } else if ("registration".equalsIgnoreCase(name)) {
            if (userParser == null) {
                throw new RuntimeException("Couldn't receive User transaction without initialization!");
            }
            req();
            return userParser.getParser(parser);
        } else if ("fixture".equalsIgnoreCase(name)) {
            req();
            return fixtureParser.getParser(parser);
        } else if ("message".equalsIgnoreCase(name)) {
            //server message;
            //" <message nature=""/>"
        } else if ("sync".equalsIgnoreCase(name) && 
                "http://commcarehq.org/sync".equals(namespace)) {
            return new TransactionParser<String>(parser) {
                /*
                 * (non-Javadoc)
                 * @see org.commcare.data.xml.TransactionParser#commit(java.lang.Object)
                 */
                @Override
                public void commit(String parsed) throws IOException {
                }

                /*
                 * (non-Javadoc)
                 * @see org.javarosa.xml.ElementParser#parse()
                 */
                @Override
                public String parse() throws InvalidStructureException,
                       IOException, XmlPullParserException,
                       UnfullfilledRequirementsException {
                    this.checkNode("sync");
                    this.nextTag("restore_id");
                    String syncToken = parser.nextText();
                    if (syncToken == null) {
                        throw new InvalidStructureException("Sync block must contain restore_id with valid ID inside!", parser);
                    }
                    syncToken = syncToken.trim();
                    
                    sandbox.setSyncToken(syncToken);
                    
                    return syncToken;
                }

            };
        }
        return null;
    }

    private void req() {
        requests++;
        reportProgress(requests);
    }

    void reportProgress(int total) {
        //nothing
    }

    void initUserParser() {
        userParser = new TransactionParserFactory() {
            UserXmlParser created = null;

            public TransactionParser getParser(KXmlParser parser) {
                if (created == null) {
                    created = new UserXmlParser(parser, sandbox.getUserStorage());
                }

                return created;
            }
        };
    }
    
    public void initFixtureParser() {
        fixtureParser = new TransactionParserFactory() {
            FixtureXmlParser created = null;

            public TransactionParser getParser(KXmlParser parser) {
                if (created == null) {
                    created = new FixtureXmlParser(parser) {
                        //TODO: store these on the file system instead of in DB?
                        private IStorageUtilityIndexed<FormInstance> fixtureStorage;

                        /*
                         * (non-Javadoc)
                         * @see org.commcare.xml.FixtureXmlParser#storage()
                         */
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

    void initCaseParser() {
        caseParser = new TransactionParserFactory() {
            CaseXmlParser created = null;

            public TransactionParser<Case> getParser(KXmlParser parser) {
                if (created == null) {
                    created = new CaseXmlParser(parser, sandbox.getCaseStorage());
                }

                return created;
            }
        };
    }

    void initStockParser() {
        stockParser = new TransactionParserFactory() {

            public TransactionParser<Ledger[]> getParser(KXmlParser parser) {
                return new LedgerXmlParsers(parser, sandbox.getLedgerStorage());
            }
        };
    }
}
