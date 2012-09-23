package de.krkm.utilities.owlcompare.experiments;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import de.krkm.trex.reasoner.TRexReasoner;
import de.krkm.utilities.owlcompare.PelletWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.BufferingMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import static java.nio.charset.Charset.defaultCharset;

public class KI2012Experiments {
    private File ontologyDirectory;
    private File resultsDirectory;
    private ResultsWriter writer;

    public static void main(String[] args) throws IOException, OWLOntologyCreationException {
        if (args.length != 2) {
            System.out.println("Usage: <inputDir> <outputDir>");
            System.exit(1);
        }
        KI2012Experiments experiments = new KI2012Experiments(new File(args[0]), new File(args[1]));
        experiments.runAllExperiments();
    }

    public KI2012Experiments(File ontologyDirectory, File resultsDirectory) {
        this.resultsDirectory = resultsDirectory;
        this.ontologyDirectory = ontologyDirectory;
        this.writer = new ResultsWriter(
                new String[]{"pelletUnsatRuntime", "pelletAllRuntime", "pelletNoUnsatClasses", "pelletNoUnsatProp",
                        "trexUnsatRuntime", "trexAllRuntime", "trexNoUnsatClasses", "trexNoUnsatProp"});
    }

    public void runAllExperiments() throws IOException, OWLOntologyCreationException {
        if (!ontologyDirectory.exists()) {
            throw new RuntimeException(ontologyDirectory.getAbsolutePath() + " does not exist!");
        }
        File[] files = ontologyDirectory.listFiles();
        for (File ontologyFile : files) {
            runExperimentsOnOntology(ontologyFile);
        }
    }

    public void runExperimentsOnOntology(File ontologyFile) throws IOException, OWLOntologyCreationException {
        File resFile = new File(resultsDirectory.getAbsolutePath() + File.separator + ontologyFile.getName() + ".txt");
        FileOutputStream resStream = new FileOutputStream(resFile);

        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);

        runUnsatClassesTrex(resStream, ontology);
        runUnsatClassesPellet(resStream, ontology);

        runAllTrex(resStream, ontology);
        runAllPellet(resStream, ontology);

