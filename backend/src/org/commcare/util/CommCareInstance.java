package org.commcare.util;

import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.UserRestore;

public interface CommCareInstance {
    void registerSuite(Suite s);

    void setProfile(Profile p);

    void registerDemoUserRestore(UserRestore userRestore);

    int getMajorVersion();

    int getMinorVersion();
}
