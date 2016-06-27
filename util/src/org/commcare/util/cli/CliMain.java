package org.commcare.util.cli;

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
            cliCommand = getCliCommand(args[0]);
        } catch (CliCommandNotFound e) {
            try {
                cmd = parser.parse(options, args);
            } catch (ParseException parseException) {
                System.out.println("Invalid arguments: " + parseException.getMessage());
                printHelpText();
                System.exit(-1);
                return;
            }

            if (cmd.hasOption("h")) {
                printHelpText();
                System.exit(0);
                return;
            } else {
                System.out.println("Invalid command  " + e.getCommandName());
                printHelpText();
                System.exit(-1);
                return;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            printHelpText();
            System.exit(-1);
            return;

        }

        try {
            final String[] restArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            cliCommand.parseArguments(restArgs);
        } catch (ParseException e) {
            System.out.println("Invalid arguments: " + e.getMessage());
            cliCommand.printHelpText();
            System.exit(-1);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            cliCommand.printHelpText();
            System.exit(-1);
            return;
        }

        cliCommand.handle();
    }

    private static CliCommand getCliCommand(String commandName) throws CliCommandNotFound {
        switch (commandName) {
            case "validate":
                return new CliValidateCommand();
            case "play":
                return new CliPlayCommand();
            default:
                throw new CliCommandNotFound(commandName);
        }
    }

    private static void printHelpText() {
        Options options = getOptions();
        HelpFormatter formatter = new HelpFormatter();
        String header = "The Command Line Interface for CommCare\n\n" +
                "The available commands are\n";
        CliCommand[] commands = {new CliValidateCommand(), new CliPlayCommand()};
        for (CliCommand command : commands) {
            header += String.format("   %-11s%s\n", command.commandName, command.helpTextDescription);
        }
        header += "\n";
        formatter.printHelp("java -jar commcare-cli.jar <command> [<args>]", header, options, "");
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
