package de.krkm.utilities.owlsubpropertycycleremover;

import de.krkm.utilities.owlsubpropertycycleremover.cycletype.CycleType;
import de.krkm.utilities.owlsubpropertycycleremover.cycletype.SubClassOfCycleType;
import de.krkm.utilities.owlsubpropertycycleremover.cycletype.SubPropertyOfCycleType;
import org.apache.commons.cli.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements CLI for OntologyCycleRemover
 */
public class Main {
    @SuppressWarnings("AccessStaticViaInstance")
    public static void main(String[] args) throws IOException, OWLOntologyCreationException,
            OWLOntologyStorageException {
        Options options = new Options();
        options.addOption(
                OptionBuilder.withLongOpt("subpropertyof").withDescription("remove subpropertyof cycles").create("sp"));
        options.addOption(
                OptionBuilder.withLongOpt("subclassof").withDescription("remove subclassof cycles").create("sc"));
        options.addOption(OptionBuilder.withLongOpt("input").withDescription("ontology to remove cycles from").hasArg()
                                       .withArgName("FILENAME").isRequired().create("i"));
        options.addOption(
                OptionBuilder.withLongOpt("output").withDescription("file to write result ontology to").hasArg()
                             .withArgName("FILENAME").isRequired().create("o"));
        options.addOption(OptionBuilder.withLongOpt("log").withDescription("file to log removed axioms to").hasArg()
                                       .withArgName("FILENAME").create("l"));
        Option iriOption = OptionBuilder.withDescription("list IRI to search for confidence values, separated by |")
                                  .hasArgs().withArgName("IRI").withValueSeparator('|').create("iri");
        options.addOption(iriOption);

        CommandLineParser parser = new PosixParser();
        CommandLine line;
        try {
            line = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.err.println("Unable to parse arguments: " + e.getMessage());
            new HelpFormatter().printHelp("java -cp ... " + Main.class.getCanonicalName(), options);
            System.exit(1);
            return;
        }

        String inputFileName = line.getOptionValue("i");
        String outputFileName = line.getOptionValue("o");
        OutputStream logFileStream = null;

        ArrayList<CycleType<?>> cycleTypes = new ArrayList<CycleType<?>>();
        if (line.hasOption("sp")) {
            cycleTypes.add(new SubPropertyOfCycleType());
        }
        if (line.hasOption("sc")) {
            cycleTypes.add(new SubClassOfCycleType());
        }

        List<IRI> confidenceIRIs = null;
        if (line.hasOption("iri")) {

            for (String val : (List<String>) iriOption.getValuesList()) {

            }
        }


        FileInputStream in = new FileInputStream(inputFileName);
        OntologyCycleRemover r = new OntologyCycleRemover(in);
        in.close();
        if (line.hasOption("l")) {
            logFileStream = new FileOutputStream(line.getOptionValue("l"));
            r.setRemovedAxiomsStream(logFileStream);
        }

        for (CycleType<?> ct : cycleTypes) {
            r.clean(ct);
        }

        FileOutputStream out = new FileOutputStream(outputFileName);
        r.saveOntology(out);
        out.close();
        if (logFileStream != null) {
            r.closeLog();
        }
    }
}