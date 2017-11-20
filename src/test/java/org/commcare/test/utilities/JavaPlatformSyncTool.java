package org.commcare.test.utilities;

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

/**
 * Created by ctsims on 8/11/2017.
 */

public class JavaPlatformSyncTool extends SyncStateMachine {

    public JavaPlatformSyncTool(String username, String password, UserSandbox sandbox,
                                SessionWrapper session) {
        super(username, password, sandbox, session);
    }

    @Override
    public void setUpAuthentication(final String qualifiedUsername, final char[] password) {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(qualifiedUsername, password);
            }
        });

    }

    @Override
    protected void performPlatformRequest(String urlToUse) {
        System.out.println(String.format("Request Triggered [Phase: %s]: URL - %s", state.toString(), nextUrlToUse));

        //Go get our sandbox!
        try {

            URL url = new URL(nextUrlToUse);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

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
                unrecoverableError(Error.Unexpected_Response);
                this.unexpectedResponseCode = conn.getResponseCode();
            }
        } catch (IOException e) {
            recoverableError(Error.Network_Error);
            e.printStackTrace();
        }
    }

    private byte[] generatePayloadFromConnection(HttpURLConnection conn) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamsUtil.writeFromInputToOutput(bis, bos);
        return bos.toByteArray();
    }

    public void processPlatformPayload(boolean inRecoveryMode){
        if(inRecoveryMode) {
            throw new RuntimeException("Cannot currently perform recovery on the mock platform sync tool");
        }

        try {
            ParseUtils.parseIntoSandbox(new ByteArrayInputStream(payload), sandbox);
        } catch (XmlPullParserException | UnfullfilledRequirementsException |
                InvalidStructureException | IOException e) {
            e.printStackTrace();
            unrecoverableError(Error.Invalid_Payload);
        }
    }
}
