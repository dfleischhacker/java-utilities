package de.krkm.utilities.owlconfidencehistogram;

import de.krkm.utilities.annotatedaxiomextractor.AnnotatedAxiomExtractor;
import org.apache.commons.cli.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.*;
import java.util.ArrayList;

/**
 * Provides the command-line entry point for the confidence value extractor
 */
public class Main {
    @SuppressWarnings("AccessStaticViaInstance")
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("ontology").hasArg().isRequired()
                                       .withArgName("FILENAME")
                                       .withDescription("file to read ontology from").create("o"));
        options.addOption(OptionBuilder.withLongOpt("out").hasArg().isRequired()
                                       .withArgName("DIRECTORY")
                                       .withDescription("directory to write confidence value files to").create
                        ("out"));


        CommandLineParser parser = new PosixParser();
        ArrayList<IRI> iris = new ArrayList<IRI>();
        iris.add(IRI.create("http://ki.informatik.uni-mannheim.de/gold-miner/annotations#confidence"));
        iris.add(IRI.create("http://www.dl-learner.org/enrichment.owl#confidence"));

        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.err.println("Unable to parse arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar ... " + Main.class.getCanonicalName(), options);
            System.exit(1);
        }

        String inFileName = line.getOptionValue("o");
        String outDirName = line.getOptionValue("out");

        FileInputStream in = null;
        try {
            in = new FileInputStream(inFileName);
        }
        catch (FileNotFoundException e) {
            System.err.format("Unable to open file '%s': %s", inFileName, e.getMessage());
            System.exit(2);
        }

        File outDir = new File(outDirName);

        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
                System.err.println("Unable to create non-existing directory " + outDir.getAbsolutePath());
                System.exit(1);
            }
        }

        try {
            ConfidenceValueExtractor cve = new ConfidenceValueExtractor(in, AnnotatedAxiomExtractor
                    .getAnnotationsProperties(
                            OWLManager.getOWLDataFactory(), iris.toArray(new IRI[iris.size()])));

            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(outDir.getAbsolutePath() + File.separator + "aggregated.txt"));

            for (Double val : cve.getConfidenceValues()) {
                writer.write(String.valueOf(val));
                writer.newLine();
            }
            writer.close();

            for (AxiomType<?> type : cve.getUsedAxiomTypes()) {
                writer = new BufferedWriter(
                        new FileWriter(outDir.getAbsolutePath() + File.separator + filterAxiomTypeName(type) + "" +
                                               ".txt"));

                for (Double val : cve.getConfidenceValues()) {
                    writer.write(String.valueOf(val));
                    writer.newLine();
                }
                writer.close();
            }

        }
        catch (OWLOntologyCreationException e) {
            System.err.println("Unable to load ontology: " + e.getMessage());
        }
        catch (IOException e) {
            System.err.println("Unable to write output file: " + e.getMessage());
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ignored) {

            }
        }
    }

    /**
     * Filter the AxiomType names for usage in filenames
     *
     * @param axiomType the axiomtype to get a file name for
     * @return filtered name of axiom type
     */
    public static String filterAxiomTypeName(AxiomType<?> axiomType) {
        return axiomType.getName().replaceAll("[^A-Za-z0-9]+", "_");
    }
}
