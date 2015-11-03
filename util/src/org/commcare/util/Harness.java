/**
 *
 */
package org.commcare.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.commcare.util.cli.ApplicationHost;
import org.commcare.util.cli.EnketoTransformer;
import org.commcare.util.mocks.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

import java.io.File;

/**
 * @author ctsims
 *
 */
public class Harness {


    static CommandLineParser parser;

    /**
     * @param args
     */
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e) {
            System.out.println("Invalid arguments: " + e.getMessage());
            System.exit(-1);
            return;
        }

        if(cmd.hasOption("h")) {
            printHelpText(options);
            System.exit(0);
            return;
        }

        args = cmd.getArgs();

        PrototypeFactory prototypeFactory = setupStaticStorage();
        if(args[0].equals("validate")) {
            if(args.length < 2) {
                printvalidateformat();
                System.exit(-1);
            }
            
            CommCareConfigEngine engine = configureApp(args[1], prototypeFactory);
            engine.describeApplication();

            System.exit(0);
        }

        if ("play".equals(args[0])) {
            try {
                CommCareConfigEngine engine = configureApp(args[1], prototypeFactory);
                ApplicationHost host = new ApplicationHost(engine, prototypeFactory);

                if(cmd.hasOption("r")) {
                    host.setRestoreToLocalFile(cmd.getOptionValue("r"));
                } else {
                    if(args.length < 4) {
                        printPlayFormat();
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
            }finally {
                //Since the CommCare libs start up threads for things like caching, if unhandled
                //exceptions bubble up they will prevent the process from dying unless we kill it
                System.exit(0);
            }
        }

        if ("test".equals(args[0])) {
            try {
                CommCareConfigEngine engine = configureApp(args[1], prototypeFactory);
                ApplicationHost host = new ApplicationHost(engine, prototypeFactory);

                if(cmd.hasOption("r")) {
                    host.setRestoreToLocalFile(cmd.getOptionValue("r"));
                } else {
                    if(args.length < 5) {
                        printTestFormat();
                        System.exit(-1);
                        return;
                    }
                    String username = args[2];
                    String password = args[3];
                    username = username.trim().toLowerCase();
                    host.setRestoreToRemoteUser(username, password);
                }
                String testPlan = args[4];
                File testFile = new File(testPlan);
                TestHarness testHarness = new TestHarness(host, testFile);
                testHarness.runTests();
                System.exit(-1);
            } catch (RuntimeException re) {
                System.out.print("Unhandled Fatal Error executing CommCare app");
                re.printStackTrace();
                throw re;
            }finally {
                //Since the CommCare libs start up threads for things like caching, if unhandled
                //exceptions bubble up they will prevent the process from dying unless we kill it
                System.exit(0);
            }
        }

        if ("transform".equals(args[0])) {
            try {
                CommCareConfigEngine engine = configureApp(args[1], prototypeFactory);
                ApplicationHost host = new ApplicationHost(engine, prototypeFactory);

                if(cmd.hasOption("r")) {
                    host.setRestoreToLocalFile(cmd.getOptionValue("r"));
                } else {
                    if(args.length < 5) {
                        printTestFormat();
                        System.exit(-1);
                        return;
                    }
                    String username = args[2];
                    String password = args[3];
                    username = username.trim().toLowerCase();
                    host.setRestoreToRemoteUser(username, password);
                }
                String xformPath = args[4];
                File xformFile = new File(xformPath);

                host.init();

                EnketoTransformer transformer = new EnketoTransformer(xformFile, host);
                //transformer.buildXmlDoc();

                System.exit(-1);
            } catch (RuntimeException re) {
                System.out.print("Unhandled Fatal Error executing CommCare app");
                re.printStackTrace();
                throw re;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //Since the CommCare libs start up threads for things like caching, if unhandled
                //exceptions bubble up they will prevent the process from dying unless we kill it
                System.exit(0);
            }
        }
    }

    private static void printHelpText(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "java -jar commcare-cli.jar", options );
    }

    private static Options getOptions() {
        Options options = new Options();

        OptionGroup play = new OptionGroup();

        options.addOption(Option.builder("r")
                .argName("FILE")
                .hasArg()
                .desc("Restore user data from FILE instead of querying the server")
                .longOpt("restore-file")
                .required(false)
                .optionalArg(false)
                .build());

        options.addOption(Option.builder("h")
                .desc("Get a list of options")
                .build());

        return options;
    }

    private static PrototypeFactory setupStaticStorage() {
        LivePrototypeFactory prototypeFactory = new LivePrototypeFactory();
        //Set up our storage
        return prototypeFactory;
    }

    private static void printPlayFormat() {
        System.out.println("Usage: java -jar thejar.jar play path/to/commcare.ccz username password");
    }

    private static void printTestFormat() {
        System.out.println("Usage: java -jar thejar.jar test path/to/commcare.ccz username password path/to/tests.txt");
    }

    private static CommCareConfigEngine configureApp(String resourcePath, PrototypeFactory factory) {
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

    private static void printformat() {
        System.out.println("Usage: java -jar thejar.jar [validate|otherstuff]");
    }

    private static void printvalidateformat() {
        System.out.println("Usage: java -jar thejar.jar validate inputfile.xml [-nojarresources|path/to/jar/resources]");
    }

}
