package org.commcare.xml;

import org.commcare.cases.ledger.Ledger;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Contains all of the logic for parsing transactions in xml that pertain to
 * ledgers (balance/transfer actions)
 *
 * @author ctsims
 */
public class LedgerXmlParsers extends TransactionParser<Ledger[]> {
    private static final String TAG_QUANTITY = "quantity";
    private static final String TAG_VALUE = "value";
    private static final String ENTRY_ID = "id";
    private static final String TRANSFER = "transfer";
    private static final String TAG_BALANCE = "balance";

    public static final String STOCK_XML_NAMESPACE = "http://commcarehq.org/ledger/v1";

    private static final String MODEL_ID = "entity-id";
    private static final String SUBMODEL_ID = "section-id";
    private static final String FINAL_NAME = "entry";

    final IStorageUtilityIndexed<Ledger> storage;

    /**
     * Creates a Parser for case blocks in the XML stream provided.
     *
     * @param parser The parser for incoming XML.
     */
    public LedgerXmlParsers(KXmlParser parser, IStorageUtilityIndexed<Ledger> storage) {
        super(parser);
        this.storage = storage;
    }

    @Override
    public Ledger[] parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode(new String[]{TAG_BALANCE, TRANSFER});

        String name = parser.getName().toLowerCase();

        final Vector<Ledger> toWrite = new Vector<>();

        String dateModified = parser.getAttributeValue(null, "date");
        if (dateModified == null) {
            throw new InvalidStructureException("<" + name + "> block with no date_modified attribute.", this.parser);
        }
        Date modified = DateUtils.parseDateTime(dateModified);

