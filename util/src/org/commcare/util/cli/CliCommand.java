package org.commcare.util.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.commcare.util.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public abstract class CliCommand {
    protected String commandName;
    protected String[] args;
    protected Options options;
    protected CommandLine cmd;

    public CliCommand(String commandName, String[] args) {
        this.commandName = commandName;
        this.args = args;

        options = getOptions();
    }
    public void parseArguments() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        cmd = parser.parse(options, args);
    }

    public void checkHelpOption() {
        if (cmd.hasOption("h")) {
            printHelpText();
            System.exit(0);
        }
    }

    public void handle() {
        checkHelpOption();
    }

    protected Options getOptions() {
        Options options = new Options();
        options.addOption(Option.builder("h")
                .desc("Get a list of options")
                .build());
        return options;
    }

    protected void printHelpText() {
        String usage = "java -jar commcare-cli.jar " + commandName + " " + getPositionalArgsHelpText();
        String header = getHelpTextDescription() + "\n";
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(200);
        formatter.printHelp(usage, header, options, "", true);
    }

    protected abstract String getPositionalArgsHelpText();
    protected abstract String getHelpTextDescription();

    protected static CommCareConfigEngine configureApp(String resourcePath, PrototypeFactory factory) {
        CommCareConfigEngine engine = new CommCareConfigEngine(System.out, factory);

        //TODO: check arg for whether it's a local or global file resource and
        //make sure it's absolute

        if (resourcePath.endsWith(".ccz")) {
            engine.initFromArchive(resourcePath);
        } else {
            engine.initFromLocalFileResource(resourcePath);
        }
        engine.initEnvironment();
        return engine;
    }
}
