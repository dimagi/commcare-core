package org.cli;

import org.commcare.util.cli.ApplicationHost;
import org.commcare.util.cli.CliCommand;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;

/**
 * Created by wpride on 12/14/2016.
 */

public class CliTests {

    @Test
    public void testApplicationHost() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File appFile = new File(classLoader.getResource("basic_app/basic.ccz").getFile());
        String resourcePath = appFile.getAbsolutePath();
        PrototypeFactory prototypeFactory = new LivePrototypeFactory();
        CommCareConfigEngine engine = CliCommand.configureApp(resourcePath, prototypeFactory);

        PrintStream outStream = new PrintStream(new ByteArrayOutputStream());
        BufferedReader bufferedReader = new TestReader(new String[] {"1"}, outStream);


        ApplicationHost host = new ApplicationHost(engine, prototypeFactory, bufferedReader, outStream);
        File restoreFile = new File(classLoader.getResource("case_create_basic.xml").getFile());
        String restorePath = restoreFile.getAbsolutePath();
        host.setRestoreToLocalFile(restorePath);
        host.run();

    }

    class TestReader extends BufferedReader {

        private String[] args;
        private int index;
        private PrintStream outStream;

        public TestReader(String[] args, PrintStream outStream) {
            super(new StringReader("derp"));
            this.args = args;
            this.outStream = outStream;
        }

        @Override
        public String readLine() throws IOException {
            if (index >= args.length) {
                System.out.println("Overflow!");
            }
            String ret = args[index];
            System.out.println("Reading line! " + ret);
            index++;
            return ret;
        }
    }

}
