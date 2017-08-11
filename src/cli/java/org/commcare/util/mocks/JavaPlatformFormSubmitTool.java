package org.commcare.util.mocks;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.util.DataUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by ctsims on 8/11/2017.
 */

public class JavaPlatformFormSubmitTool {

    UserSandbox sandbox;
    CommCarePlatform platform;

    CoreNetworkContext context;

    String baseUrl;
    String qualifiedUsername;
    String password;

    public JavaPlatformFormSubmitTool(UserSandbox sandbox, CoreNetworkContext context) {
        this.sandbox = sandbox;
        baseUrl = PropertyManager.instance().getSingularProperty("PostURL");
        this.context = context;
    }

    public String getSubmitUrl() {
        return this.baseUrl;
    }

    public boolean submitFormToServer(byte[] serializedForm) {
        URL url;
        try {
            url = new URL(baseUrl);
        } catch (MalformedURLException e) {
            System.out.println("Invalid submission url: " +baseUrl);
            return false;
        }
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);

            HashMap<String, String> submissionProperties = new HashMap<>();
            addCommonRequestProperties(submissionProperties);
            context.addAuthProperty(conn, submissionProperties);
            context.configureProperties(conn, submissionProperties);

            OutputStream postStream = conn.getOutputStream();
            StreamsUtil.writeFromInputToOutput(new ByteArrayInputStream(serializedForm), postStream);

            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                if(responseCode != 200 ) {
                    System.out.println("Form submission succeeded w/Response code: " + responseCode);
                }
                return true;
            } else if (responseCode == 401) {
                System.out.println("Incorrect authentication during form submission");
                return false;
            } else {
                System.out.println("Unexpected response code during form submission: " + responseCode);
                return false;
            }


        } catch(IOException ioe) {
            System.out.println("Network issue during form submission: " + ioe.getMessage());
            return false;
        }
    }

    public void addCommonRequestProperties(HashMap<String, String> properties) {
        //TODO: This should get centralized around common requests.

        properties.put("X-OpenRosa-Version", "2.1");
        if (sandbox.getSyncToken() != null) {
            properties.put("X-CommCareHQ-LastSyncToken", sandbox.getSyncToken());
        }
        properties.put("x-openrosa-deviceid", "commcare-mock-utility");

    }



}
