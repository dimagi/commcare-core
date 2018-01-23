package org.cli;

import org.commcare.util.cli.ApplicationHost;
import org.commcare.util.cli.CliCommand;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by wpride on 12/14/2016.
 */

public class CliTests {

    @Test
    public void testConstraintsForm() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File appFile = new File(classLoader.getResource("basic_app/basic.ccz").getFile());
        String resourcePath = appFile.getAbsolutePath();
        PrototypeFactory prototypeFactory = new LivePrototypeFactory();
        CommCareConfigEngine engine = CliCommand.configureApp(resourcePath, prototypeFactory);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream outStream = new PrintStream(baos);
        BufferedReader bufferedReader = new TestReader(new String[] {"1", "0", "\n"}, baos);

        ApplicationHost host = new ApplicationHost(engine, prototypeFactory, bufferedReader, outStream);
        File restoreFile = new File(classLoader.getResource("case_create_basic.xml").getFile());
        String restorePath = restoreFile.getAbsolutePath();
        host.setRestoreToLocalFile(restorePath);
        boolean passed = false;
        try {
            host.run();
        } catch (EarlyExitException e) {
            passed = true;
        }
        assertTrue(passed);
    }

    @Test
    public void testCaseSelection() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File appFile = new File(classLoader.getResource("basic_app/basic.ccz").getFile());
        String resourcePath = appFile.getAbsolutePath();
        PrototypeFactory prototypeFactory = new LivePrototypeFactory();
        CommCareConfigEngine engine = CliCommand.configureApp(resourcePath, prototypeFactory);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream outStream = new PrintStream(baos);
        BufferedReader bufferedReader = new TestReader(new String[] {"2", "1", "5", "\n"}, baos);

        ApplicationHost host = new ApplicationHost(engine, prototypeFactory, bufferedReader, outStream);
        File restoreFile = new File(classLoader.getResource("basic_app/restore.xml").getFile());
        String restorePath = restoreFile.getAbsolutePath();
        host.setRestoreToLocalFile(restorePath);
        boolean passed = false;
        try {
            host.run();
        } catch (EarlyExitException e) {
            passed = true;
        }
        assertTrue(passed);
    }

    class TestReader extends BufferedReader {

        private String[] args;
        private int index;
        private ByteArrayOutputStream outStream;

        TestReader(String[] args, ByteArrayOutputStream outStream) {
            super(new StringReader("Unused dummy reader"));
            this.args = args;
            this.outStream = outStream;
        }

        @Override
        public String readLine() throws IOException {
            String output = new String(outStream.toByteArray(), StandardCharsets.UTF_8);
            if (index == 0) {
                Assert.assertTrue(output.contains("Basic Tests"));
                Assert.assertTrue(output.contains("0)Basic Form Tests"));
            } else if (index == 1) {
                Assert.assertTrue(output.contains("0)Create a Case"));
            } else if (index == 2) {
                Assert.assertTrue(output.contains("Case | vl1"));
                Assert.assertTrue(output.contains("Date Opened"));
                Assert.assertTrue(output.contains("case one"));
            } else if (index == 3) {
                Assert.assertTrue(output.contains("Form Start: Press Return to proceed"));
            } else {
                Assert.assertTrue(output.contains("This form will allow you to add and update"));
                throw new EarlyExitException();
            }
            String ret = args[index];
            index++;
            outStream.reset();
            return ret;
        }
    }

    private class EarlyExitException extends RuntimeException {}

}
