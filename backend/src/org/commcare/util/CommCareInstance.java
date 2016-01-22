package org.commcare.util;

import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;

public interface CommCareInstance {
    void registerSuite(Suite s);

    void setProfile(Profile p);

    int getMajorVersion();

    int getMinorVersion();
}
