package org.commcare.util.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

public abstract class CliCommand {
    public final String commandName;
    public final String helpTextDescription;
    public final String positionalArgsHelpText;

    protected String[] args;
    protected final Options options;
    protected CommandLine cmd;

    public CliCommand(String commandName, String helpTextDescription, String positionalArgsHelpText) {
        options = getOptions();
        this.commandName = commandName;
        this.helpTextDescription = helpTextDescription;
        this.positionalArgsHelpText = positionalArgsHelpText;
    }

    /**
     * @param args    all the args after (not including) commandName
     */
    public void parseArguments(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        cmd = parser.parse(options, args);
        this.args = cmd.getArgs();
    }

    private void checkHelpOption() {
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
        String usage = "java -jar commcare-cli.jar " + commandName + " " + positionalArgsHelpText;
        String header = helpTextDescription + "\n";
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(200);
        formatter.printHelp(usage, header, options, "", true);
    }

    protected static CommCareConfigEngine configureApp(String resourcePath, PrototypeFactory factory) {
        CommCareConfigEngine engine = new CommCareConfigEngine(factory);

        //TODO: check arg for whether it's a local or global file resource and
        //make sure it's absolute
        try {
            if (resourcePath.endsWith(".ccz")) {
                engine.initFromArchive(resourcePath);
            } else {
                engine.initFromLocalFileResource(resourcePath);
            }
        } catch (InstallCancelledException e) {
            System.out.println("Install was cancelled by the user or system");
            e.printStackTrace(System.out);
            System.exit(-1);
        } catch (UnresolvedResourceException e) {
            System.out.println("While attempting to resolve the necessary resources, one couldn't be found: " + e.getResource().getResourceId());
            e.printStackTrace(System.out);
            System.exit(-1);
        } catch (UnfullfilledRequirementsException e) {
            System.out.println("While attempting to resolve the necessary resources, a requirement wasn't met");
            e.printStackTrace(System.out);
            System.exit(-1);
        }
        engine.initEnvironment();
        return engine;
    }
}
