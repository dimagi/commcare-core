/**
 * 
 */
package org.commcare.util.cli;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCareConfigEngine;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.SessionFrame;
import org.commcare.util.mocks.LivePrototypeFactory;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.SessionWrapper;
import org.commcare.util.mocks.User;
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

/**
 * @author ctsims
 *
 */
public class ApplicationHost {
    CommCareConfigEngine mEngine;
    CommCarePlatform mPlatform;
    String mUsername;
    String mPassword;
    MockUserDataSandbox mSandbox;
    SessionWrapper mSession;
    
    LivePrototypeFactory mPrototypeFactory = new LivePrototypeFactory();
    
    BufferedReader reader;
    
    public ApplicationHost(CommCareConfigEngine engine, String username, String password) {
        this.mUsername = username;
        this.mPassword = password;
        this.mEngine = engine;
        
        this.mPlatform = engine.getPlatform();
        
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void run() {
        setupSandbox();
        
        mSession = new SessionWrapper(mPlatform, mSandbox);
        
        loop();
    }
    
    
    private void loop() {        
        while(true) {
            mSession.clearAllState();
            loopSession();
        }
    }
    
    private void loopSession() {
        Screen s = getNextScreen();
        
        while(s != null) {
            s.init(mPlatform, mSession, mSandbox);
            System.out.println("");
            System.out.println("");
            System.out.println("");
            System.out.println("");
            System.out.println("");
            System.out.println("");
            System.out.println("");
            s.prompt(System.out);
            System.out.print("> ");
            try {
                String input = reader.readLine();
                s.updateSession(mSession, input);
                s = getNextScreen();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        //We have a session and are ready to fill out a form!
        
        
        //Get our form object
        String formXmlns = mSession.getForm();
        
        XFormPlayer player = new XFormPlayer(System.in, System.out, null);
        player.setSessionIIF(mSession.getIIF());
        player.start(mEngine.loadFormByXmlns(formXmlns));

    }
    
    private Screen getNextScreen() {
        String next = mSession.getNeededData();
        
        if(next == null) {
            //XFORM TIME!
            return null;
        }
        else if(next.equals(SessionFrame.STATE_COMMAND_ID)) {
            return new MenuScreen();
        } else if(next.equals(SessionFrame.STATE_DATUM_VAL)) {
            return new EntityScreen();
        } else if(next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
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
                return;
            }
        }
    }
    

    private void error(Exception e) {
        e.printStackTrace();
        System.exit(-1);
    }

    private void setupSandbox() {
        //Set up our storage
        PrototypeFactory.setStaticHasher(mPrototypeFactory);
        mSandbox = new MockUserDataSandbox(mPrototypeFactory);
        
        //fetch the restore data and set credentials
        String otaRestoreURL = PropertyManager._().getSingularProperty("ota-restore-url") + "?version=2.0";
        String domain = PropertyManager._().getSingularProperty("cc_user_domain");
        final String qualifiedUsername = mUsername + "@" + domain;
        
        Authenticator.setDefault (new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication (qualifiedUsername, mPassword.toCharArray());
            }
        });
        
        //Go get our sandbox!
        try{
            URL url = new URL(otaRestoreURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            System.out.println("Restoring user " + this.mUsername + " to domain " + domain);
            
            MockDataUtils.parseIntoSandbox(new BufferedInputStream(conn.getInputStream()), mSandbox);
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
                //Initialize our User
        for(IStorageIterator<User> iterator = mSandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
            User u = iterator.nextRecord();
            if(mUsername.equalsIgnoreCase(u.getUsername())) {
                mSandbox.setLoggedInUser(u);
            }
        }
    }

}
