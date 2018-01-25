package org.commcare.util.cli;

import org.commcare.cases.util.CaseDBUtils;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.engine.CommCareConfigEngine;
import org.commcare.util.mocks.CLISessionWrapper;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Screen;
import org.commcare.util.screen.SessionUtils;
import org.commcare.util.screen.SyncScreen;
import org.javarosa.core.model.User;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.engine.XFormPlayer;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * CLI host for running a commcare application which has been configured and instatiated
 * for the provided user.
 *
 * @author ctsims
 */
public class ApplicationHost {
    private final CommCareConfigEngine mEngine;
    private final CommCarePlatform mPlatform;
    private UserSandbox mSandbox;
    private CLISessionWrapper mSession;

    private boolean mUpdatePending = false;
    private String mUpdateTarget = null;
    private boolean mSessionHasNextFrameReady = false;

    private final PrototypeFactory mPrototypeFactory;

    private final BufferedReader reader;

    private String username;
    private String qualifiedUsername;
    private String password;
    private String mRestoreFile;
    private boolean mRestoreStrategySet = false;

    public ApplicationHost(CommCareConfigEngine engine, PrototypeFactory prototypeFactory) {
        this.mEngine = engine;
        this.mPlatform = engine.getPlatform();

        reader = new BufferedReader(new InputStreamReader(System.in));
        this.mPrototypeFactory = prototypeFactory;
    }

    public void setRestoreToRemoteUser(String username, String password) {
        this.username = username;
        this.password = password;
        String domain = mPlatform.getPropertyManager().getSingularProperty("cc_user_domain");
        this.qualifiedUsername = username + "@" + domain;
        mRestoreStrategySet = true;
    }

    public void setRestoreToLocalFile(String filename) {
        this.mRestoreFile = filename;
        mRestoreStrategySet = true;
    }

    public void setRestoreToDemoUser() {
        mRestoreStrategySet = true;
    }

    public void run() {
        if (!mRestoreStrategySet) {
            throw new RuntimeException("You must set up an application host by calling " +
                    "one of hte setRestore*() methods before running the app");
        }
        setupSandbox();

        mSession = new CLISessionWrapper(mPlatform, mSandbox);

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
        mEngine.attemptAppUpdate(updateTarget);
    }

