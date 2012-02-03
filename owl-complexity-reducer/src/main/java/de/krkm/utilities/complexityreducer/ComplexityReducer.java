package de.krkm.utilities.complexityreducer;

import com.clarkparsia.pellet.owlapiv3.PelletLoader;
import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.mindswap.pellet.KnowledgeBase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;

/**
 * Uses the Pellet reasoner to reduce the complexity of an OWL Full ontology to OWL DL. This is done by ignoring some
 * axioms
 */
public class ComplexityReducer {
    private final static Logger log = LoggerFactory.getLogger(ComplexityReducer.class);

    public ComplexityReducer() {
    }

    @SuppressWarnings("AccessStaticViaInstance")
    public static void main(String[] args) throws OWLOntologyCreationException, IOException,
                                                  OWLOntologyStorageException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("input").withDescription("ontology to reduce").isRequired()
                                       .hasArg().withArgName("FILENAME").create("i"));
        options.addOption(
            OptionBuilder.withLongOpt("output").withDescription("filename to write reduced ontology to").isRequired()
                         .hasArg().withArgName("FILENAME").create("o"));

        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.err.println("Unable to parse arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar " + ComplexityReducer.class.getCanonicalName(), options);

            System.exit(1);
        }

        String inputFileName = line.getOptionValue("i");
        System.out.println(inputFileName);

        InputStream input;

        if (inputFileName.endsWith("bz2")) {
            log.info("Decompressing from bzip2 format for file '{}'", inputFileName);
            input = new BZip2CompressorInputStream(new FileInputStream(inputFileName));
        }
        else {
            log.info("Uncompressed input for file '{}'", inputFileName);
            input = new FileInputStream(inputFileName);
        }

        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(input);
        reduce(ontology);

        String outputFileName = line.getOptionValue("o");
        OutputStream output;
        if (outputFileName.endsWith("bz2")) {
            log.info("Compressing output '{}'", outputFileName);
            output = new BZip2CompressorOutputStream(new FileOutputStream(outputFileName));
        }
        else {
            log.info("Uncompressed output to '{}'", outputFileName);
            output = new FileOutputStream(outputFileName);
        }

        manager.saveOntology(ontology, output);
        input.close();
        output.close();
    }

    /**
     * Removes all axioms unsupported by Pellet from the given ontology.
     *
     * Note: This method modifies the given ontology!
     *
     * @param ontology ontology to reduce, will be modified
     */
    public static void reduce(OWLOntology ontology) {
        log.info("Starting reduction step");
        log.info("Ontology has {} axioms", ontology.getAxiomCount());
        log.info("Loading ontology into PelletLoader");
        PelletLoader loader = new PelletLoader(new KnowledgeBase());
        loader.load(Collections.singleton(ontology));
        log.info("Done loading ontology");

        log.info("Found {} unsupported axioms", loader.getUnsupportedAxioms().size());

        for (OWLAxiom ax : loader.getUnsupportedAxioms()) {
            log.debug("Removing axiom '{}'", ax);
            ontology.getOWLOntologyManager().removeAxiom(ontology, ax);
        }

        log.info("Reduced ontology has {} axioms", ontology.getAxiomCount());
    }
}
