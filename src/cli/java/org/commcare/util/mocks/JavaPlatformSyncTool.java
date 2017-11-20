package org.commcare.util.mocks;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.commcare.modern.session.SessionWrapper;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.User;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by ctsims on 8/11/2017.
 */

public class JavaPlatformSyncTool extends SyncStateMachine {

    public boolean isRecoverySupported = false;
    CoreNetworkContext context;

    public JavaPlatformSyncTool(String username, String password, UserSandbox sandbox,
                                SessionWrapper session) {
        super(username, password, sandbox, session);
        context = new CoreNetworkContext(username, password);
    }

    @Override
    protected void performPlatformRequest(String urlToUse) {
        System.out.println(String.format("Request Triggered [Phase: %s]: URL - %s", state.toString(), nextUrlToUse));

        //Go get our sandbox!
        try {
            URL url = new URL(nextUrlToUse);
            HttpURLConnection conn = getHttpConnection(url);
            System.out.println(String.format("Response: %d", conn.getResponseCode()));
            if (conn.getResponseCode() == 412) {
                this.state = State.Recovery_Requested;
            } else if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                unrecoverableError(Error.Invalid_Credentials);
            } else if (conn.getResponseCode() == 200) {
                payload = this.generatePayloadFromConnection(conn);
                this.state = State.Payload_Received;
            } else if (conn.getResponseCode() == 202) {
                this.state = State.Waiting_For_Progress;
            } else {
                unrecoverableError(Error.Unexpected_Server_Response_Code);
                this.unexpectedResponseCode = conn.getResponseCode();
            }
        } catch (IOException e) {
            recoverableError(Error.Network_Error);
            e.printStackTrace();
        }
    }

    private HttpURLConnection getHttpConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        HashMap<String, String> headers = new HashMap<>();
        populateCurrentPlatformConnectionHeaders(headers);
        context.addAuthProperty(conn, headers);
        context.configureProperties(conn, headers);
        return conn;
    }

    private byte[] generatePayloadFromConnection(HttpURLConnection conn) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamsUtil.writeFromInputToOutput(bis, bos);
        return bos.toByteArray();
    }

    public void processPlatformPayload(boolean inRecoveryMode){
        if(inRecoveryMode) {
            unrecoverableError("The Java Platform Sync cannot yet handle 412 recovery events",
                    Error.Unhandled_Situation);
            return;
        }

        try {
            ParseUtils.parseIntoSandbox(new ByteArrayInputStream(payload), sandbox);
            this.state = State.Success;
        } catch (XmlPullParserException | UnfullfilledRequirementsException |
                InvalidStructureException | IOException e) {
            e.printStackTrace();
            unrecoverableError(Error.Invalid_Payload);
        }
    }

    protected void processPlatformWaitSignal() {
        //TODO: This is not correct
        System.out.println("Received a 202, waiting for 5 seconds before retrying");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        state = SyncStateMachine.State.Ready_For_Request;
    }
}
