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
import org.commcare.xml.UserXmlParser;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

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
public class CommCareTransactionParserFactory implements TransactionParserFactory {

    private int[] caseTallies;
    private String restoreId;
    private boolean tolerant;
    private String message;
    private OrderedHashtable<String, String> messages = new OrderedHashtable<String,String>();

    /**
     * Creates a new factory for processing incoming XML.
     * @param tolerant True if processing should fail in the event of conflicting data,
     * false if processing should proceed as long as it is possible.
     */
    public CommCareTransactionParserFactory(boolean tolerant) {
        restoreId = null;
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
        if ("case".equalsIgnoreCase(name)) {
            return new AttachableCaseXMLParser(parser, caseTallies, tolerant, (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY));
        } else if("registration".equalsIgnoreCase(name)) {
            //TODO: It's possible we want to do the restoreID thing after signalling success, actually. If the
            //restore gets cut off, we don't want to be re-sending the token, since it implies that it worked.
            return new UserXmlParser(parser, restoreId);
        }  else if(LedgerXmlParsers.STOCK_XML_NAMESPACE.equalsIgnoreCase(namespace)) {
            return new LedgerXmlParsers(parser, (IStorageUtilityIndexed)StorageManager.getStorage(Ledger.STORAGE_KEY));
        } else if("message".equalsIgnoreCase(name)) {
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

        } else if ("sync".equalsIgnoreCase(name)) {
            return new TransactionParser<String>(parser) {
                public void commit(String parsed) throws IOException {
                    //do nothing
                }

                public String parse() throws XmlPullParserException, IOException, InvalidStructureException {
                    if(this.nextTagInBlock("Sync")){
                        this.checkNode("restore_id");
                        String newId = parser.nextText().trim();
                        if(restoreId != null) {
                            Logger.log("TRANSACTION","Warning: Multiple restore ID's seen:" + restoreId + "," + newId);
                        }
                        restoreId = newId;
                        return restoreId;
                    } else {
                        throw new InvalidStructureException("<Sync> block missing <restore_id>", this.parser);
                    }
                }
            };
        } else if("fixture".equalsIgnoreCase(name)) {
            return new FixtureXmlParser(parser);
        }
        return null;
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
     * @return After processing is completed, if a restore ID was present in the payload
     * it will be returned here.
     */
    public String getRestoreId() {
        return restoreId;
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
