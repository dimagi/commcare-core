package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Represents an app that exists on CommCare HQ (either india or prod) and is accessible for
 * the currently-authenticated user (either mobile or web) to install.
 *
 * Created by amstone326 on 2/3/17.
 */
public class AppAvailableForInstall implements Serializable, Externalizable {

    private String domain;
    private String appName;
    private String appVersion;
    private boolean isOnProd;
    private String profileRef;
    private String mediaProfileRef;

    public AppAvailableForInstall() {
        // for deserialization
    }

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

    public String getDomainName() {
        return domain;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        domain = ExtUtil.readString(in);
        appName = ExtUtil.readString(in);
        appVersion = ExtUtil.readString(in);
        isOnProd = ExtUtil.readBool(in);
        profileRef = ExtUtil.readString(in);
        mediaProfileRef = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, domain);
        ExtUtil.writeString(out, appName);
        ExtUtil.writeString(out, appVersion);
        ExtUtil.writeBool(out, isOnProd);
        ExtUtil.writeString(out, profileRef);
        ExtUtil.writeString(out, mediaProfileRef);
    }
}
