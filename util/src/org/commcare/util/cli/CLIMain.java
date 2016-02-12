package org.commcare.util.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

/**
 * @author ctsims
 */
public class CliMain {
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine cmd;
        CliCommand cliCommand;

        try {
            cliCommand = getCliCommand(args);
        } catch (CliCommandNotFound e) {
            try {
                cmd = parser.parse(options, args);
            } catch (ParseException parseException) {
                System.out.println("Invalid arguments: " + parseException.getMessage());
                System.exit(-1);
                return;
            }

            if (cmd.hasOption("h")) {
                printHelpText(options);
                System.exit(0);
                return;
            } else {
                System.out.println("Invalid command  " + e.getCommandName());
                printHelpText(options);
                System.exit(-1);
                return;
            }
        }

        try {
            cliCommand.parseArguments();
        } catch (ParseException e) {
            System.out.println("Invalid arguments: " + e.getMessage());
            System.exit(-1);
            return;
        }

        cliCommand.handle();
    }

    private static CliCommand getCliCommand(String[] args) throws CliCommandNotFound {
        final String commandName = args[0];
        final String[] restArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        switch (commandName) {
            case "validate":
                return new CliValidateCommand(commandName, restArgs);
            case "play":
                return new CliPlayCommand(commandName, restArgs);
            default:
                throw new CliCommandNotFound(commandName);
        }
    }

    private static void printHelpText(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar commcare-cli.jar", options);
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder("h")
                .desc("Get a list of options")
                .build());

        return options;
    }

    private static class CliCommandNotFound extends Exception {
        private final String commandName;
        public CliCommandNotFound(String commandName) {
            this.commandName = commandName;
        }
        public String getCommandName() {
            return commandName;
        }
    }
}
