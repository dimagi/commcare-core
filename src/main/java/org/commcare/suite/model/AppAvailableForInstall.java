package org.commcare.suite.model;

import java.io.Serializable;

/**
 * Created by amstone326 on 2/3/17.
 */

public class AppAvailableForInstall implements Serializable {

    private String domain;
    private String appName;
    private String appVersion;
    private boolean isOnProd;
    private String profileRef;
    private String mediaProfileRef;

    public AppAvailableForInstall(String domain, String appName, String appVersion,
                                  boolean isOnProd, String profileRef, String mediaProfileRef) {
        this.domain = domain;
        this.appName = appName;
        this.appVersion = appVersion;
        this.isOnProd = isOnProd;
        this.profileRef = profileRef;
        this.mediaProfileRef = mediaProfileRef;
    }

    public String getAppName() {
        return appName;
    }

    public String getMediaProfileRef() {
        return mediaProfileRef;
    }
}
