package org.commcare.util.cli;

import org.commcare.util.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public class CliValidateCommand extends CliCommand {
    public CliValidateCommand(String commandName, String[] args) {
        super(commandName, args);
    }

    @Override
    public void handle() {
        super.handle();
        PrototypeFactory prototypeFactory = new LivePrototypeFactory();
        if (args.length < 2) {
            printHelpText();
            System.exit(-1);
        }

        CommCareConfigEngine engine = configureApp(args[1], prototypeFactory);
        engine.describeApplication();

        System.exit(0);
    }

    @Override
    protected String getPositionalArgsHelpText() {
        return "<inputfile> [-nojarresources|path/to/jar/resources]";
    }

    @Override
    protected String getHelpTextDescription() {
        return "Validate a CommCare app";
    }
}
