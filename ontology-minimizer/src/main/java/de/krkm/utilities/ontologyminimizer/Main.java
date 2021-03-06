package de.krkm.utilities.ontologyminimizer;


import org.apache.commons.cli.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.*;
import java.util.ArrayList;

/**
 * Implements entry point for command-line interface of the ontology minimizer application
 */
public class Main {
    @SuppressWarnings("AccessStaticViaInstance")
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
        options.addOption(
                OptionBuilder.withLongOpt("confiri").withDescription("IRIs of confidence annotations").hasArgs()
                             .withArgName("IRI").create("conf"));
        options.addOption(OptionBuilder.withLongOpt("log").hasArg().withDescription("file to write removed axioms to")
                                       .withArgName("FILENAME").create("l"));

        FileOutputStream removedAxiomStream = null;
        FileInputStream annotatedStream = null;
        FileInputStream coherentStream = null;
        FileOutputStream outputStream = null;
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar " + Main.class.getCanonicalName(), options);
                System.exit(0);
            }



            annotatedStream = new FileInputStream(line.getOptionValue("a"));
            coherentStream = new FileInputStream(line.getOptionValue("c"));
            outputStream = new FileOutputStream(line.getOptionValue("o"));

            String[] iriStrings = line.getOptionValues("conf");

            ArrayList<IRI> iris = new ArrayList<IRI>();
            if (iriStrings == null) {
                iris.add(IRI.create("http://ki.informatik.uni-mannheim.de/gold-miner/annotations#confidence"));
                iris.add(IRI.create("http://www.dl-learner.org/enrichment.owl#confidence"));
            } else {
                for (String iri : iriStrings) {
                    iris.add(IRI.create(iri));
                }
            }

            File snapShotDir = null;
            if (line.hasOption("s")) {
                snapShotDir = new File(line.getOptionValue("s"));
            }


            OntologyMinimizer minimizer =
                    new OntologyMinimizer(coherentStream, annotatedStream, outputStream, iris, snapShotDir);

            removedAxiomStream = null;
            if (line.hasOption("l")) {
                removedAxiomStream = new FileOutputStream(line.getOptionValue("l"));
                minimizer.setRemovedAxiomsStream(removedAxiomStream);
            }
            minimizer.startMinimization();
            try {
                minimizer.saveGeneratedOntology();
                removedAxiomStream.close();
            }
            catch (OWLOntologyStorageException e) {
                System.err.println("Unable to save generated ontology: " + e.getMessage());
                System.exit(4);
            }
            catch (IOException e) {
                System.err.println("Unable to save generated ontology: " + e.getMessage());
                System.exit(4);
            }
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
        catch (OntologyMinimizationException e) {
            System.err.println("Unable to minimize ontology: " + e.getMessage());
            System.exit(3);
        }
        finally {
            try {
                if (removedAxiomStream != null) {
                    removedAxiomStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }

                if (annotatedStream != null) {
                    annotatedStream.close();
                }

                if (coherentStream != null) {
                    coherentStream.close();
                }
            }
            catch (IOException ignored) {

            }
        }
    }
}