        resStream.close();
    }

    private void runUnsatClassesTrex(FileOutputStream resStream, OWLOntology ontology) throws IOException {
        StringBuilder sb = new StringBuilder();

        long startTime = System.currentTimeMillis();
        sb.append("ExplanationsUnsatConceptsTrex: Started at ").append(startTime).append("\n");
        TRexReasoner trex = new TRexReasoner(ontology, true);
        sb.append("ExplanationsUnsatConcepts: TRex Init done at ").append(System.currentTimeMillis()).append("\n");

        Set<OWLClass> trexIncoherentClasses = trex.getIncoherentClasses();
        for (OWLClass incohClass : trexIncoherentClasses) {
            int classId = trex.getNamingManager().getConceptId(incohClass.getIRI().toString());
            trex.conceptDisjointness.getExplanation(classId, classId);
        }
        int totalRuntime = toSeconds(System.currentTimeMillis() - startTime);
        sb.append("ExplanationsUnsatConceptsTrex: Total reasoning time: ")
          .append(totalRuntime).append("\n");
        sb.append("ExplanationsUnsatConceptsTrex: Got all incoherent classes at ").append(System.currentTimeMillis())
          .append("\n");
        sb.append("ExplanationsUnsatConceptsTrex: Number of incoherent classes is ")
          .append(trexIncoherentClasses.size()).append("\n");


        sb.append("\n");
        resStream.write(sb.toString().getBytes(defaultCharset()));
    }

    private void runAllTrex(FileOutputStream resStream, OWLOntology ontology) throws IOException {
        StringBuilder sb = new StringBuilder();

        long startTime = System.currentTimeMillis();
        sb.append("ExplanationsAllTrex: Started at ").append(startTime).append("\n");
        TRexReasoner trex = new TRexReasoner(ontology);
        sb.append("ExplanationsAllTrex: TRex Init done at ").append(System.currentTimeMillis()).append("\n");

        Set<OWLClass> trexIncoherentClasses = trex.getIncoherentClasses();
        for (OWLClass incohClass : trexIncoherentClasses) {
            int classId = trex.getNamingManager().getConceptId(incohClass.getIRI().toString());
            trex.conceptDisjointness.getExplanation(classId, classId);
        }

        Set<OWLObjectProperty> trexIncoherentProperties = trex.getIncoherentProperties();
        for (OWLObjectProperty incohClass : trexIncoherentProperties) {
            int classId = trex.getNamingManager().getPropertyId(incohClass.getIRI().toString());
            trex.propertyDisjointness.getExplanation(classId, classId);
        }

        sb.append("ExplanationsAllTrex: Total reasoning time before cycles: ")
          .append(toSeconds(System.currentTimeMillis() - startTime))
          .append("\n");

        Set<OWLClass> trexConceptCycles = trex.getConceptCycles();
        for (OWLClass incohClass : trexConceptCycles) {
            int classId = trex.getNamingManager().getConceptId(incohClass.getIRI().toString());
            trex.conceptSubsumption.getExplanation(classId, classId);
        }

        Set<OWLObjectProperty> trexPropertyCycles = trex.getPropertyCycles();
        for (OWLObjectProperty incohClass : trexPropertyCycles) {
            int classId = trex.getNamingManager().getPropertyId(incohClass.getIRI().toString());
            trex.propertySubsumption.getExplanation(classId, classId);
        }

        sb.append("ExplanationsAllTrex: Total reasoning time after cycles: ")
          .append(toSeconds(System.currentTimeMillis() - startTime))
          .append("\n");
        sb.append("ExplanationsAllTrex: Got all incoherent classes at ").append(System.currentTimeMillis()).append("\n");
        sb.append("ExplanationsAllTrex: Number of incoherent classes is ").append(trexIncoherentClasses.size())
          .append("\n");


        sb.append("\n");
        resStream.write(sb.toString().getBytes(defaultCharset()));
    }

    private void runUnsatClassesPellet(FileOutputStream resStream, OWLOntology ontology) throws IOException {
        StringBuilder sb = new StringBuilder();

        long startTime = System.currentTimeMillis();
        sb.append("ExplanationsUnsatConceptsPellet: Started at ").append(startTime).append("\n");
        PelletReasoner pelletReasoner = new PelletReasoner(ontology, BufferingMode.BUFFERING);
        PelletWrapper pellet = new PelletWrapper(pelletReasoner);
        PelletExplanation expl = new PelletExplanation(pelletReasoner);
        sb.append("ExplanationsUnsatConceptsPellet: Pellet Init done at ").append(System.currentTimeMillis()).append("\n");

        Set<OWLClass> trexIncoherentClasses = null;
        int errorCount = 0;
        try {
            trexIncoherentClasses = pellet.getUnsatisfiableConcepts();
        }
        catch (Throwable t) {
            sb.append("ExplanationsUnsatConceptsPellet: Unable to get unsat classes for ").append("\n");
            sb.append("\n");
            resStream.write(sb.toString().getBytes(defaultCharset()));
            return;
        }
        for (OWLClass incohClass : trexIncoherentClasses) {
            try {
                expl.getUnsatisfiableExplanations(incohClass);
            }
            catch (Throwable t) {
//                sb.append("ExplanationsUnsatConceptsPellet: Unable to get explanation for ").append(incohClass)
//                  .append(": ").append(t
//                        .getMessage()).append("\n");
                errorCount++;
            }
        }
        sb.append("ExplanationsUnsatConceptsPellet: Total reasoning time: ")
          .append(toSeconds(System.currentTimeMillis() - startTime)).append("\n");
        sb.append("ExplanationsUnsatConceptsPellet: Got all incoherent classes at ").append(System.currentTimeMillis())
          .append("\n");
        sb.append("ExplanationsUnsatConceptsPellet: Number of incoherent classes is ")
          .append(trexIncoherentClasses.size()).append("\n");
        sb.append("ExplanationsUnsatConceptsPellet: Errors getting explanations ").append(errorCount).append("\n");

        sb.append("\n");
        resStream.write(sb.toString().getBytes(defaultCharset()));
    }

    private void runAllPellet(FileOutputStream resStream, OWLOntology ontology) throws IOException {
        StringBuilder sb = new StringBuilder();

        long startTime = System.currentTimeMillis();
        sb.append("ExplanationsAllPellet: Started at ").append(startTime).append("\n");
        PelletReasoner pelletReasoner = new PelletReasoner(ontology, BufferingMode.BUFFERING);
        PelletWrapper pellet = new PelletWrapper(pelletReasoner);
        PelletExplanation expl = new PelletExplanation(pelletReasoner);
        sb.append("ExplanationsAllPellet: Pellet Init done at ").append(System.currentTimeMillis()).append("\n");

        Set<OWLClass> trexIncoherentClasses = null;
        int errorCount = 0;
        try {
            trexIncoherentClasses = pellet.getUnsatisfiableConcepts();
        }
        catch (Throwable t) {
            sb.append("ExplanationsUnsatConceptsPellet: Unable to get unsat classes for ").append("\n");
            sb.append("\n");
            resStream.write(sb.toString().getBytes(defaultCharset()));
            return;
        }
        for (OWLClass incohClass : trexIncoherentClasses) {
            try {
                expl.getUnsatisfiableExplanations(incohClass);
            }
            catch (Throwable t) {
//                sb.append("ExplanationsAllPellet: Unable to get explanation for ").append(incohClass)
//                  .append(": ").append(t
//                        .getMessage()).append("\n");
                errorCount++;
            }
        }

        Set<OWLObjectProperty> trexIncoherentProperties;
        try {
            trexIncoherentProperties = pellet.getUnsatisfiableProperties();
        }
        catch (Throwable t) {
            sb.append("ExplanationsUnsatConceptsPellet: Unable to get unsat properties for ").append("\n");
            return;
        }
        for (OWLObjectProperty incohProperty : trexIncoherentProperties) {
            OWLDataFactory owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
            try {
                expl.getEntailmentExplanation(owlDataFactory
                        .getOWLSubObjectPropertyOfAxiom(incohProperty, owlDataFactory.getOWLBottomObjectProperty()));
            }
            catch (Throwable t) {
//                sb.append("ExplanationsUnsatConceptsPellet: Unable to get explanation for ").append(incohProperty)
//                  .append(": ").append(t
//                        .getMessage()).append("\n");
                errorCount++;
            }
        }

        sb.append("ExplanationsAllPellet: Total reasoning time: ")
          .append(toSeconds(System.currentTimeMillis() - startTime)).append("\n");
        sb.append("ExplanationsAllPellet: Got all incoherent classes at ").append(System.currentTimeMillis())
          .append("\n");
        sb.append("ExplanationsAllPellet: Number of incoherent classes is ")
          .append(trexIncoherentClasses.size()).append("\n");
        sb.append("ExplanationsAllPellet: Errors getting explanations ").append(errorCount).append("\n");

        sb.append("\n");
        resStream.write(sb.toString().getBytes(defaultCharset()));
    }

    public static int toSeconds(long nanoSec) {
        return (int) (nanoSec / (1000));
    }
}
