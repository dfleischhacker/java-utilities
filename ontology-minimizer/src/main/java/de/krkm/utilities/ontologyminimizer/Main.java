package de.krkm.utilities.ontologyminimizer;


import org.apache.commons.cli.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.*;

/**
 * Implements entry point for command-line interface of the ontology minimizer application
 */
public class Main {
    @SuppressWarnings("AccessStaticViaInstance")
    public static void main(String[] args) throws Exception {
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

            if (iriStrings == null) {
                iriStrings = new String[2];
                iriStrings[0] = "http://ki.informatik.uni-mannheim.de/gold-miner/annotations#confidence";
                iriStrings[1] = "http://www.dl-learner.org/enrichment.owl#confidence";
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology annotatedOntology = manager.loadOntologyFromOntologyDocument(annotatedStream);
            OntologyMinimizer minimizer = new OntologyMinimizer(annotatedOntology, iriStrings);

            manager.removeOntology(annotatedOntology);
            manager.getOWLDataFactory().purge();

            OWLOntology coherentOntology = manager.loadOntologyFromOntologyDocument(coherentStream);
            if (line.hasOption("s")) {
                minimizer.setSnapShotDir(new File(line.getOptionValue("s")));
            }

            removedAxiomStream = null;
            if (line.hasOption("l")) {
                removedAxiomStream = new FileOutputStream(line.getOptionValue("l"));
            }
            OWLOntology minimizedOntology = minimizer.startMinimization(coherentOntology, removedAxiomStream);
            try {
                manager.saveOntology(minimizedOntology, outputStream);
                removedAxiomStream.close();
            }
            catch (Exception e) {
                System.err.println("Unable to save generated ontology: " + e.getMessage());
                throw e;
            }
        }
        catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar " + Main.class.getCanonicalName(), options);
            return;
        }
        catch (FileNotFoundException e) {
            System.err.println("Error opening file: " + e.getMessage());
            throw e;
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
