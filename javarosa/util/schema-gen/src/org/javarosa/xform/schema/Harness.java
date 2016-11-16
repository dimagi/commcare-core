package org.javarosa.xform.schema;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.javarosa.core.model.FormDef;
import org.javarosa.engine.XFormPlayer;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.FormInstanceLoader;
import org.javarosa.xform.util.XFormUtils;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

public class Harness {
    // Track specification extension keywords so we know what to do during
    // parsing when they are encountered.
    private static final Hashtable<String, Vector<String>> specExtensionKeywords =
            new Hashtable<>();
    // Namespace for which inner elements should be parsed.
    private static final Vector<String> parseSpecExtensionsInnerElements =
            new Vector<>();
    // Namespace for which we supress "unrecognized element" warnings
    private static final Vector<String> suppressSpecExtensionWarnings =
            new Vector<>();

    public static void main(String[] args) {
        Options options = new Options();
        CommandLine argsParsedWithOptions = parseCommandlineOptions(args, options);
        processCommandlineOptions(argsParsedWithOptions, options);

        // get unproccessed command-line arguments
        String[] leftOverArgs = argsParsedWithOptions.getArgs();

        // Dispatch on remaining command-line argument
        if (leftOverArgs.length == 0 || leftOverArgs[0].equals("schema")) {
            FormDef form = loadFormDef(leftOverArgs);
            processSchema(form);
        } else if (leftOverArgs[0].equals("summary")) {
            FormDef form = loadFormDef(leftOverArgs);
            System.out.println(FormOverview.overview(form));
        } else if (leftOverArgs[0].equals("csvdump")) {
            FormDef form = loadFormDef(leftOverArgs);
            try {
                System.out.println(FormTranslationFormatter.dumpTranslationsIntoCSV(form));
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (leftOverArgs[0].equals("csvimport")) {
            csvImport(leftOverArgs);
        } else if (leftOverArgs[0].equals("validatemodel")) {
            validateModel(leftOverArgs[1], leftOverArgs[2]);
        } else if (leftOverArgs[0].equals("validate")) {
            validateForm(leftOverArgs);
        } else if (leftOverArgs[0].equals("runinstance")) {
            // load a form and an incomplete instance into the form player
            XFormPlayer xfp = new XFormPlayer(System.in, System.out, null);
            xfp.start(loadFormAndInstance(leftOverArgs[1], leftOverArgs[2]));
        } else if (leftOverArgs[0].equals("run")) {
            XFormPlayer xfp = new XFormPlayer(System.in, System.out, null);
            try {
                xfp.start(leftOverArgs[1]);
            } catch (FileNotFoundException e) {
                System.out.println("File not found at " + args[0] + "!!!!");
            }

        } else {
            printHelpMessage(options);
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Setup command-line options and parse them.
     *
     * @param args    String array of command-line arguments passed in.
     * @param options Options object that we add to in this function
     * @return CommandLine parsed options
     */
    private static CommandLine parseCommandlineOptions(String[] args, Options options) {
        options.addOption(Option.builder("E").argName("namespace=tag1,...,tagN")
                .valueSeparator('=')
                .numberOfArgs(2)
                .desc("comma-delimited list of reserved tags at given namespace for the parser to expect")
                .build());
        options.addOption(Option.builder("S").argName("namespace")
                .hasArg()
                .desc("suppress warnings when parser encounters elements at given namespace")
                .build());
        options.addOption(Option.builder("I").argName("namespace")
                .hasArg()
                .desc("continue parsing inner elements of unrecognized tag at given namespace")
                .build());
        options.addOption(new Option("help", "print this message"));

        CommandLineParser parser = new DefaultParser();

        try {
            return parser.parse(options, args);
        } catch (ParseException exp) {
            System.err.println("Parsing failed: " + exp.getMessage());
            System.exit(1);
        }
        return null;
    }

    /**
     * Setup local variables based on parsed command-line options.
     *
     * @param argsParsedWithOptions are command-line arguments that have been
     *                              processed with the options passed in.
     * @param options               command-line options used for printing help message
     */
    private static void processCommandlineOptions(CommandLine argsParsedWithOptions, Options options) {
        if (argsParsedWithOptions.hasOption("help")) {
            printHelpMessage(options);
            System.exit(0);
        }

        // read in specification extension keywords from command-line options
        Properties extensions = argsParsedWithOptions.getOptionProperties("E");
        if (extensions != null) {
            Enumeration<?> properties = extensions.propertyNames();
            while (properties.hasMoreElements()) {
                String namespace = (String)properties.nextElement();
                String tagString = extensions.getProperty(namespace);
                String[] tagsArr = tagString.split(",");
                Vector<String> tags = new Vector<>();
                for (int i = 0; i < tagsArr.length; i++) {
                    tags.add(tagsArr[i]);
                }
                specExtensionKeywords.put(namespace, tags);
            }
        }

        // read in namespace warning suppression for specification extensions
        // from command-line options
        Properties namespaceWarningSupression = argsParsedWithOptions.getOptionProperties("S");
        if (namespaceWarningSupression != null) {
            Enumeration<?> properties = namespaceWarningSupression.propertyNames();
            while (properties.hasMoreElements()) {
                String namespace = (String)properties.nextElement();
                suppressSpecExtensionWarnings.add(namespace);
            }
        }

        // read in inner element parsing logic for specification extensions
        // from command-line options
        Properties namespaceParseInner = argsParsedWithOptions.getOptionProperties("I");
        if (namespaceParseInner != null) {
            Enumeration<?> properties = namespaceParseInner.propertyNames();
            while (properties.hasMoreElements()) {
                String namespace = (String)properties.nextElement();
                parseSpecExtensionsInnerElements.add(namespace);
            }
        }
    }

    /**
     * Print help message for command-line argument options.
     *
     * @param options command-line options used for printing help message
     */
    private static void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar form_translate.jar [OPTION] ... validate \n" +
                "or: java -jar form_translate.jar [ schema | summary | csvdump] \n" +
                "or: java -jar form_translate.jar csvimport [delimeter] [encoding] [outcoding] < translations.csv > itextoutput \n" +
                "or: java -jar form_translate.jar validatemodel /path/to/xform /path/to/instance", options);
    }

    /**
     * Read in form from standard input or filename argument and run it through
     * XForm parser, logging errors along the way.
     *
     * @param args is an String array, where the first entry, if present will
     *             be treated as a filename.
     */
    private static void validateForm(String[] args) {
        InputStream inputStream = System.in;

        // If command line args non-empty, treat first entry as filename we
        // open to get the form.
        if (args.length > 1) {
            String formPath = args[1];

            try {
                inputStream = new FileInputStream(formPath);
            } catch (FileNotFoundException e) {
                System.err.println("Couldn't find file at: " + formPath);
                System.exit(1);
            }
        }
        PrintStream defaultOutStream = System.out;
        PrintStream responseStream;
        // Redirect output to syserr because sysout is being used for the
        // response, and must be kept clean.
        try {
            responseStream = new PrintStream(System.out, false, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            responseStream = System.out;
        }
        System.setOut(System.err);

        InputStreamReader isr;
        try {
            isr = new InputStreamReader(inputStream, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            System.out.println("UTF 8 encoding unavailable, trying default encoding");
            isr = new InputStreamReader(inputStream);
        }

        try {
            JSONReporter reporter = new JSONReporter();
            try {
                XFormParser parser = new XFormParser(isr);
                // setup xformparser with options parsed from command-line
                parser.setupAllSpecExtensions(specExtensionKeywords,
                        suppressSpecExtensionWarnings,
                        parseSpecExtensionsInnerElements);
                parser.attachReporter(reporter);
                parser.parse();

                reporter.setPassed();
            } catch (IOException e) {
                // Rethrow this. This is probably a failure of the system, not the form
                reporter.setFailed(e);
                System.err.println("IO Exception while processing form");
                e.printStackTrace();
                System.exit(1);
            } catch (XFormParseException xfpe) {
                reporter.setFailed(xfpe);
            } catch (Exception e) {
                reporter.setFailed(e);
            }

            responseStream.print(reporter.generateJSONReport());
        } finally {
            try {
                isr.close();
                // reset output stream on exit
                System.setOut(defaultOutStream);
            } catch (IOException e) {
                System.err.println("IO Exception while closing stream.");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void validateModel(String formPath, String modelPath) {
        FileInputStream formInput = null;
        FileInputStream instanceInput = null;

        try {
            formInput = new FileInputStream(formPath);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find file at: " + formPath);
            System.exit(1);
        }

        try {
            instanceInput = new FileInputStream(modelPath);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find file at: " + modelPath);
            System.exit(1);
        }

        try {
            FormInstanceValidator validator =
                    new FormInstanceValidator(formInput, instanceInput);
            validator.simulateEntryTest();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Form instance appears to be valid");
    }

    /**
     * Build a form definition and load a particular instance into it.
     *
     * @param formPath     Filepath to XML form definition
     * @param instancePath Filepath to XML form instance
     * @return The form definition with the given instance loaded. Triggers a
     * system exit if any problems are encountered.
     */
    private static FormDef loadFormAndInstance(String formPath, String instancePath) {
        FileInputStream formInput = null;
        FileInputStream instanceInput = null;

        try {
            formInput = new FileInputStream(formPath);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find file at: " + formPath);
            System.exit(1);
        }

        try {
            instanceInput = new FileInputStream(instancePath);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find file at: " + instancePath);
            System.exit(1);
        }

        FormDef formDef = null;
        try {
            formDef = FormInstanceLoader.loadInstance(formInput, instanceInput);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (formDef == null) {
            System.exit(1);
        }

        return formDef;
    }


    private static void csvImport(String[] args) {
        // TODO: refactor so that instead of passing in args, we just pass in
        // individual arguments
        if (args.length > 1) {
            String delimeter = args[1];
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out,
                    delimeter, null, null);
        } else if (args.length > 2) {
            String delimeter = args[1];
            String encoding = args[2];
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out,
                    delimeter, encoding, null);
        } else if (args.length > 3) {
            String delimeter = args[1];
            String incoding = args[2];
            String outcoding = args[3];
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out,
                    delimeter, incoding, outcoding);
        } else {
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out);
        }
    }

    private static void processSchema(FormDef form) {
        Document schemaDoc = InstanceSchema.generateInstanceSchema(form);
        KXmlSerializer serializer = new KXmlSerializer();
        try {
            serializer.setOutput(System.out, null);
            schemaDoc.write(serializer);
            serializer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function to load the form definition from a filepath.
     *
     * @param args is an String array, where the second entry, if present will
     *             be treated as a filename.
     * @return FormDef loaded from filepath present in the argument array
     */
    private static FormDef loadFormDef(String[] args) {
        // Redirect output to syserr because sysout is being used for the
        // response, and must be kept clean.
        PrintStream responseStream = System.out;
        System.setOut(System.err);

        InputStream inputStream = System.in;

        // open form file
        if (args.length > 1) {
            String formPath = args[1];

            try {
                inputStream = new FileInputStream(formPath);
            } catch (FileNotFoundException e) {
                System.out.println("Couldn't find file at: " + formPath);
                System.exit(1);
            }
        }

        FormDef form = XFormUtils.getFormFromInputStream(inputStream);

        // reset the system output on exit.
        System.setOut(responseStream);

        return form;
    }
}
