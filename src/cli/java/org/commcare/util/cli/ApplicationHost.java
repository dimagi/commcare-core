package org.commcare.util.cli;

import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.cases.util.InvalidCaseGraphException;
import org.commcare.core.interfaces.MemoryVirtualDataInstanceStorage;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.engine.CommCareConfigEngine;
import org.commcare.util.mocks.CLISessionWrapper;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityListSubscreen;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.EntityScreenContext;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.MultiSelectEntityScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Screen;
import org.commcare.util.screen.SessionUtils;
import org.commcare.util.screen.SyncScreen;
import org.javarosa.core.model.User;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.engine.XFormPlayer;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

import static org.commcare.util.screen.MultiSelectEntityScreen.USE_SELECTED_VALUES;

/**
 * CLI host for running a commcare application which has been configured and instatiated
 * for the provided user.
 *
 * @author ctsims
 */
public class ApplicationHost {
    private final CommCareConfigEngine mEngine;
    private final CommCarePlatform mPlatform;
    private MockUserDataSandbox mSandbox;
    private CLISessionWrapper mSession;

    private boolean mUpdatePending = false;
    private String mUpdateTarget = null;
    private boolean mSessionHasNextFrameReady = false;

    private final PrototypeFactory mPrototypeFactory;

    private final BufferedReader reader;
    private final PrintStream printStream;

    private String username;
    private String qualifiedUsername;
    private String password;
    private String mRestoreFile;
    private String mRestoreStrategy = null;

    private SessionUtils mSessionUtils = new SessionUtils();

    private VirtualDataInstanceStorage virtualInstanceStorage = new MemoryVirtualDataInstanceStorage();

    public ApplicationHost(CommCareConfigEngine engine,
                           PrototypeFactory prototypeFactory,
                           BufferedReader reader,
                           PrintStream out) {
        this.mEngine = engine;
        this.mPlatform = engine.getPlatform();
        this.reader = reader;
        this.mPrototypeFactory = prototypeFactory;
        this.printStream = out;
    }

