package org.commcare.data.xml;

import org.commcare.resources.model.CommCareOTARestoreListener;
import org.javarosa.core.log.WrappedException;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * A DataModelPullParser pulls together the parsing of
 * different data models in order to be able to perform
 * a master update/restore of remote data.
 *
 * @author ctsims
 */
public class DataModelPullParser extends ElementParser<Boolean> {

    Vector<String> errors;

    TransactionParserFactory factory;

    boolean failfast;
    boolean deep;

    InputStream is;

    String requiredRootEnvelope = null;

    CommCareOTARestoreListener rListener;

    public DataModelPullParser(InputStream is, TransactionParserFactory factory) throws InvalidStructureException, IOException {
        this(is, factory, false);
    }

    public DataModelPullParser(InputStream is, TransactionParserFactory factory, CommCareOTARestoreListener rl) throws InvalidStructureException, IOException {
        this(is, factory, false, false, rl);
    }

    public DataModelPullParser(InputStream is, TransactionParserFactory factory, boolean deep) throws InvalidStructureException, IOException {
        this(is, factory, false, deep);
    }

    public DataModelPullParser(InputStream is, TransactionParserFactory factory, boolean failfast, boolean deep) throws InvalidStructureException, IOException {
        this(is, factory, failfast, deep, null);
    }

    public DataModelPullParser(InputStream is, TransactionParserFactory factory, boolean failfast, boolean deep, CommCareOTARestoreListener rListener) throws InvalidStructureException, IOException {
        super(ElementParser.instantiateParser(is));
        this.is = is;
        this.failfast = failfast;
        this.factory = factory;
        errors = new Vector<>();
        this.deep = deep;
        this.rListener = rListener;
    }

    public Boolean parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        try {

            String rootName = parser.getName();

            if (requiredRootEnvelope != null && !requiredRootEnvelope.equals(rootName)) {
                throw new InvalidStructureException("Invalid xml evelope: \"" + rootName + "\" when looking for \"" + requiredRootEnvelope + "\"", parser);
            }

            String itemString = parser.getAttributeValue(null, "items");

            int itemNumber = -1;

            if (itemString != null) {

                try {
                    itemNumber = Integer.parseInt(itemString);
                } catch (NumberFormatException e) {
                    itemNumber = 0;
                }
                if (rListener != null) {
                    rListener.setTotalForms(itemNumber);
                }
                //throw new InvalidStructureException("<item> block with no item_id attribute.", this.parser);
            }
            //Here we'll go through in search of CommCare data models and parse
            //them using the appropriate CommCare Model data parser.

            //Go through each child of the root element
            parseBlock(rootName);
        } finally {
            //kxmlparser might close the stream, but we can't be sure, especially if
            //we bail early due to schema errors
            try {
                is.close();
            } catch (IOException ioe) {
                //swallow
            }
        }

        if (errors.size() == 0) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    private void parseBlock(String root) throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        int parsedCounter = 0;
        while (this.nextTagInBlock(root)) {

            if (listenerSet()) {
                rListener.onUpdate(parsedCounter);
                parsedCounter++;
            }

            String name = parser.getName();
            if (name == null) {
                continue;
            }

            TransactionParser transaction = factory.getParser(parser);
            if (transaction == null) {
                //nothing to be done for this element, recurse?
                if (deep) {
                    parseBlock(name);
                } else {
                    this.skipBlock(name);
                }
            } else {
                if (!failfast) {
                    try {
                        transaction.parse();
                    } catch (Exception e) {
                        e.printStackTrace();
                        deal(e, name);
                    }
                } else {
                    transaction.parse();
                }
            }
        }
    }

    private void deal(Exception e, String parentTag) throws XmlPullParserException, IOException {
        errors.addElement(WrappedException.printException(e));
        this.skipBlock(parentTag);

        if (failfast) {
            throw new WrappedException(e);
        }
    }

    public String[] getParseErrors() {
        String[] errorBuf = new String[errors.size()];
        for (int i = 0; i < errorBuf.length; ++i) {
            errorBuf[i] = errors.elementAt(i);
        }
        return errorBuf;
    }

    public boolean listenerSet() {
        return (rListener != null);
    }
}
