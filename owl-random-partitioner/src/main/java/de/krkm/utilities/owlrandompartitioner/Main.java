package de.krkm.utilities.owlrandompartitioner;

import org.apache.commons.cli.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.*;
import java.util.Set;

/**
 * Entry point of RandomOntologyPartitioner for CLI
 */
public class Main {
    @SuppressWarnings("AccessStaticViaInstance")
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(
                OptionBuilder.withLongOpt("original").withDescription("original ontology to partition").hasArg()
                             .withArgName("FILENAME").isRequired().create("o"));
        options.addOption(
                OptionBuilder.withLongOpt("targetDir").withDescription("directory to write partitions to").hasArg()
                             .withArgName("DIRECTORY").isRequired().create("t"));
        options.addOption(
                OptionBuilder.withLongOpt("partitions").withDescription("number of partitions").hasArg()
                             .withArgName("NUMBER").isRequired().create("n"));
        options.addOption(
                OptionBuilder.withLongOpt("base").withDescription("base ontology to add to each partition").hasArg()
                             .withArgName("FILENAME").create("b"));

        HelpFormatter formatter = new HelpFormatter();

        CommandLineParser parser = new PosixParser();

        CommandLine line;
        try {
            line = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.err.println("Unable to parse command-line arguments: " + e.getMessage());
            formatter.printHelp(Main.class.getCanonicalName(), options);
            return;
        }

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        File targetDir = new File(line.getOptionValue("t"));

        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                System.err
                      .format("Target dir '%s' does not exist and not able to create it!",
                              targetDir.getAbsolutePath());
                return;
            }
        }

        FileInputStream originalOntologyStream = null;
        OWLOntology originalOntology;

        try {
            originalOntologyStream = new FileInputStream(line.getOptionValue("o"));
            originalOntology = manager.loadOntologyFromOntologyDocument(originalOntologyStream);
        }
        catch (FileNotFoundException e) {
            System.err.format("Unable to load original ontology '%s': %s\n", line.getOptionValue("o"), e.getMessage());
            return;
        }
        catch (OWLOntologyCreationException e) {
            System.err.format("Unable to load original ontology '%s': %s\n", line.getOptionValue("o"), e.getMessage());
            return;
        }
        finally {
            if (originalOntologyStream != null) {
                try {
                    originalOntologyStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        FileInputStream baseOntologyStream;
        OWLOntology baseOntology = null;
        if (line.hasOption("b")) {
            try {
                baseOntologyStream = new FileInputStream(line.getOptionValue("b"));
                baseOntology = manager.loadOntologyFromOntologyDocument(baseOntologyStream);
            }
            catch (FileNotFoundException e) {
                System.err.format("Unable to load base ontology '%s': %s\n", line.getOptionValue("b"), e.getMessage());
                return;
            }
            catch (OWLOntologyCreationException e) {
                System.err.format("Unable to load base ontology '%s': %s\n", line.getOptionValue("b"), e.getMessage());
                return;
            }
        }

        RandomOntologyPartitioner rop = new RandomOntologyPartitioner(originalOntology, baseOntology);

        int numberOfPartitions = Integer.parseInt(line.getOptionValue("n"));

        Set<OWLOntology> partitions;
        try {
            partitions = rop.partition(numberOfPartitions);
        }
        catch (OntologyPartitioningException e) {
            System.err.println("Unable to create partitions: " + e.getMessage());
            return;
        }

        int writeCounter = 0;

        for (OWLOntology ontology : partitions) {
            String outFileName = targetDir.getAbsolutePath() + File.separator + "partition_" + writeCounter + ".owl";
            try {
                FileOutputStream out;
                System.out.printf("Saving ontology '%s'\n", outFileName);
                out = new FileOutputStream(outFileName);
                manager.saveOntology(ontology, new RDFXMLOntologyFormat(), out);
                out.close();
            }
            catch (OWLOntologyStorageException e) {
                System.err.format("Unable to save partition to file '%s': %s\n", outFileName, e.getMessage());
            }
            catch (IOException e) {
                System.err.format("Unable to save partition to file '%s': %s\n", outFileName, e.getMessage());
            }
            writeCounter++;
        }
    }
}
