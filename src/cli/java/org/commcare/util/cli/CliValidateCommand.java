package org.commcare.util.cli;

import org.apache.commons.cli.ParseException;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public class CliValidateCommand extends CliCommand {

    private String resourcePath;

    public CliValidateCommand() {
        super("validate", "Validate a CommCare app", "<inputfile> [-nojarresources|path/to/jar/resources]");
    }

    @Override
    public void parseArguments(String[] args) throws ParseException {
        super.parseArguments(args);
        resourcePath = this.args[0];
    }

    @Override
    public void handle() {
        super.handle();
        PrototypeFactory prototypeFactory = new LivePrototypeFactory();
        CommCareConfigEngine engine = configureApp(resourcePath, prototypeFactory);
        engine.describeApplication();

        System.exit(0);
    }
}
