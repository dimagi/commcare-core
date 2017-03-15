package org.commcare.xml.bulk;

import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A bulk element parser reads multiple types of the same transaction together into a buffer of
 * TreeElements before performing a bulk processing step.
 *
 *
 * An implementing class should organize its implementation into the following steps:
 *
 * preParseValidate - Validate only the root element of the streaming xml doc, shouldn't advance
 * the processing
 *
 * requestModelReadsForElement - Gather id's for models that will be needed to process a transaction
 *
 * performBulkRead - Read any relevant data from storage all at once
 *
 * processBufferedElement - After the bulk read, process the transactions one by one
 *
 * performBulkWrite - Write the processed models to storage
 *
 * Created by ctsims on 3/14/2017.
 */

public abstract class BulkElementParser<T> extends TransactionParser<TreeElement> {

    protected int bulkTrigger = 500;
    private List<TreeElement> currentBulkElementBacklog = new ArrayList<>();
    private Set<String> currentBulkReadSet = new HashSet<>();
    private Map<String, T> currentOperatingSet = new HashMap<>();
    private SortedMap<String, T> writeLog = new TreeMap<>();

    private int currentBulkReadCount = 0;

    public BulkElementParser(KXmlParser parser) {
        super(parser);
    }

    protected void setBulkProcessTrigger(int newTriggerCount) {
        this.bulkTrigger = newTriggerCount;
    }

    @Override
    public TreeElement parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {

        preParseValidate();
        TreeElement subElement = new TreeElementParser(parser, 0, "bulk_parser").parse();
        requestModelReadsForElement(subElement, currentBulkReadSet);
        currentBulkElementBacklog.add(subElement);
        currentBulkReadCount++;

        if (currentBulkReadCount > bulkTrigger) {
            processCurrentBuffer();
        }
        return subElement;
    }

    public void processCurrentBuffer() throws IOException, XmlPullParserException,
            InvalidStructureException {
        currentOperatingSet = new HashMap<>();
        performBulkRead(currentBulkReadSet, currentOperatingSet);
        for (TreeElement t : currentBulkElementBacklog) {
            processBufferedElement(t, currentOperatingSet, writeLog);
        }
        performBulkWrite(writeLog);
        clearState();
    }


    protected void clearState() {
        currentBulkElementBacklog.clear();
        currentBulkReadSet.clear();
        currentOperatingSet.clear();
        writeLog.clear();
        currentBulkReadCount = 0;
    }

    @Override
    protected void flush() throws IOException, XmlPullParserException,
            InvalidStructureException {
        processCurrentBuffer();
    }


    @Override
    protected void commit(TreeElement parsed) throws IOException, InvalidStructureException {

    }

    /**
     * Perform checks on the Root Element Name and attributes to establish that this parser is
     * handling the correct element
     *
     * MUST NOT SEEK BEYOND THE CURRENT ELEMENT
     *
     * @throws InvalidStructureException If the parser was passed an element it cannot parse, or
     *                                   that element has a structure which is incorrect based on the root element or attributes
     */
    protected abstract void preParseValidate() throws InvalidStructureException;

    /**
     * For the provided tree element, gather any models which this parser will need to read in
     * order to process the tree element into a writable object
     *
     * @param currentBulkReadSet Models which will be read in the next bulk read. This method should
     *                           add any needed model's ID's to this set.
     */
    protected abstract void requestModelReadsForElement(TreeElement bufferedTreeElement, Set<String> currentBulkReadSet) throws InvalidStructureException;

    /**
     * Read any/all models that have been requested, and loads them into the currentOperatingSet
     * provided. Any ID's which don't match a valid object won't be loaded into the operating set.
     *
     * This step is fired when a bulk process is requested, and begins the bulk processing phase
     *
     * @param currentBulkReadSet  A list of ID's to be read in bulk
     * @param currentOperatingSet the destination mapping from each ID to a matching model
     *                            (if one exists)
     */
    protected abstract void performBulkRead(Set<String> currentBulkReadSet, Map<String, T> currentOperatingSet) throws InvalidStructureException, IOException, XmlPullParserException;

    /**
     * Process the buffered element into a transaction to be written in the next bulk write.
     *
     * This method should add the element to the writeLog after creating its model.
     *
     * This method is fired after the bulk read and is part of the bulk processing phase
     *
     * IMPORTANT - If the processed element is a member of the currentOperatingSet, it should be
     * written _both_ to the currentOperatingSet _as well as_ the writeLog.
     *
     * @param bufferedTreeElement the element to be processed
     * @param currentOperatingSet The operating set of data from the bulk read and other
     *                            processed elements
     * @param writeLog            A list of models to be written during hte bulk write, this method should add
     *                            the processed model to this list.
     */
    protected abstract void processBufferedElement(TreeElement bufferedTreeElement, Map<String, T> currentOperatingSet, SortedMap<String, T> writeLog) throws InvalidStructureException;

    /**
     * Writes the list of buffered models into storage as efficiently as possible.
     *
     * This call of this method ends the bulk processing phase.
     *
     * @param writeLog A list of models to be processed into storage, keyed by their unique ID
     */
    protected abstract void performBulkWrite(SortedMap<String, T> writeLog) throws IOException;

}
