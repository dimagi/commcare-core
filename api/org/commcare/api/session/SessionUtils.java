package org.commcare.api.session;

import org.commcare.api.engine.XFormPlayer;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.screens.EntityScreen;
import org.commcare.api.screens.MenuScreen;
import org.commcare.api.screens.Screen;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.javarosa.core.model.User;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by willpride on 10/27/15.
 */
public class SessionUtils {

    public static final String USERNAME = "api-user";

    public static SessionWrapper performInstall(String resourcePath) {
        ApiConfigEngine mEngine = new ApiConfigEngine();
        if (resourcePath.endsWith(".ccz")) {
            mEngine.initFromArchive(resourcePath);
        } else {
            //mEngine.initFromLocalFileResource(resourcePath);
        }
        mEngine.initEnvironment();
        SessionWrapper sessionWrapper = new SessionWrapper(mEngine.getPlatform(), new UserSqlSandbox(USERNAME));
        return sessionWrapper;
    }

    public static void performRestore(UserSandbox sandbox, String restoreFileReference) {
        restoreFileToSandbox(sandbox, restoreFileReference);
    }

    private static void restoreFileToSandbox(UserSandbox sandbox, String restoreFile) {
        System.out.println("Restore file: " + restoreFile);
        FileInputStream fios = null;
        try {
            fios = new FileInputStream(restoreFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            ParseUtils.parseIntoSandbox(new BufferedInputStream(fios), sandbox, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Initialize our User
        for (IStorageIterator<User> iterator = sandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
            User u = iterator.nextRecord();
            sandbox.setLoggedInUser(u);
            System.out.println("Setting logged in user to: " + u.getUsername());
            break;
        }
    }

    public static Screen getNextScreen(SessionWrapper mSession) {
        String next = mSession.getNeededData();

        if (next == null) {
            return null;
        } else if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            return new MenuScreen();
        } else if (next.equals(SessionFrame.STATE_DATUM_VAL)) {
            return new EntityScreen();
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum(mSession);
            return getNextScreen(mSession);
        }
        throw new RuntimeException("Unexpected Frame Request: " + mSession.getNeededData());
    }

    public static void computeDatum(SessionWrapper mSession) {
        //compute
        SessionDatum datum = mSession.getNeededDatum();
        XPathExpression form;
        try {
            form = XPathParseTool.parseXPath(datum.getValue());
        } catch (XPathSyntaxException e) {
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
                e.printStackTrace();
            }
        }
    }

    public static void printStack(SessionWrapper mSession) {
        SessionFrame frame = mSession.getFrame();
        System.out.println("Live Frame" + (frame.getFrameId() == null ? "" : " [" + frame.getFrameId() + "]"));
        System.out.println("----------");
        for(StackFrameStep step : frame.getSteps()) {
            if (step.getType().equals(SessionFrame.STATE_COMMAND_ID)) {
                System.out.println("COMMAND: " + step.getId());
            } else {
                System.out.println("DATUM : " + step.getId() + " - " + step.getValue());
            }
        }
    }

    public static XFormPlayer initFormEntry(SessionWrapper mSession){
        String formXmlns = mSession.getForm();
        XFormPlayer player = new XFormPlayer(System.in, System.out, null, mSession);
        player.setmPreferredLocale(Localization.getGlobalLocalizerAdvanced().getLocale());
        player.setSessionIIF(mSession.getIIF());
        player.start(mSession.loadFormByXmlns(formXmlns));
        return player;
    }

    public static boolean startFormEntry(SessionWrapper mSession) {
        //Get our form object
        String formXmlns = mSession.getForm();
        if (formXmlns == null) {
            //finishSession();
            return true;
        } else {

            XFormPlayer player = new XFormPlayer(System.in, System.out, null, mSession);
            player.setmPreferredLocale(Localization.getGlobalLocalizerAdvanced().getLocale());
            player.setSessionIIF(mSession.getIIF());
            player.start(mSession.loadFormByXmlns(formXmlns));

            //If the form saved properly, process the output
            if (player.getExecutionResult() == XFormPlayer.FormResult.Completed) {
                if (!processResultInstance(mSession, player.getResultStream())) {
                    return true;
                }
                //finishSession();
                return true;
            } else if(player.getExecutionResult() == XFormPlayer.FormResult.Cancelled) {
                mSession.stepBack();
                return false;
            } else {
                //Handle this later
                return true;
            }
        }
    }


    private static boolean processResultInstance(SessionWrapper sessionWrapper, InputStream resultStream) {
        try {
            DataModelPullParser parser = new DataModelPullParser(
                    resultStream, new CommCareTransactionParserFactory(sessionWrapper.getSandbox()), true, true);
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                resultStream.close();
            } catch(IOException e) {}
        }
        return true;
    }
}
