package org.commcare.util.screen;

import org.commcare.cases.util.CaseDBUtils;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

public class SessionUtils {

    public static void restoreUserToSandbox(UserSandbox sandbox,
                                            SessionWrapper session,
                                            CommCarePlatform platform,
                                            String username,
                                            final String password,
                                            PrintStream printStream) {
        String urlStateParams = "";

        boolean failed = true;

        boolean incremental = false;

        if (sandbox.getLoggedInUser() != null) {
            String syncToken = sandbox.getSyncToken();
            String caseStateHash = CaseDBUtils.computeCaseDbHash(sandbox.getCaseStorage());

            urlStateParams = String.format("&since=%s&state=ccsh:%s", syncToken, caseStateHash);
            incremental = true;

            printStream.println(String.format(
                    "\nIncremental sync requested. \nSync Token: %s\nState Hash: %s",
                    syncToken, caseStateHash));
        }

        PropertyManager propertyManager = platform.getPropertyManager();

        //fetch the restore data and set credentials
        String otaFreshRestoreUrl = propertyManager.getSingularProperty("ota-restore-url") +
                "?version=2.0";

        String otaSyncUrl = otaFreshRestoreUrl + urlStateParams;

        String domain = propertyManager.getSingularProperty("cc_user_domain");
        final String qualifiedUsername = username + "@" + domain;

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(qualifiedUsername, password.toCharArray());
            }
        });

        //Go get our sandbox!
        try {
            printStream.println("GET: " + otaSyncUrl);
            URL url = new URL(otaSyncUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            if (conn.getResponseCode() == 412) {
                printStream.println("Server Response 412 - The user sandbox is not consistent with " +
                        "the server's data. \n\nThis is expected if you have changed cases locally, " +
                        "since data is not sent to the server for updates. \n\nServer response cannot be restored," +
                        " you will need to restart the user's session to get new data.");
            } else if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                printStream.println("\nInvalid username or password!");
            } else if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {

                printStream.println("Restoring user " + username + " to domain " + domain);

                ParseUtils.parseIntoSandbox(new BufferedInputStream(conn.getInputStream()), sandbox);

                printStream.println("User data processed, new state token: " + sandbox.getSyncToken());
                failed = false;
            } else {
                printStream.println("Unclear/Unexpected server response code: " + conn.getResponseCode());
            }
        } catch (InvalidStructureException | IOException
                | XmlPullParserException | UnfullfilledRequirementsException e) {
            e.printStackTrace();
        }

        if (failed) {
            if (!incremental) {
                System.exit(-1);
            }
        } else {
            //Initialize our User
            for (IStorageIterator<User> iterator = sandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
                User u = iterator.nextRecord();
                if (username.equalsIgnoreCase(u.getUsername())) {
                    sandbox.setLoggedInUser(u);
                }
            }
        }

        if (session != null) {
            // old session data is now no longer valid
            session.clearVolatiles();
        }
    }
}
