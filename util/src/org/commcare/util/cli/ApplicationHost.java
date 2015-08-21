package org.commcare.util.cli;

import org.commcare.core.interfaces.UserDataInterface;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.suite.model.SessionDatum;
import org.javarosa.core.model.User;
import org.commcare.util.CommCareConfigEngine;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.SessionFrame;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.engine.XFormPlayer;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
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
    private final String mUsername;
    private final String mPassword;
    private UserDataInterface mSandbox;
    private SessionWrapper mSession;

    private boolean mUpdatePending = false;
    private boolean mForceLatestUpdate = false;
    private boolean mSessionHasNextFrameReady = false;


    private final PrototypeFactory mPrototypeFactory;

    private final BufferedReader reader;

    public ApplicationHost(CommCareConfigEngine engine, String username, String password, PrototypeFactory prototypeFactory) {
        this.mUsername = username;
        this.mPassword = password;
        this.mEngine = engine;
        this.mPlatform = engine.getPlatform();

        reader = new BufferedReader(new InputStreamReader(System.in));
        this.mPrototypeFactory = prototypeFactory;
    }

    public void run() {
        setupSandbox();

        mSession = new SessionWrapper(mPlatform, mSandbox);

        try {
            loop();
        }catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    private void loop() throws IOException {
        boolean keepExecuting = true;
        while (keepExecuting) {
            if(!mSessionHasNextFrameReady) {
                mSession.clearAllState();
            }
            mSessionHasNextFrameReady = false;
            keepExecuting = loopSession();

            if(this.mUpdatePending) {
               processAppUpdate();
            }
        }
    }

    private void processAppUpdate() {
        mSession.clearAllState();
        this.mUpdatePending = false;
        boolean forceUpdate = mForceLatestUpdate;
        this.mForceLatestUpdate = false;
        mEngine.attemptAppUpdate(forceUpdate);
    }

    private boolean loopSession() throws IOException {
        Screen s = getNextScreen();

        while (s != null) {
            try {
                s.init(mSession);
                System.out.println("\n\n\n\n\n\n");
                s.prompt(System.out);
                System.out.print("> ");

                String input = reader.readLine();

                //TODO: Command language
                if(input.startsWith(":")) {
                    if(input.equals(":exit") || input.equals(":quit")) {
                        return false;
                    }
                    if (input.startsWith(":update")) {
                        mUpdatePending = true;

                        if(input.contains("-f")) {
                            mForceLatestUpdate = true;
                        }
                        return true;
                    }

                    if(input.equals(":home")) {
                        return true;
                    }
                }

                s.updateSession(mSession, input);
                s = getNextScreen();
            } catch (CommCareSessionException ccse) {
                printErrorAndContinue("Error during session execution:", ccse);

                //Restart
                return true;
            }
        }
        //We have a session and are ready to fill out a form!

        //Get our form object
        String formXmlns = mSession.getForm();

        XFormPlayer player = new XFormPlayer(System.in, System.out, null);
        player.setSessionIIF(mSession.getIIF());
        player.start(mEngine.loadFormByXmlns(formXmlns));

        //If the form saved properly, process the output
        if(player.getExecutionResult() == XFormPlayer.FormResult.Completed) {
            if(!processResultInstance(player.getResultStream())) {
                return true;
            }
            mSession.clearVolitiles();
            if(mSession.finishExecuteAndPop(mSession.getEvaluationContext())) {
                mSessionHasNextFrameReady = true;
            }
        }

        //After we finish, continue executing
        return true;
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
            } catch(IOException e) {}
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
        String next = mSession.getNeededData();

        if (next == null) {
            //XFORM TIME!
            return null;
        } else if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            return new MenuScreen();
        } else if (next.equals(SessionFrame.STATE_DATUM_VAL)) {
            return new EntityScreen();
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum();
            return getNextScreen();
        }
        throw new RuntimeException("Unexpected Frame Request: " + mSession.getNeededData());
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
        if (datum.getType() == SessionDatum.DATUM_TYPE_FORM) {
            mSession.setXmlns(XPathFuncExpr.toString(form.eval(ec)));
            mSession.setDatum("", "awful");
        } else {
            try {
                mSession.setDatum(datum.getDataId(), XPathFuncExpr.toString(form.eval(ec)));
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
        mSandbox = new MockUserDataSandbox(mPrototypeFactory);

        //fetch the restore data and set credentials
        String otaRestoreURL = PropertyManager._().getSingularProperty("ota-restore-url") + "?version=2.0";
        String domain = PropertyManager._().getSingularProperty("cc_user_domain");
        final String qualifiedUsername = mUsername + "@" + domain;

        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(qualifiedUsername, mPassword.toCharArray());
            }
        });

        //Go get our sandbox!
        try {
            URL url = new URL(otaRestoreURL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            System.out.println("Restoring user " + this.mUsername + " to domain " + domain);

            ParseUtils.parseIntoSandbox(new BufferedInputStream(conn.getInputStream()), mSandbox);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        //Initialize our User
        for (IStorageIterator<User> iterator = mSandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
            User u = iterator.nextRecord();
            if (mUsername.equalsIgnoreCase(u.getUsername())) {
                mSandbox.setLoggedInUser(u);
            }
        }
    }
}
