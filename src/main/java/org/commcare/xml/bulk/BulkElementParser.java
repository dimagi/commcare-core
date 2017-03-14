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

        validateElementEntry();
        TreeElement subElement = new TreeElementParser(parser, 0, "bulk_parser").parse();
        expandBulkReadSetForElement(subElement, currentBulkReadSet);
        currentBulkElementBacklog.add(subElement);
        currentBulkReadCount++;

        if(currentBulkReadCount > bulkTrigger) {
            processCurrentBuffer();
        }
        return subElement;
    }

    public void processCurrentBuffer() throws IOException, XmlPullParserException,
            InvalidStructureException {
        currentOperatingSet = new HashMap<>();
        performBulkRead(currentBulkReadSet, currentOperatingSet);
        for(TreeElement t : currentBulkElementBacklog) {
            processBufferedElement(t, currentOperatingSet,writeLog);
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
     * that element has a structure which is incorrect based on the root element or attributes
     */
    protected abstract void validateElementEntry() throws InvalidStructureException;
    protected abstract void expandBulkReadSetForElement(TreeElement subElement, Set<String> currentBulkReadSet) throws InvalidStructureException;
    protected abstract void performBulkRead(Set<String> currentBulkReadSet, Map<String, T> currentOperatingSet) throws InvalidStructureException, IOException, XmlPullParserException;
    protected abstract void processBufferedElement(TreeElement t, Map<String, T> currentOperatingSet, SortedMap<String, T> writeLog) throws InvalidStructureException;
    protected abstract void performBulkWrite(SortedMap<String, T> writeLog) throws IOException;

}
