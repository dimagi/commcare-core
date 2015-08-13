/**
 *
 */
package org.commcare.util;

import org.commcare.util.cli.ApplicationHost;
import org.commcare.util.mocks.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Harness {

    PrototypeFactory prototypeFactory;

    /**
     * @param args
     */
    public static void main(String[] args) {
        if(args.length < 1) {
            printformat();
            System.exit(-1);
        }

        PrototypeFactory prototypeFactory = setupStaticStorage();
        if(args[0].equals("validate")) {
            if(args.length < 2) {
                printvalidateformat();
                System.exit(-1);
            }
            
            CommCareConfigEngine engine = configureApp(args, prototypeFactory);
            engine.describeApplication();
            
            System.exit(0);
        }

        if ("play".equals(args[0])) {
            try {
                if (args.length < 4) {
                    printplayformat();
                    System.exit(-1);
                }


                CommCareConfigEngine engine = configureApp(args, prototypeFactory);
                String username = args[2];
                String password = args[3];

                ApplicationHost host = new ApplicationHost(engine, username, password, prototypeFactory);

                host.run();
                System.exit(-1);
            } finally {
                //Since the CommCare libs start up threads for things like caching, if unhandled
                //exceptions bubble up they will prevent the process from dying unless we kill it
                System.exit(0);
            }
        }
    }

    private static PrototypeFactory setupStaticStorage() {
        LivePrototypeFactory prototypeFactory = new LivePrototypeFactory();
        //Set up our storage
        PrototypeFactory.setStaticHasher(prototypeFactory);
        return prototypeFactory;
    }

    private static void printplayformat() {
        System.out.println("Usage: java -jar thejar.jar play path/to/commcare.ccz username password");
    }

    private static CommCareConfigEngine configureApp(String[] args, PrototypeFactory factory) {
        CommCareConfigEngine engine = new CommCareConfigEngine(System.out, factory);

        //TODO: check arg for whether it's a local or global file resource and
        //make sure it's absolute

        String resourcePath = args[1];
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
