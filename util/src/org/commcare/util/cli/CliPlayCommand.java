package org.commcare.util.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.commcare.util.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public class CliPlayCommand extends CliCommand {
    public CliPlayCommand(String commandName, String[] args) {
        super(commandName, args);
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();
        options.addOption(Option.builder("r")
                .argName("FILE")
                .hasArg()
                .desc("Restore user data from FILE instead of querying the server")
                .longOpt("restore-file")
                .required(false)
                .optionalArg(false)
                .build());
        return options;
    }

    @Override
    public void handle() {
        super.handle();
        PrototypeFactory prototypeFactory = new LivePrototypeFactory();
        if (args.length < 2) {
            printHelpText();
            System.exit(-1);
        }
        try {
            CommCareConfigEngine engine = configureApp(args[1], prototypeFactory);
            ApplicationHost host = new ApplicationHost(engine, prototypeFactory);

            if (cmd.hasOption("r")) {
                host.setRestoreToLocalFile(cmd.getOptionValue("r"));
            } else {
                if (args.length < 4) {
                    printHelpText();
                    System.exit(-1);
                    return;
                }
                String username = args[2];
                String password = args[3];
                username = username.trim().toLowerCase();
                host.setRestoreToRemoteUser(username, password);
            }

            host.run();
            System.exit(-1);
        } catch (RuntimeException re) {
            System.out.print("Unhandled Fatal Error executing CommCare app");
            re.printStackTrace();
            throw re;
        } finally {
            //Since the CommCare libs start up threads for things like caching, if unhandled
            //exceptions bubble up they will prevent the process from dying unless we kill it
            System.exit(0);
        }
    }

    @Override
    protected String getPositionalArgsHelpText() {
        return "<commcare.ccz url/path> <username> <password>";
    }

    @Override
    protected String getHelpTextDescription() {
        return "Play a CommCare app from the command line";
    }
}