        if (name.equals(TAG_BALANCE)) {
            String entityId = parser.getAttributeValue(null, MODEL_ID);
            if (entityId == null) {
                throw new InvalidStructureException("<balance> block with no " + MODEL_ID + " attribute.", this.parser);
            }

            final Ledger ledger = retrieveOrCreate(entityId);

            //The section ID being defined or not determines whether this is going to update a single section or whether
            //we'll be updating multiple sections
            String sectionId = parser.getAttributeValue(null, SUBMODEL_ID);

            if (sectionId == null) {
                //Complex case: we need to update multiple sections on a per-entry basis
                while (this.nextTagInBlock(TAG_BALANCE)) {

                    //We need to capture some of the state (IE: Depth, etc) to parse recursively,
                    //so create a new anonymous parser.
                    new ElementParser<Ledger[]>(this.parser) {
                        @Override
                        public Ledger[] parse() throws InvalidStructureException, IOException, XmlPullParserException {
                            String productId = parser.getAttributeValue(null, ENTRY_ID);

                            //Walk through the value setters and pull out all of the quantities to be updated for this section.
                            while (this.nextTagInBlock(FINAL_NAME)) {
                                this.checkNode(TAG_VALUE);

                                String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
                                String sectionId = parser.getAttributeValue(null, SUBMODEL_ID);
                                if (sectionId == null || "".equals(sectionId)) {
                                    throw new InvalidStructureException("<value> update requires a valid @" + SUBMODEL_ID + " attribute", this.parser);
                                }
                                int quantity = this.parseInt(quantityString);

                                //This performs the actual modification. This entity will be written outside of the loop
                                ledger.setEntry(sectionId, productId, quantity);
                            }
                            return null;
                        }

                    }.parse();
                }
            } else {
                //Simple case - Updating one section
                while (this.nextTagInBlock(TAG_BALANCE)) {
                    this.checkNode(FINAL_NAME);
                    String id = parser.getAttributeValue(null, ENTRY_ID);
                    String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
                    if (id == null || id.equals("")) {
                        throw new InvalidStructureException("<" + FINAL_NAME + "> update requires a valid @id attribute", this.parser);
                    }
                    int quantity = this.parseInt(quantityString);
                    ledger.setEntry(sectionId, id, quantity);
                }
            }

            //Either way, we've updated the ledger and want to write it now
            toWrite.addElement(ledger);
        } else if (name.equals(TRANSFER)) {

            //First, figure out where we're reading/writing and load the ledgers
            String source = parser.getAttributeValue(null, "src");
            String destination = parser.getAttributeValue(null, "dest");

            if (source == null && destination == null) {
                throw new InvalidStructureException("<transfer> block no source or destination id.", this.parser);
            }

            final Ledger sourceLeger = source == null ? null : retrieveOrCreate(source);
            final Ledger destinationLedger = destination == null ? null : retrieveOrCreate(destination);

            //The section ID being defined or not determines whether this is going to update a single section or whether
            //we'll be updating multiple sections

            String sectionId = parser.getAttributeValue(null, SUBMODEL_ID);

            if (sectionId == null) {
                while (this.nextTagInBlock(TRANSFER)) {
                    //We need to capture some of the state (IE: Depth, etc) to parse recursively,
                    //so create a new anonymous parser.
                    new ElementParser<Ledger[]>(this.parser) {
                        @Override
                        public Ledger[] parse() throws InvalidStructureException, IOException, XmlPullParserException {
                            String productId = parser.getAttributeValue(null, ENTRY_ID);

                            //Walk through and find what sections to update for this entry
                            while (this.nextTagInBlock(FINAL_NAME)) {
                                this.checkNode(TAG_VALUE);

                                String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
                                String sectionId = parser.getAttributeValue(null, SUBMODEL_ID);
                                if (sectionId == null || sectionId.equals("")) {
                                    throw new InvalidStructureException("<value> update requires a valid @" + SUBMODEL_ID + " attribute", this.parser);
                                }
                                int quantity = this.parseInt(quantityString);

                                if (sourceLeger != null) {
                                    sourceLeger.setEntry(sectionId, productId, sourceLeger.getEntry(sectionId, productId) - quantity);
                                }
                                if (destinationLedger != null) {
                                    destinationLedger.setEntry(sectionId, productId, destinationLedger.getEntry(sectionId, productId) + quantity);
                                }
                            }
                            return null;
                        }

                    }.parse();
                }
            } else {
                //Easy case, we've got a single section and we're going to transfer between the ledgers
                while (this.nextTagInBlock(TRANSFER)) {
                    this.checkNode(FINAL_NAME);
                    String entryId = parser.getAttributeValue(null, ENTRY_ID);
                    String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
                    if (entryId == null || entryId.equals("")) {
                        throw new InvalidStructureException("<" + FINAL_NAME + "> update requires a valid @" + ENTRY_ID + " attribute", this.parser);
                    }
                    int quantity = this.parseInt(quantityString);

                    if (sourceLeger != null) {
                        sourceLeger.setEntry(sectionId, entryId, sourceLeger.getEntry(sectionId, entryId) - quantity);
                    }
                    if (destinationLedger != null) {
                        destinationLedger.setEntry(sectionId, entryId, destinationLedger.getEntry(sectionId, entryId) + quantity);
                    }
                }
            }

            //Either way, we want to now write both ledgers.
            if (sourceLeger != null) {
                toWrite.addElement(sourceLeger);
            }
            if (destinationLedger != null) {
                toWrite.addElement(destinationLedger);
            }
        }

        Ledger[] tw = new Ledger[toWrite.size()];
        int i = 0;
        for (Ledger s : toWrite) {
            tw[i] = s;
            i++;
        }
        //this should really be decided on _not_ in the parser...
        commit(tw);

        return tw;
    }

    @Override
    protected void commit(Ledger[] parsed) throws IOException {
        try {
            for (Ledger s : parsed) {
                storage().write(s);
            }
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new IOException("Storage full while writing case!");
        }
    }

    public Ledger retrieveOrCreate(String entityId) {
        try {
            return storage().getRecordForValue(Ledger.INDEX_ENTITY_ID, entityId);
        } catch (NoSuchElementException nsee) {
            return new Ledger(entityId);
        }
    }

    public IStorageUtilityIndexed<Ledger> storage() {
        return storage;
    }

}
