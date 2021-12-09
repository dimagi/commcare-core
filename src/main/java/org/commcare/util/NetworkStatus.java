package org.commcare.util;

import org.commcare.core.network.CommCareNetworkService;
import org.commcare.core.network.CommCareNetworkServiceGenerator;
import org.javarosa.core.services.Logger;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * @author $|-|!˅@M
 */
public class NetworkStatus {
    public static boolean isCaptivePortal() {
        String captivePortalURL = "http://www.commcarehq.org/serverup.txt";
        CommCareNetworkService commCareNetworkService =
                CommCareNetworkServiceGenerator.createNoAuthCommCareNetworkService();
        try {
            Response<ResponseBody> response =
                    commCareNetworkService.makeGetRequest(captivePortalURL, new HashMap<>(), new HashMap<>()).execute();
            return response.code() == 200 && !"success".equals(response.body().string());
        } catch (IOException e) {
            Logger.log(LogTypes.TYPE_WARNING_NETWORK, "Detecting captive portal failed with exception" + e.getMessage());
            return false;
        }
    }
}
