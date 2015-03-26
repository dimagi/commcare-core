/**
 *
 */
package org.commcare.util;

import java.io.File;

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
            CommCareConfigEngine engine = new CommCareConfigEngine(System.out);

            //TODO: check arg for whether it's a local or global file resource and
            //make sure it's absolute

            engine.addLocalFileResource(args[1]);
            if(args.length > 2) {
                if(args[2].equals("-nojarresources")) {
                    //Skip it
                } else {
                    engine.addJarResources(args[2]);
                }
            } else {
                engine.addJarResources(".." + File.separator + "application"  + File.separator + "resources");
                engine.addJarResources(".." + File.separator + ".."  + File.separator + "javarosa" + File.separator + "j2me" + File.separator + "shared-resources" + File.separator + "resources");
            }
            engine.resolveTable();
            engine.validateResources();
            engine.describeApplication();
            System.exit(0);
        }
    }

    private static void printformat() {
        System.out.println("Usage: java -jar thejar.jar [validate|otherstuff]");
    }

    private static void printvalidateformat() {
        System.out.println("Usage: java -jar thejar.jar validate inputfile.xml [-nojarresources|path/to/jar/resources]");
    }

}
