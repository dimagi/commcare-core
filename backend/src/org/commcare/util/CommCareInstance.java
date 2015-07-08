package org.commcare.util;

import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;

public interface CommCareInstance {
    public void registerSuite(Suite s);

    public void setProfile(Profile p);

    public int getMajorVersion();

    public int getMinorVersion();
}
