package org.commcare.util.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.commcare.util.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public class CliPlayCommand extends CliCommand {

    private String resourcePath;
    private String username;
    private String password;

    public CliPlayCommand() {
        super("play", "Play a CommCare app from the command line", "<commcare.ccz url/path> [<username> <password>]");
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();
        Option restoreFile = Option.builder("r")
                .argName("FILE")
                .hasArg()
                .desc("Restore user data from FILE instead of querying the server")
                .longOpt("restore-file")
                .required(false)
                .optionalArg(false)
                .build();
        Option demoUser = Option.builder("d")
                .argName("DEMO_USER")
                .desc("Use the demo user restore in the app")
                .longOpt("use-demo-user")
                .required(false)
                .optionalArg(false)
                .build();
        options.addOption(restoreFile).addOption(demoUser);

        return options;
    }

    @Override
    public void parseArguments(String[] args) throws ParseException {
        super.parseArguments(args);

        resourcePath = this.args[0];
        if (!cmd.hasOption("r") && !cmd.hasOption("d")) {
            username = this.args[1];
            password = this.args[2];
        }
    }

    @Override
    public void handle() {
        super.handle();
        PrototypeFactory prototypeFactory = new LivePrototypeFactory();
        try {
            CommCareConfigEngine engine = configureApp(resourcePath, prototypeFactory);
            ApplicationHost host = new ApplicationHost(engine, prototypeFactory);

            if (cmd.hasOption("r")) {
                host.setRestoreToLocalFile(cmd.getOptionValue("r"));
            } else if (cmd.hasOption("d")) {
                host.setRestoreToDemoUser();
            } else {
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
}
