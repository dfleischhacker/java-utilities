package de.krkm.utilities.owlcompare.experiments;

import de.krkm.trex.reasoner.TRexReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * Runs the experiments on all ontologies of the cleaned ontology directory
 */
public class ExperimentsRunner {
    private File ontologyDirectory;
    private File resultsDirectory;

    public ExperimentsRunner(File ontologyDirectory, File resultsDirectory) {
        this.resultsDirectory = resultsDirectory;
        this.ontologyDirectory = ontologyDirectory;
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

        compareUnsatConcepts(resStream, ontology);
        compareUnsatProperties(resStream, ontology);

        resStream.close();
    }

    public static void main(String[] args) throws IOException, OWLOntologyCreationException {
        if (args.length != 2) {
            System.out.println("Please provide: <ontology dir> <results dir>");
            return;
        }
        File ontologyDirectory = new File(args[0]);
        File resultsDirectory = new File(args[1]);

        if (!resultsDirectory.exists()) {
            resultsDirectory.mkdirs();
        }

        ExperimentsRunner runner = new ExperimentsRunner(ontologyDirectory, resultsDirectory);
        runner.runAllExperiments();
    }

    public void compareUnsatConcepts(FileOutputStream resStream, OWLOntology ontology) throws IOException {
        StringBuilder sb = new StringBuilder();

        long startTime = System.nanoTime();
        sb.append("UnsatConcepts: Started at ").append(startTime).append("\n");
        TRexReasoner trex = new TRexReasoner(ontology);
        sb.append("UnsatConcepts: TRex Init done at ").append(System.nanoTime()).append("\n");

        Set<OWLClass> trexIncoherentClasses = trex.getIncoherentClasses();
        sb.append("UnsatConcepts: Total reasoning time: ").append(toSeconds(System.nanoTime() - startTime)).append("\n");
        sb.append("UnsatConcepts: Got all incoherent classes at ").append(System.nanoTime()).append("\n");
        sb.append("UnsatConcepts: Number of incoherent classes is ").append(trexIncoherentClasses.size()).append("\n");

        sb.append("\n");
        resStream.write(sb.toString().getBytes(Charset.defaultCharset()));
    }

    public void compareUnsatProperties(FileOutputStream resStream, OWLOntology ontology) throws IOException {
        StringBuilder sb = new StringBuilder();

        long startTime = System.nanoTime();
        sb.append("UnsatProperties: Started at ").append(startTime).append("\n");
        TRexReasoner trex = new TRexReasoner(ontology);
        sb.append("UnsatProperties: TRex Init done at ").append(System.nanoTime()).append("\n");

        Set<OWLObjectProperty> trexIncoherentClasses = trex.getIncoherentProperties();
        sb.append("UnsatProperties: Total reasoning time: ").append(toSeconds(System.nanoTime() - startTime)).append("\n");
        sb.append("UnsatProperties: Got all incoherent properties at ").append(System.nanoTime()).append("\n");
        sb.append("UnsatProperties: Number of incoherent properties is ").append(trexIncoherentClasses.size()).append("\n");
        sb.append("UnsatProperties: ").append(trexIncoherentClasses.size()).append("\n");

        sb.append("\n");
        resStream.write(sb.toString().getBytes(Charset.defaultCharset()));
    }

    public static int toSeconds(long nanoSec) {
        return (int) (nanoSec / (1000 * 1000 * 1000));
    }
}
