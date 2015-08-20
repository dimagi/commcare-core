package org.commcare.util.cli;

import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.SessionWrapper;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A subscreen's lifecycle is controlled by its host, and it is spawned and managed by it.
 *
 * Subscreens should receive their initializaiton during instantiation and know how to update
 * their parent screen to direct it during navigation.
 *
 * Created by ctsims on 8/20/2015.
 */
public abstract class Subscreen<T extends CompoundScreenHost>  {

    /**
     * The subscreen should process the provided input, and update any relevant state in its
     * host screen.
     *
     * There is no direct mechanism for directing navigation to another screen, apart from specific
     * calls to the host to indicate that it should stage another screen to be displayed
     *
     * The return value of this method indicates whether the current Compound Screen is finished and
     * should update the session and return control to the application.
     *
     * @param input User input
     * @param host The Compound Screen that should be updated based on the user's input
     * @return True if the compound screen is ready and the session should move to the next step.
     * False if current Compound Screen should continue executing.
     */
    public abstract boolean handleInputAndUpdateHost(String input, T host);

    /**
     * Display this subscreen
     */
    public abstract void prompt(PrintStream out);

}
