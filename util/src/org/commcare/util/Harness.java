/**
 *
 */
package org.commcare.util;

import org.commcare.util.cli.ApplicationHost;

/**
 * @author ctsims
 *
 */
public class Harness {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if(args.length < 1) {
            printformat();
            System.exit(-1);
        }
        if(args[0].equals("validate")) {
            if(args.length < 2) {
                printvalidateformat();
                System.exit(-1);
            }
            
            CommCareConfigEngine engine = configureApp(args);
            engine.describeApplication();
            
            System.exit(0);
        }
        
        if(args[0].equals("play")) {
            if(args.length < 4) {
                printplayformat();
                System.exit(-1);
            }
            CommCareConfigEngine engine = configureApp(args);
            String username = args[2];
            String password = args[3];
            
            ApplicationHost host = new ApplicationHost(engine, username, password);
            
            host.run();
            
            System.exit(0);
        }
    }
    
    private static void printplayformat() {
        System.out.println("Usage: java -jar thejar.jar play path/to/commcare.ccz username password");
    }

    private static CommCareConfigEngine configureApp(String[] args) {
        CommCareConfigEngine engine = new CommCareConfigEngine(System.out);

        //TODO: check arg for whether it's a local or global file resource and
        //make sure it's absolute

        String resourcePath = args[1];
        if(resourcePath.endsWith(".ccz")) {
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
