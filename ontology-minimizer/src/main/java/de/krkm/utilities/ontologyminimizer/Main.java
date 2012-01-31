package de.krkm.utilities.ontologyminimizer;


import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Implements entry point for command-line interface of the ontology minimizer application
 */
public class Main {
    public static void main(String[] args) {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "shows this help message");
        options.addOption(OptionBuilder.withLongOpt("coherent").isRequired()
                                       .withDescription("coherent ontology to use as input")
                                       .hasArg().withArgName("FILENAME").create("c"));
        options.addOption(OptionBuilder.withLongOpt("annotated").isRequired()
                                       .withDescription("annotated ontology to retrieve confidence values from")
                                       .hasArg().withArgName("FILENAME").create("a"));
        options.addOption(OptionBuilder.withLongOpt("output").isRequired()
                                       .withDescription("file to write generated ontology to")
                                       .hasArg().withArgName("FILENAME").create("o"));
        options.addOption(OptionBuilder.withLongOpt("snapshot")
                                       .withDescription("directory to write snapshots to")
                                       .hasArg().withArgName("DIRECTORY").create("s"));

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar " + Main.class.getCanonicalName(), options);
                System.exit(0);
            }

            FileInputStream annotatedStream = new FileInputStream(line.getOptionValue("a"));
            FileInputStream coherentStream = new FileInputStream(line.getOptionValue("c"));
            FileOutputStream outputStream = new FileOutputStream(line.getOptionValue("o"));

            File snapShotDir = null;
            if (line.hasOption("s")) {
                snapShotDir = new File(line.getOptionValue("s"));
            }

            OntologyMinimizer minimizer =
                new OntologyMinimizer(coherentStream, annotatedStream, outputStream, snapShotDir);
        }
        catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar " + Main.class.getCanonicalName(), options);
            System.exit(2);
        }
        catch (FileNotFoundException e) {
            System.err.println("Error opening file: " + e.getMessage());
            System.exit(2);
        }
    }
}
