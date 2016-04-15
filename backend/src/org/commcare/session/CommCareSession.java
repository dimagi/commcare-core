package org.commcare.session;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

/**
 * Before arriving at the Form Entry phase, CommCare applications
 * need to determine what form to enter, and with what pre-requisites.
 *
 * A CommCare Session helps to encapsulate this information by identifying
 * the set of possible entry operations (Every piece of data needed to begin
 * entry) and specifying the operation which would most quickly filter our
 * the set of operations.
 *
 * NOTE: Currently horribly coupled to the platform.
 *
 * @author ctsims
 */
public class CommCareSession {

    private final CommCarePlatform platform;
    private StackFrameStep popped;
    private String currentCmd;

    /**
     * A table of all datums (id --> value) that are currently on the session stack
     */
    private final OrderedHashtable<String, String> collectedDatums;
    private String currentXmlns;

    /**
     * The current session frame data
     */
    public SessionFrame frame;

    /**
     * The stack of pending Frames
     */
    private final Stack<SessionFrame> frameStack;

    public CommCareSession(CommCarePlatform platform) {
        this.platform = platform;
        collectedDatums = new OrderedHashtable<String, String>();
        this.frame = new SessionFrame();
        this.frameStack = new Stack<SessionFrame>();
    }

    /**
     *  Copy constructor
     */
    public CommCareSession(CommCareSession oldCommCareSession) {
        // NOTE: 'platform' is being copied in a shallow manner
        this.platform = oldCommCareSession.platform;

        if (oldCommCareSession.popped != null) {
            this.popped = new StackFrameStep(oldCommCareSession.popped);
        }
        this.currentCmd = oldCommCareSession.currentCmd;
        this.currentXmlns = oldCommCareSession.currentXmlns;
        this.frame = new SessionFrame(oldCommCareSession.frame);

        collectedDatums = new OrderedHashtable<String, String>();
        for (Enumeration e = oldCommCareSession.collectedDatums.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            collectedDatums.put(key, oldCommCareSession.collectedDatums.get(key));
        }

        this.frameStack = new Stack<SessionFrame>();
        // NOTE: can't use for/each due to J2ME build issues w/ Stack
        for (int i = 0; i < oldCommCareSession.frameStack.size(); i++) {
            frameStack.addElement(oldCommCareSession.frameStack.elementAt(i));
        }
    }

    public Vector<Entry> getEntriesForCommand(String commandId) {
        return getEntriesForCommand(commandId, new OrderedHashtable<String, String>());
    }

    /**
     * @param commandId the current command id
     * @param data all of the datums already on the stack
     * @return A list of all of the form entry actions that are possible with the given commandId
     * and the given list of already-collected datums
     */
    private Vector<Entry> getEntriesForCommand(String commandId,
                                               OrderedHashtable<String, String> data) {
        for (Suite s : platform.getInstalledSuites()) {
            for (Menu m : s.getMenus()) {
                // We need to see if everything in this menu can be matched
                if (commandId.equals(m.getId())) {
                    return getEntriesFromMenu(m, data);
                }
            }

            if (s.getEntries().containsKey(commandId)) {
                Vector<Entry> entries = new Vector<Entry>();
                entries.addElement(s.getEntries().get(commandId));
                return entries;
            }
        }

        return new Vector<Entry>();
    }

    private Vector<Entry> getEntriesFromMenu(Menu menu,
                                             OrderedHashtable<String, String> data) {
        Vector<Entry> entries = new Vector<Entry>();
        Hashtable<String, Entry> map = platform.getMenuMap();
        //We're in a menu we have a set of requirements which
        //need to be fulfilled
        for (String cmd : menu.getCommandIds()) {
            Entry e = map.get(cmd);
            if (e == null) {
                throw new RuntimeException("No entry found for menu command [" + cmd + "]");
            }
            boolean valid = true;
            Vector<SessionDatum> requirements = e.getSessionDataReqs();
            if (requirements.size() >= data.size()) {
                for (int i = 0; i < data.size(); ++i) {
                    if (!requirements.elementAt(i).getDataId().equals(data.keyAt(i))) {
                        valid = false;
                    }
                }
            }
            if (valid) {
                entries.addElement(e);
            }
        }
        return entries;
    }

