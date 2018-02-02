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
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertTrue;

/**
 *
 * Tests for the CommCare CLI
 *
 * Uses a specific, highly paired format to deal with the CLI's I/O
 *
 * @author wpride
 */

public class CliTests {

    private class CliTestRun<E extends CliTestReader> {

        CliTestRun(String applicationPath,
                          String restoreResource,
                          Class<E> cliTestReaderClass,
                          String steps) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
            ApplicationHost host = buildApplicationHost(applicationPath, restoreResource, cliTestReaderClass, steps);
            boolean passed = false;
            try {
                host.run();
            } catch (TestPassException e) {
                passed = true;
            }
            assertTrue(passed);
        }

        private ApplicationHost buildApplicationHost(String applicationResource,
                                                     String restoreResource,
                                                     Class<E> cliTestReaderClass,
                                                     String steps) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            ClassLoader classLoader = getClass().getClassLoader();
            String applicationPath = new File(classLoader.getResource(applicationResource).getFile()).getAbsolutePath();
            PrototypeFactory prototypeFactory = new LivePrototypeFactory();

            CommCareConfigEngine engine = CliCommand.configureApp(applicationPath, prototypeFactory);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream outStream = new PrintStream(baos);

            Constructor<E> ctor = cliTestReaderClass.getConstructor(String.class, ByteArrayOutputStream.class);
            CliTestReader reader = ctor.newInstance(steps, baos);

            ApplicationHost host = new ApplicationHost(engine, prototypeFactory, reader, outStream);
            File restoreFile = new File(classLoader.getResource(restoreResource).getFile());
            String restorePath = restoreFile.getAbsolutePath();
            host.setRestoreToLocalFile(restorePath);
            return host;
        }
    }

    @Test
    public void testConstraintsForm() throws Exception {
        // Start a basic form
        new CliTestRun<>("basic_app/basic.ccz",
                "case_create_basic.xml",
                BasicTestReader.class,
                "1 0 \n");
    }

    @Test
    public void testCaseSelection() throws Exception {
        // Perform case selection
        new CliTestRun<>("basic_app/basic.ccz",
                "basic_app/restore.xml",
                CaseTestReader.class,
                "2 1 5 \n");
    }


    /**
     * The CliTestReader overrides the Reader (usually System.in) passed into the CLI
     * and so is able to provide input through the readLine() function that the CLI
     * reads from. We are also able to get the output at this point and make assertions
     * about its content.
     */
    static abstract class CliTestReader extends BufferedReader {

        private String[] steps;
        private int stepIndex;
        private ByteArrayOutputStream outStream;

        CliTestReader(String steps, ByteArrayOutputStream outStream) {
            super(new StringReader("Unused dummy reader"));
            this.steps = steps.split(" ");
            this.outStream = outStream;
        }

        @Override
        public String readLine() throws IOException {
            String output = new String(outStream.toByteArray(), StandardCharsets.UTF_8);
            processLine(stepIndex, output);
            String ret = steps[stepIndex++];
            outStream.reset();
            // Return the next input for the CLI to process
            return ret;
        }

        abstract void processLine(int stepIndex, String output);
    }

    static class BasicTestReader extends CliTestReader {

        public BasicTestReader(String args, ByteArrayOutputStream outStream) {
            super(args, outStream);
        }

        void processLine(int stepIndex, String output) {
            switch(stepIndex) {
                case 0:
                    Assert.assertTrue(output.contains("Basic Tests"));
                    Assert.assertTrue(output.contains("0)Basic Form Tests"));
                    break;
                case 1:
                    Assert.assertTrue(output.contains("0)Constraints"));
                    break;
                case 2:
                    Assert.assertTrue(output.contains("Press Return to proceed"));
                    break;
                case 3:
                    Assert.assertTrue(output.contains("This form tests different logic constraints."));
                    throw new TestPassException();
                default:
                    throw new RuntimeException(String.format("Did not recognize output %s at stepIndex %s", output, stepIndex));
            }
        }
    }

    static class CaseTestReader extends CliTestReader {

        public CaseTestReader(String args, ByteArrayOutputStream outStream) {
            super(args, outStream);
        }

        void processLine(int stepIndex, String output) {
            switch(stepIndex) {
                case 0:
                    Assert.assertTrue(output.contains("Basic Tests"));
                    Assert.assertTrue(output.contains("0)Basic Form Tests"));
                    break;
                case 1:
                    Assert.assertTrue(output.contains("0)Create a Case"));
                    break;
                case 2:
                    Assert.assertTrue(output.contains("Case | vl1"));
                    Assert.assertTrue(output.contains("Date Opened"));
                    Assert.assertTrue(output.contains("case one"));
                    break;
                case 3:
                    Assert.assertTrue(output.contains("Form Start: Press Return to proceed"));
                    break;
                case 4:
                    Assert.assertTrue(output.contains("This form will allow you to add and update"));
                    throw new TestPassException();
                default:
                    throw new RuntimeException(String.format("Did not recognize output %s at stepIndex %s", output, stepIndex));

            }
        }
    }

    // Because the CLI is a REPL that will loop indefinitely unless certain code paths are
    // reached we need to provide a way for tests to exit early. This exception will be
    // caught at the top level of the CliTestRun and set the tests to pass when thrown.
    private static class TestPassException extends RuntimeException {}

}