    public ApplicationHost(CommCareConfigEngine engine, PrototypeFactory prototypeFactory) {
        this(engine, prototypeFactory, new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    public void setRestoreToRemoteUser() {
        mRestoreStrategy = "remote";
        checkUsernamePasswordValid();
    }

    private void checkUsernamePasswordValid() {
        if (this.username == null || this.password == null) {
            throw new RuntimeException("username and password required");
        }
    }

    public void setUsernamePassword(String username, String password) {
        this.username = username;
        this.password = password;
        String domain = mPlatform.getPropertyManager().getSingularProperty("cc_user_domain");
        this.qualifiedUsername = username + "@" + domain;
    }

    public void setRestoreToLocalFile(String filename) {
        this.mRestoreFile = filename;
        mRestoreStrategy = "file";
    }

    public void setRestoreToDemoUser() {
        mRestoreStrategy = "demo";
    }

    public void advanceSessionWithEndpoint(String endpointId, String[] endpointArgs) {
        if (endpointId == null) {
            return;
        }

        Endpoint endpoint = mPlatform.getEndpoint(endpointId);
        if (endpoint == null) {
            throw new RuntimeException(endpointId + " not found");
        }
        if (endpointArgs == null) {
            endpointArgs = new String[0];
        }

        mSession.clearAllState();
        mSession.clearVolatiles();

        EvaluationContext evalContext = mSession.getEvaluationContext();
        try {
            Endpoint.populateEndpointArgumentsToEvaluationContext(endpoint, new ArrayList<String>(Arrays.asList(endpointArgs)), evalContext);
        } catch (Endpoint.InvalidEndpointArgumentsException e) {
            String missingMessage = "";
            if (e.hasMissingArguments()) {
                missingMessage = String.format(" Missing arguments: %s.", String.join(", ", e.getMissingArguments()));
            }
            String unexpectedMessage = "";
            if (e.hasUnexpectedArguments()) {
                unexpectedMessage = String.format(" Unexpected arguments: %s.", String.join(", ", e.getUnexpectedArguments()));
            }
            throw new RuntimeException("Invalid arguments for endpoint." + missingMessage + unexpectedMessage);
        }

        for (StackOperation op : endpoint.getStackOperations()) {
            mSession.executeStackOperations(new Vector<>(Arrays.asList(op)), evalContext);
            Screen s = getNextScreen();
            if (s instanceof SyncScreen) {
                try {
                    s.init(mSession);
                    s.handleInputAndUpdateSession(mSession, "", false, null);
                } catch (CommCareSessionException ccse) {
                    printErrorAndContinue("Error during session execution:", ccse);
                }
            }
        }
        mSessionHasNextFrameReady = true;
    }

    public void run(String endpointId, String[] endpointArgs) {
        if (mRestoreStrategy == null) {
            throw new RuntimeException("You must set up an application host by calling " +
                    "one of the setRestore*() methods before running the app");
        }
        setupSandbox();

        mSession = new CLISessionWrapper(mPlatform, mSandbox);

        advanceSessionWithEndpoint(endpointId, endpointArgs);

        try {
            loop();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void loop() throws IOException {
        boolean keepExecuting = true;
        while (keepExecuting) {
            if (!mSessionHasNextFrameReady) {
                mSession.clearAllState();
            }
            mSessionHasNextFrameReady = false;
            keepExecuting = loopSession();

            if (this.mUpdatePending) {
                processAppUpdate();
            }
        }
    }

    private void processAppUpdate() {
        mSession.clearAllState();
        this.mUpdatePending = false;
        String updateTarget = mUpdateTarget;
        this.mUpdateTarget = null;
        try {
            mEngine.attemptAppUpdate(updateTarget, username);
        } catch (UnresolvedResourceException e) {
            printStream.println("Update Failed! Couldn't find or install one of the remote resources");
            e.printStackTrace();
        } catch (UnfullfilledRequirementsException e) {
            printStream.println("Update Failed! This CLI host is incompatible with the app");
            e.printStackTrace();
        } catch (InstallCancelledException e) {
            printStream.println("Update Failed! Update was cancelled");
            e.printStackTrace();
        } catch (ResourceInitializationException e) {
            printStream.println("Update Failed! Couldn't initialize one of the resources");
            e.printStackTrace();
        }
    }

    private boolean loopSession() throws IOException {
        Screen screen = getNextScreen();
        boolean screenIsRedrawing = false;

        boolean sessionIsLive = true;
        while (sessionIsLive) {
            while (screen != null) {
                try {
                    if (!screenIsRedrawing) {
                        screen.init(mSession);

                        if (screen.shouldBeSkipped()) {
                            screen = getNextScreen();
                            continue;
                        }
                    }

                    printStream.println("\n\n\n\n\n\n");
                    printStream.println(screen.getWrappedDisplaytitle(mSandbox, mPlatform));

                    printStream.println("====================");
                    boolean requiresInput = screen.prompt(printStream);
                    screenIsRedrawing = false;
                    String input = "";
                    if (requiresInput) {
                        printStream.print("> ");
                        input = reader.readLine();
                    }

                    //TODO: Command language
                    if (input.startsWith(":")) {
                        if (input.equals(":exit") || input.equals(":quit")) {
                            return false;
                        }
                        if (input.startsWith(":update")) {
                            mUpdatePending = true;

                            if (input.contains(("--latest")) || input.contains("-f")) {
                                mUpdateTarget = "build";
                                printStream.println("Updating to most recent build");
                            } else if (input.contains(("--preview")) || input.contains("-p")) {
                                mUpdateTarget = "save";
                                printStream.println("Updating to latest app preview");
                            } else {
                                mUpdateTarget = "release";
                                printStream.println("Updating to newest Release");
                            }
                            return true;
                        }

                        if (input.equals(":home")) {
                            return true;
                        }

                        if (input.equals(":back")) {
                            mSession.stepBack(mSession.getEvaluationContext());
                            screen = getNextScreen();
                            continue;
                        }

                        if (input.equals(":stack")) {
                            printStack(mSession);

                            continue;
                        }

                        if (input.startsWith(":lang")) {
                            String[] langArgs = input.split(" ");
                            if (langArgs.length != 2) {
                                printStream.println("Command format\n:lang [langcode]");
                                continue;
                            }

                            String newLocale = langArgs[1];
                            setLocale(newLocale);

                            continue;
                        }

                        if (input.startsWith(":sync")) {
                            syncAndReport();
                            continue;
                        }
                    }

                    // When a user selects an entity in the EntityListSubscreen, this sets mCurrentSelection
                    // which ultimately updates the session, so getNextScreen will move onto the form list,
                    // skipping the entity detail. To avoid this, flag that we want to force a redraw in this case.
                    boolean waitForCaseDetail = false;
                    if (screen instanceof MultiSelectEntityScreen) {
                        String[] selectedValues = input.split(",");
                        screenIsRedrawing = screen.handleInputAndUpdateSession(mSession,
                                                                               USE_SELECTED_VALUES,
                                                                               false,
                                                                               selectedValues);
                    } else {
                        if (screen instanceof EntityScreen) {
                            boolean isAction = input.startsWith("action "); // Don't wait for case detail if action
                            EntityScreen eScreen = (EntityScreen)screen;
                            if (!isAction && eScreen.getCurrentScreen() instanceof EntityListSubscreen) {
                                waitForCaseDetail = true;
                            }
                        }
                        screenIsRedrawing = !screen.handleInputAndUpdateSession(mSession, input, false, null);
                    }
                    if (!screenIsRedrawing && !waitForCaseDetail) {
                        screen = getNextScreen();
                        if (screen instanceof EntityScreen) {
                            screen.init(mSession);
                            EntityScreen entityScreen = (EntityScreen)screen;
                            if (entityScreen.evalAndExecuteAutoLaunchAction("", mSession)) {
                                screen = getNextScreen();
                            }
                        }
                    }
                } catch (CommCareSessionException ccse) {
                    printErrorAndContinue("Error during session execution:", ccse);

                    //Restart
                    return true;
                } catch (XPathException xpe) {
                    printErrorAndContinue("XPath Evaluation exception during session execution:", xpe);

                    //Restart
                    return true;
                }
            }
            //We have a session and are ready to fill out a form!

            printStream.println("Starting form entry with the following stack frame");
            printStack(mSession);
            //Get our form object
            String formXmlns = mSession.getForm();

            if (formXmlns == null) {
                finishSession();
                return true;
            } else {
                XFormPlayer player = new XFormPlayer(reader, printStream, null);
                player.setPreferredLocale(Localization.getGlobalLocalizerAdvanced().getLocale());
                player.setSessionIIF(mSession.getIIF());
                player.start(mEngine.loadFormByXmlns(formXmlns));

                //If the form saved properly, process the output
                if (player.getExecutionResult() == XFormPlayer.FormResult.Completed) {
                    if (!processResultInstance(player.getResultStream())) {
                        return true;
                    }
                    finishSession();
                    return true;
                } else if (player.getExecutionResult() == XFormPlayer.FormResult.Cancelled) {
                    mSession.stepBack(mSession.getEvaluationContext());
                    screen = getNextScreen();
                } else {
                    //Handle this later
                    return true;
                }
            }
        }
        //After we finish, continue executing
        return true;
    }

    private void printStack(CLISessionWrapper mSession) {
        SessionFrame frame = mSession.getFrame();
        printStream.println("Live Frame");
        printStream.println("----------");
        for (StackFrameStep step : frame.getSteps()) {
            if (step.getType().equals(SessionFrame.STATE_COMMAND_ID)) {
                printStream.println("COMMAND: " + step.getId());
            } else {
                printStream.println("DATUM : " + step.getId() + " - " + step.getValue());
            }
        }
    }

    private void finishSession() {
        mSession.clearVolatiles();
        if (mSession.finishExecuteAndPop(mSession.getEvaluationContext())) {
            mSessionHasNextFrameReady = true;
        }
    }

    private boolean processResultInstance(InputStream resultStream) {
        try {
            DataModelPullParser parser = new DataModelPullParser(
                    resultStream, new CommCareTransactionParserFactory(mSandbox), true, true);
            parser.parse();
        } catch (Exception e) {
            printErrorAndContinue("Error processing the form result!", e);
            return false;
        } finally {
            try {
                resultStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void printErrorAndContinue(String error, Exception e) {
        printStream.println(error);
        e.printStackTrace();
        printStream.println("Press return to restart the session");
        try {
            reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Screen getNextScreen() {
        String next = mSession.getNeededData(mSession.getEvaluationContext());
        if (next == null) {
            //XFORM TIME!
            return null;
        } else if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            return new MenuScreen();
        } else if (next.equals(SessionFrame.STATE_DATUM_VAL)) {
            return new EntityScreen(true);
        } else if (next.equals(SessionFrame.STATE_QUERY_REQUEST)) {
            checkUsernamePasswordValid();
            return new QueryScreen(qualifiedUsername, password, System.out, virtualInstanceStorage, mSessionUtils);
        } else if (next.equals(SessionFrame.STATE_SYNC_REQUEST)) {
            checkUsernamePasswordValid();
            return new SyncScreen(qualifiedUsername, password, System.out, mSessionUtils);
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum();
            return getNextScreen();
        } else if (next.equals(SessionFrame.STATE_MULTIPLE_DATUM_VAL)) {
            try {
                return new MultiSelectEntityScreen(true, true, mSession, virtualInstanceStorage, false,
                        new EntityScreenContext());
            } catch (CommCareSessionException ccse) {
                printErrorAndContinue("Error during session execution:", ccse);
            }
        }
        throw new RuntimeException("Unexpected Frame Request: " + next);
    }

    private void computeDatum() {
        //compute
        SessionDatum datum = mSession.getNeededDatum();
        XPathExpression form;
        try {
            form = XPathParseTool.parseXPath(datum.getValue());
        } catch (XPathSyntaxException e) {
            //TODO: What.
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        EvaluationContext ec = mSession.getEvaluationContext();
        if (datum instanceof FormIdDatum) {
            mSession.setXmlns(FunctionUtils.toString(form.eval(ec)));
            mSession.setEntityDatum("", "awful");
        } else {
            try {
                mSession.setEntityDatum(datum, FunctionUtils.toString(form.eval(ec)));
            } catch (XPathException e) {
                error(e);
            }
        }
    }

    private void error(Exception e) {
        e.printStackTrace();
        System.exit(-1);
    }

    private void setupSandbox() {
        //Set up our storage
        MockUserDataSandbox sandbox = new MockUserDataSandbox(mPrototypeFactory);
        //this gets configured earlier when we installed the app, should point it in the
        //right direction!
        sandbox.setAppFixtureStorageLocation((IStorageUtilityIndexed<FormInstance>)
                mPlatform.getStorageManager().getStorage(FormInstance.STORAGE_KEY));

        mSandbox = sandbox;
        if (mRestoreStrategy == "remote") {
            mSessionUtils.restoreUserToSandbox(mSandbox, mSession, mPlatform, username, password, System.out);
        } else if (mRestoreStrategy == "file" && mRestoreFile != null) {
            restoreFileToSandbox(mSandbox, mRestoreFile);
        } else if (mRestoreStrategy == "demo") {
            restoreDemoUserToSandbox(mSandbox);
        } else {
            throw new RuntimeException("Unknown restore strategy " + mRestoreStrategy);
        }
    }

    private void restoreFileToSandbox(UserSandbox sandbox, String restoreFile) {
        FileInputStream fios = null;
        try {
            printStream.println("Restoring user data from local file " + restoreFile);
            fios = new FileInputStream(restoreFile);
        } catch (FileNotFoundException e) {
            printStream.println("No restore file found at" + restoreFile);
            System.exit(-1);
        }
        try {
            ParseUtils.parseIntoSandbox(new BufferedInputStream(fios), sandbox, false);
        } catch (Exception e) {
            printStream.println("Error parsing local restore data from " + restoreFile);
            e.printStackTrace();
            System.exit(-1);
        }

        initUser();
    }

    private void initUser() {
        User u = mSandbox.getUserStorage().read(0);
        mSandbox.setLoggedInUser(u);
        printStream.println("Setting logged in user to: " + u.getUsername());
    }

    private void restoreDemoUserToSandbox(UserSandbox sandbox) {
        try {
            ParseUtils.parseIntoSandbox(mPlatform.getDemoUserRestore().getRestoreStream(), sandbox, false);
        } catch (Exception e) {
            printStream.println("Error parsing demo user restore from app");
            e.printStackTrace();
            System.exit(-1);
        }

        initUser();
    }

    private void setLocale(String locale) {
        Localizer localizer = Localization.getGlobalLocalizerAdvanced();

        String availableLocales = "";

        for (String availabile : localizer.getAvailableLocales()) {
            availableLocales += availabile + "\n";
            if (locale.equals(availabile)) {
                localizer.setLocale(locale);

                return;
            }
        }

        printStream.println("Locale '" + locale + "' is undefined in this app! Available Locales:");
        printStream.println("---------------------");
        printStream.println(availableLocales);
    }

    private void syncAndReport() {
        performCasePurge(mSandbox);
        if (username != null && password != null) {
            System.out.println("Requesting sync...");
            mSessionUtils.restoreUserToSandbox(mSandbox, mSession, mPlatform, username, password, System.out);
        } else {
            printStream.println("Syncing is only available when using raw user credentials");
        }
    }

    public void performCasePurge(UserSandbox sandbox) {
        printStream.println("Performing Case Purge");
        CasePurgeFilter purger = null;
        try {
            purger = new CasePurgeFilter(sandbox.getCaseStorage(),
                    SandboxUtils.extractEntityOwners(sandbox));
        } catch (InvalidCaseGraphException e) {
            printStream.println(e.getMessage());
            return;
        }

        int removedCases = sandbox.getCaseStorage().removeAll(purger).size();

        printStream.println("");
        printStream.println("Purge Report");
        printStream.println("=========================");
        if (removedCases == 0) {
            printStream.println("0 Cases Purged");
        } else {
            printStream.println("Cases Removed from device[" + removedCases + "]: " +
                    purger.getRemovedCasesString());
        }
        if (!("".equals(purger.getRemovedCasesString()))) {
            printStream.println("[Error/Warning] Cases Missing from Device: " + purger.getMissingCasesString());
        }
        if (purger.invalidEdgesWereRemoved()) {
            printStream.println("[Error/Warning] During Purge Invalid Edges were Detected");
        }
    }

    public void setSessionUtils(SessionUtils sessionUtils) {
        mSessionUtils = sessionUtils;
    }
}