    public OrderedHashtable<String, String> getData() {
        return collectedDatums;
    }

    public CommCarePlatform getPlatform() {
        return this.platform;
    }

    /**
     * Based on the current state of the session, determine what information is needed next to
     * proceed
     *
     * @return 1 of the 4 STATE strings declared at the top of SessionFrame, or null if
     * the session does not need anything else to proceed
     */
    public String getNeededData() {
        if (currentCmd == null) {
            return SessionFrame.STATE_COMMAND_ID;
        }

        Vector<Entry> entries = getEntriesForCommand(currentCmd, collectedDatums);
        String needDatum = getDataNeededByAllEntries(entries);

        if (needDatum != null) {
            return needDatum;
        } else if (entries.size() > 1 || !entries.elementAt(0).getCommandId().equals(currentCmd)) {
            //the only other thing we can need is a form command. If there's
            //still more than one applicable entry, we need to keep going
            return SessionFrame.STATE_COMMAND_ID;
        } else {
            return null;
        }
    }

    /**
     * Checks that all entries have the same id for their first required data,
     * and if so, returns the data's associated session state. Otherwise,
     * returns null.
     */
    private String getDataNeededByAllEntries(Vector<Entry> entries) {
        String datumNeededByAllEntriesSoFar = null;
        String neededDatumId = null;
        for (Entry e : entries) {
            SessionDatum datumNeededForThisEntry =
                getFirstMissingDatum(collectedDatums, e.getSessionDataReqs());
            if (datumNeededForThisEntry != null) {
                if (neededDatumId == null) {
                    neededDatumId = datumNeededForThisEntry.getDataId();
                    if (datumNeededForThisEntry.getNodeset() != null) {
                        datumNeededByAllEntriesSoFar = SessionFrame.STATE_DATUM_VAL;
                    } else {
                        datumNeededByAllEntriesSoFar = SessionFrame.STATE_DATUM_COMPUTED;
                    }
                } else if (!neededDatumId.equals(datumNeededForThisEntry.getDataId())) {
                    // data needed from the first entry isn't consistent with
                    // the current entry
                    return null;
                }
            } else {
                // we don't need any data, or the first data needed isn't
                // consistent across entries
                return null;
            }
        }

        return datumNeededByAllEntriesSoFar;
    }

    public String[] getHeaderTitles() {
        Hashtable<String, String> menus = new Hashtable<String, String>();

        for (Suite s : platform.getInstalledSuites()) {
            for (Menu m : s.getMenus()) {
                menus.put(m.getId(), m.getName().evaluate());
            }
        }

        Vector<StackFrameStep> steps = frame.getSteps();
        String[] returnVal = new String[steps.size()];


        Hashtable<String, Entry> entries = platform.getMenuMap();
        int i = 0;
        for (StackFrameStep step : steps) {
            if (SessionFrame.STATE_COMMAND_ID.equals(step.getType())) {
                //Menu or form.
                if (menus.containsKey(step.getId())) {
                    returnVal[i] = menus.get(step.getId());
                } else if (entries.containsKey(step.getId())) {
                    returnVal[i] = entries.get(step.getId()).getText().evaluate();
                }
            } else if (SessionFrame.STATE_DATUM_VAL.equals(step.getType())) {
                //TODO: Grab the name of the case
            } else if (SessionFrame.STATE_DATUM_COMPUTED.equals(step.getType())) {
                //Nothing to do here
            }

            if (returnVal[i] != null) {
                //Menus contain a potential argument listing where that value is on the screen,
                //clear it out if it exists
                returnVal[i] = Localizer.processArguments(returnVal[i], new String[]{""}).trim();
            }

            ++i;
        }

        return returnVal;
    }