    private boolean loopSession() throws IOException {
        Screen s = getNextScreen();
        boolean screenIsRedrawing = false;

        boolean sessionIsLive = true;
        while (sessionIsLive) {
            while (s != null) {
                try {
                    if (!screenIsRedrawing) {
                        s.init(mSession);

                        if (s.shouldBeSkipped()) {
                            s = getNextScreen();
                            continue;
                        }
                    }

                    System.out.println("\n\n\n\n\n\n");
                    System.out.println(s.getWrappedDisplaytitle(mSandbox, mPlatform));

                    System.out.println("====================");
                    s.prompt(System.out);
                    System.out.print("> ");

                    screenIsRedrawing = false;
                    String input = reader.readLine();

                    //TODO: Command language
                    if (input.startsWith(":")) {
                        if (input.equals(":exit") || input.equals(":quit")) {
                            return false;
                        }
                        if (input.startsWith(":update")) {
                            mUpdatePending = true;

                            if (input.contains(("--latest")) || input.contains("-f")) {
                                mUpdateTarget = "build";
                                System.out.println("Updating to most recent build");
                            } else if (input.contains(("--preview")) || input.contains("-p")) {
                                mUpdateTarget = "save";
                                System.out.println("Updating to latest app preview");
                            } else {
                                mUpdateTarget = "release";
                                System.out.println("Updating to newest Release");
                            }
                            return true;
                        }

                        if (input.equals(":home")) {
                            return true;
                        }

                        if (input.equals(":back")) {
                            mSession.stepBack(mSession.getEvaluationContext());
                            s = getNextScreen();
                            continue;
                        }

                        if (input.equals(":stack")) {
                            printStack(mSession);

                            continue;
                        }

                        if (input.startsWith(":lang")) {
                            String[] langArgs = input.split(" ");
                            if (langArgs.length != 2) {
                                System.out.println("Command format\n:lang [langcode]");
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

                    screenIsRedrawing = s.handleInputAndUpdateSession(mSession, input);
                    if (!screenIsRedrawing) {
                        s = getNextScreen();
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

            System.out.println("Starting form entry with the following stack frame");
            printStack(mSession);
            //Get our form object
            String formXmlns = mSession.getForm();

            if (formXmlns == null) {
                finishSession();
                return true;
            } else {
                XFormPlayer player = new XFormPlayer(System.in, System.out, null);
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
                    s = getNextScreen();
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
        System.out.println("Live Frame");
        System.out.println("----------");
        for (StackFrameStep step : frame.getSteps()) {
            if (step.getType().equals(SessionFrame.STATE_COMMAND_ID)) {
                System.out.println("COMMAND: " + step.getId());
            } else {
                System.out.println("DATUM : " + step.getId() + " - " + step.getValue());
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
        System.out.println(error);
        e.printStackTrace();
        System.out.println("Press return to restart the session");
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
            return new EntityScreen();
        } else if (next.equals(SessionFrame.STATE_QUERY_REQUEST)) {
            return new QueryScreen(qualifiedUsername, password, System.out);
        } else if (next.equals(SessionFrame.STATE_SYNC_REQUEST)) {
            return new SyncScreen(qualifiedUsername, password, System.out);
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum();
            return getNextScreen();
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
            mSession.setDatum("", "awful");
        } else {
            try {
                mSession.setDatum(datum.getDataId(), FunctionUtils.toString(form.eval(ec)));
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
        if (username != null && password != null) {
            SessionUtils.restoreUserToSandbox(mSandbox, mSession, mPlatform, username, password);
        } else if (mRestoreFile != null) {
            restoreFileToSandbox(mSandbox, mRestoreFile);
        } else {
            restoreDemoUserToSandbox(mSandbox);
        }
    }

    private void restoreFileToSandbox(UserSandbox sandbox, String restoreFile) {
        FileInputStream fios = null;
        try {
            System.out.println("Restoring user data from local file " + restoreFile);
            fios = new FileInputStream(restoreFile);
        } catch (FileNotFoundException e) {
            System.out.println("No restore file found at" + restoreFile);
            System.exit(-1);
        }
        try {
            ParseUtils.parseIntoSandbox(new BufferedInputStream(fios), sandbox, false);
        } catch (Exception e) {
            System.out.println("Error parsing local restore data from " + restoreFile);
            e.printStackTrace();
            System.exit(-1);
        }

        initUser();
    }

    private void initUser() {
        User u = mSandbox.getUserStorage().read(0);
        mSandbox.setLoggedInUser(u);
        System.out.println("Setting logged in user to: " + u.getUsername());
    }

    private void restoreDemoUserToSandbox(UserSandbox sandbox) {
        try {
            ParseUtils.parseIntoSandbox(mPlatform.getDemoUserRestore().getRestoreStream(), sandbox, false);
        } catch (Exception e) {
            System.out.println("Error parsing demo user restore from app");
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

        System.out.println("Locale '" + locale + "' is undefined in this app! Available Locales:");
        System.out.println("---------------------");
        System.out.println(availableLocales);
    }

    private void syncAndReport() {
        performCasePurge(mSandbox);

        if (username != null && password != null) {
            System.out.println("Requesting sync...");
            SessionUtils.restoreUserToSandbox(mSandbox, mSession, mPlatform, username, password);
        } else {
            System.out.println("Syncing is only available when using raw user credentials");
        }
    }

    public static void performCasePurge(UserSandbox sandbox) {
        System.out.println("Performing Case Purge");
        CasePurgeFilter purger = new CasePurgeFilter(sandbox.getCaseStorage(),
                SandboxUtils.extractEntityOwners(sandbox));

        int removedCases = sandbox.getCaseStorage().removeAll(purger).size();

        System.out.println("");
        System.out.println("Purge Report");
        System.out.println("=========================");
        if (removedCases == 0) {
            System.out.println("0 Cases Purged");
        } else {
            System.out.println("Cases Removed from device[" + removedCases + "]: " +
                    purger.getRemovedCasesString());
        }
        if (!("".equals(purger.getRemovedCasesString()))) {
            System.out.println("[Error/Warning] Cases Missing from Device: " + purger.getMissingCasesString());
        }
        if (purger.invalidEdgesWereRemoved()) {
            System.out.println("[Error/Warning] During Purge Invalid Edges were Detected");
        }
    }

}