    /**
     * @return The next relevant datum for the current entry. Requires there to be
     * an entry on the stack
     */
    public SessionDatum getNeededDatum() {
        Entry entry = getEntriesForCommand(getCommand()).elementAt(0);
        return getNeededDatum(entry);
    }

    /**
     * @param entry An entry which is consistent as a step on the stack
     * @return A session datum definition if one is pending. Null otherwise.
     */
    public SessionDatum getNeededDatum(Entry entry) {
        return getFirstMissingDatum(getData(), entry.getSessionDataReqs());
    }

    /**
     * Return the first SessionDatum that is in allDatumsNeeded, but is not represented in
     * datumsCollectedSoFar
     */
    private SessionDatum getFirstMissingDatum(OrderedHashtable datumsCollectedSoFar,
                                              Vector<SessionDatum> allDatumsNeeded) {
        for (SessionDatum datum : allDatumsNeeded) {
            if (!datumsCollectedSoFar.containsKey(datum.getDataId())) {
                return datum;
            }
        }
        return null;
    }

    public Detail getDetail(String id) {
        for (Suite s : platform.getInstalledSuites()) {
            Detail d = s.getDetail(id);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    public Menu getMenu(String id) {
        for (Suite suite : platform.getInstalledSuites()) {
            for (Menu m : suite.getMenus()) {
                if (id.equals(m.getId())) {
                    return m;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public Suite getCurrentSuite() {
        for (Suite s : platform.getInstalledSuites()) {
            for (Menu m : s.getMenus()) {
                //We need to see if everything in this menu can be matched
                if (currentCmd.equals(m.getId())) {
                    return s;
                }

                if (s.getEntries().containsKey(currentCmd)) {
                    return s;
                }
            }
        }

        return null;
    }

    public void stepBack() {
        // Pop the first thing off of the stack frame, no matter what
        popSessionFrameStack();

        // Keep popping things off until the value of needed data indicates that we are back to
        // somewhere where we are waiting for user-provided input
        while (this.getNeededData() == null ||
                this.getNeededData().equals(SessionFrame.STATE_DATUM_COMPUTED)) {
            popSessionFrameStack();
        }
    }

    private void popSessionFrameStack() {
        StackFrameStep recentPop = frame.popStep();
        //TODO: Check the "base state" of the frame after popping to see if we invalidated the stack
        syncState();
        popped = recentPop;
    }

    public void setDatum(String keyId, String value) {
        frame.pushStep(new StackFrameStep(SessionFrame.STATE_DATUM_VAL, keyId, value));
        syncState();
    }

    public void setComputedDatum(EvaluationContext ec) throws XPathException {
        SessionDatum datum = getNeededDatum();
        XPathExpression form;
        try {
            form = XPathParseTool.parseXPath(datum.getValue());
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        if (datum.getType() == SessionDatum.DATUM_TYPE_FORM) {
            setXmlns(XPathFuncExpr.toString(form.eval(ec)));
            setDatum("", "awful");
        } else {
            setDatum(datum.getDataId(), XPathFuncExpr.toString(form.eval(ec)));
        }
    }

    public void setXmlns(String xmlns) {
        frame.pushStep(new StackFrameStep(SessionFrame.STATE_FORM_XMLNS, xmlns, null));
        syncState();
    }

    public void setCommand(String commandId) {
        frame.pushStep(new StackFrameStep(SessionFrame.STATE_COMMAND_ID, commandId, null));
        syncState();
    }

    public void syncState() {
        this.collectedDatums.clear();
        this.currentCmd = null;
        this.currentXmlns = null;
        this.popped = null;

        for (StackFrameStep step : frame.getSteps()) {
            if (SessionFrame.STATE_DATUM_VAL.equals(step.getType())) {
                String key = step.getId();
                String value = step.getValue();
                if (key != null && value != null) {
                    collectedDatums.put(key, value);
                }
            } else if (SessionFrame.STATE_COMMAND_ID.equals(step.getType())) {
                this.currentCmd = step.getId();
            } else if (SessionFrame.STATE_FORM_XMLNS.equals(step.getType())) {
                this.currentXmlns = step.getId();
            }
        }
    }

    public StackFrameStep getPoppedStep() {
        return popped;
    }

    public String getForm() {
        if (this.currentXmlns != null) {
            return this.currentXmlns;
        }
        String command = getCommand();
        if (command == null) {
            return null;
        }

        Entry e = platform.getMenuMap().get(command);
        if (e.isView()) {
            return null;
        } else {
            return ((FormEntry)e).getXFormNamespace();
        }
    }

    public String getCommand() {
        return this.currentCmd;
    }

    /**
     * Clear the current stack frame and release any pending
     * stack frames (completely clearing the session)
     */
    public void clearAllState() {
        frame = new SessionFrame();
        frameStack.removeAllElements();
        syncState();
    }

    /**
     * Retrieve an evaluation context in which to evaluate expressions in the
     * current session state
     *
     * @param iif the instance initailzier for the current platform
     * @return Evaluation context for current session state
     */
    public EvaluationContext getEvaluationContext(InstanceInitializationFactory iif) {
        return this.getEvaluationContext(iif, getCommand());
    }

    /**
     * Retrieve an evaluation context in which to evaluate expressions in the context of a given
     * command in the installed app
     *
     * @param iif the instance initializer for the current platform
     * @return Evaluation context for a command in the installed app
     */
    public EvaluationContext getEvaluationContext(InstanceInitializationFactory iif, String command) {

        if (command == null) {
            return new EvaluationContext(null);
        }
        Entry entry = getEntriesForCommand(command).elementAt(0);

        Hashtable<String, DataInstance> instances = entry.getInstances();

        for (Enumeration en = instances.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();
            instances.put(key, instances.get(key).initialize(iif, key));
        }

        return new EvaluationContext(null, instances);
    }

    public SessionFrame getFrame() {
        //TODO: Type safe copy
        return frame;
    }

    /**
     * Executes a set of stack operations against the current session environment.
     *
     * The context data and session data provided will consistently match the live frame
     * when the operations began executing, although frame operations will be executed
     * against the most recent frame. (IE: If a new frame is pushed here, xpath expressions
     * calculated within it will be evaluated against the starting, but <push> actions
     * will happen against the newly pushed frame)
     */
    public boolean executeStackOperations(Vector<StackOperation> ops, EvaluationContext ec) {
        //the on deck frame is the frame that is the target of operations that execute
        //as part of this stack update. If at the end of the stack ops the frame on deck
        //doesn't match the current (living) frame, it will become the the current frame
        SessionFrame onDeck = frame;

        //Whether the current frame is on the stack (we wanna treat it as a "phantom" bottom element
        //at first, basically.
        boolean currentFramePushed = false;

        for (StackOperation op : ops) {
            //First, see if there is a frame with a matching ID for this op
            //(relevant for a couple reasons, and possibly prevents a costly XPath lookup)
            String frameId = op.getFrameId();
            SessionFrame matchingFrame = null;
            if (frameId != null) {
                //TODO: This is correct, right? We want to treat the current frame
                //as part of the "environment" and not let people create a new frame
                //with the same id? Possibly this should only be true if the current
                //frame is live?
                if (frameId.equals(frame.getFrameId())) {
                    matchingFrame = frame;
                } else {
                    //Otherwise, peruse the stack looking for another
                    //frame with a matching ID.
                    for (Enumeration e = frameStack.elements(); e.hasMoreElements(); ) {
                        SessionFrame stackFrame = (SessionFrame)e.nextElement();
                        if (frameId.equals(stackFrame.getFrameId())) {
                            matchingFrame = stackFrame;
                            break;
                        }
                    }
                }
            }

            boolean newFrame = false;
            switch (op.getOp()) {
                //Note: the Create step and Push step utilize the same code,
                //and the create step does some setup first
                case StackOperation.OPERATION_CREATE:
                    //First make sure we have no existing frames with this ID
                    if (matchingFrame != null) {
                        //If we do, just bail.
                        continue;
                    }
                    //Otherwise, create our new frame (we'll only manipulate it
                    //and add it if it is triggered)
                    matchingFrame = new SessionFrame(frameId);

                    //Ok, now fall through to the push case using that frame,
                    //as the push operations are ~identical
                    newFrame = true;
                case StackOperation.OPERATION_PUSH:
                    //Ok, first, see if we need to execute this op
                    if (!op.isOperationTriggered(ec)) {
                        //Otherwise, we're done.
                        continue;
                    }

                    //If we don't have a frame yet, this push is targeting the
                    //frame on deck
                    if (matchingFrame == null) {
                        matchingFrame = onDeck;
                    }

                    //Now, execute the steps in this operation
                    for (StackFrameStep step : op.getStackFrameSteps()) {
                        matchingFrame.pushStep(step.defineStep(ec));
                    }

                    //ok, frame should be appropriately modified now.
                    //we also need to push this frame if it's new
                    if (newFrame) {
                        //Before we can push a frame onto the stack, we need to
                        //make sure the stack is clean. This means that if the
                        //current frame has a snapshot, we've gotta make sure
                        //the existing frames are still valid.

                        //TODO: We might want to handle this differently in the future,
                        //so that we can account for the invalidated frames in the ui
                        //somehow.
                        cleanStack();

                        //OK, now we want to take the current frame and put it up on the frame stack unless
                        //this frame is dead (IE: We're closing it out). then we'll push the new frame
                        //on top of it.
                        if (!frame.isDead() && !currentFramePushed) {
                            frameStack.push(frame);
                            currentFramePushed = true;
                        }

                        frameStack.push(matchingFrame);
                    }
                    break;
                case StackOperation.OPERATION_CLEAR:
                    if (matchingFrame != null) {
                        if (op.isOperationTriggered(ec)) {
                            frameStack.removeElement(matchingFrame);
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("Undefined stack operation: " + op.getOp());
            }
        }

        //All stack ops executed. Now we need to see if we're on the right frame.
        if (!this.frame.isDead() && frame != onDeck) {
            //If the current frame isn't dead, and isn't on deck, that means we've pushed
            //in new frames and need to load up the correct one

            if (!finishAndPop()) {
                //Somehow we didn't end up with any frames after that? that's incredibly weird, I guess
                //we should just start over.
                this.clearAllState();
            }
            return true;
        }
        //otherwise we still want to make sure we sync
        this.syncState();
        return false;
    }

    /**
     * Checks to see if the current frame has a clean snapshot. If
     * not, clears the stack and the snapshot (since the snapshot can
     * only be relevant to the existing frames)
     */
    private void cleanStack() {
        //See whether the current frame was incompatible with its start
        //state.
        if (frame.isSnapshotIncompatible()) {
            //If it is, our frames can no longer make sense.
            this.frameStack.removeAllElements();
            frame.clearSnapshot();
        }
    }

    /**
     * Called after a session has been completed. Executes and pending stack operations
     * from the current session, completes the session, and pops the top of any pending
     * frames into execution.
     *
     * @return True if there was a pending frame and it has been
     * popped into the current session. False if the stack was empty
     * and the session is over.
     */
    public boolean finishExecuteAndPop(EvaluationContext ec) {
        Vector<StackOperation> ops = getCurrentEntry().getPostEntrySessionOperations();

        //Let the session know that the current frame shouldn't work its way back onto the stack
        markCurrentFrameForDeath();

        //First, see if we have operations to run
        if(ops.size() > 0) {
            executeStackOperations(ops, ec);
        }
        return finishAndPop();
    }

    /**
     * Complete the current session (and perform any cleanup), then
     * check the stack for any pending frames, and load the top one
     * into the current session if so.
     *
     * @return True if there was a pending frame and it has been
     * popped into the current session. False if the stack was empty
     * and the session is over.
     */
    private boolean finishAndPop() {
        cleanStack();

        if (frameStack.empty()) {
            return false;
        } else {
            frame = frameStack.pop();
            //Ok, so if _after_ popping from the stack, we still have
            //stack members, we need to be careful about making sure
            //that they won't get triggered if we abandon the current
            //frame
            if (!frameStack.isEmpty()) {
                frame.captureSnapshot();
            }

            syncState();
            return true;
        }
    }

    /**
     * Retrieve the single valid entry for the current session, should be called only
     * when the current request is fully built
     *
     * @return The unique valid entry built on this session. Will throw an exception if there isn't
     * a unique entry.
     */
    public Entry getCurrentEntry() {
        Vector<Entry> e = getEntriesForCommand(getCommand());
        if (e.size() > 1) {
            throw new IllegalStateException("The current session does not contain a single valid entry");
        }
        if (e.size() == 0) {
            throw new IllegalStateException("The current session has no valid entry");
        }
        return e.elementAt(0);
    }

    /**
     * Retrieves a valid datum definition in the current session's history
     * which contains a selector for the datum Id provided.
     *
     * Can be used to resolve the context about an item that
     * has been selected in this session.
     *
     * @param datumId The ID of a session datum in the session history
     * @return An Entry object which contains a selector for that datum
     * which is in this session history
     */
    public SessionDatum findDatumDefinition(String datumId) {
        //We're performing a walk down the entities in this session here,
        //we should likely generalize this to make it easier to do it for other
        //operations

        Vector<StackFrameStep> steps = frame.getSteps();

        int stepId = -1;
        //walk to our datum
        for (int i = 0; i < steps.size(); ++i) {
            if (SessionFrame.STATE_DATUM_VAL.equals(steps.elementAt(i).getType()) &&
                    steps.elementAt(i).getId().equals(datumId)) {
                stepId = i;
                break;
            }
        }
        if (stepId == -1) {
            System.out.println("I don't think this should be possible...");
            return null;
        }

        //ok, so now we have our step, we want to walk backwards until we find the entity
        //associated with our ID
        for (int i = stepId; i >= 0; i--) {
            if (steps.elementAt(i).getType().equals(SessionFrame.STATE_COMMAND_ID)) {
                Vector<Entry> entries = this.getEntriesForCommand(steps.elementAt(i).getId());

                //TODO: Don't we know the right entry? What if our last command is an actual entry?
                for (Entry entry : entries) {
                    for (SessionDatum datum : entry.getSessionDataReqs()) {
                        if (datum.getDataId().equals(datumId)) {
                            return datum;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void markCurrentFrameForDeath() {
        frame.kill();
    }

    /**
     * Does the command only have a view entry, and no other actions available
     * to take?
     */
    public boolean isViewCommand(String command) {
        Vector<Entry> entries = this.getEntriesForCommand(command);
        return entries.elementAt(0).isView();
    }

    public void addExtraToCurrentFrameStep(String key, String value) {
        frame.addExtraTopStep(key, value);
    }

    public String getCurrentFrameStepExtra(String key) {
        return frame.getTopStepExtra(key);
    }

    /**
     * Builds a session from by restoring serialized SessionFrame and syncing
     * from that. Doesn't support restoring the frame stack
     */
    public static CommCareSession restoreSessionFromStream(CommCarePlatform ccPlatform,
                                                           DataInputStream inputStream)
            throws DeserializationException, IOException {
        SessionFrame restoredFrame = new SessionFrame();
        restoredFrame.readExternal(inputStream, ExtUtil.defaultPrototypes());

        CommCareSession restoredSession = new CommCareSession(ccPlatform);
        restoredSession.frame = restoredFrame;
        restoredSession.syncState();

        return restoredSession;
    }

    public void serializeSessionState(DataOutputStream outputStream) throws IOException {
        frame.writeExternal(outputStream);
    }
}
