package de.krkm.utilities.owlcompare.experiments;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;

import java.io.File;
import java.io.FileOutputStream;

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

    public void runAllExperiments() {
        for (File ontologyFile : ontologyDirectory.listFiles()) {
            runExperimentsOnOntology(ontologyFile);
        }
    }

    public void runExperimentsOnOntology(File ontologyFile) {

    }

    public static void main(String[] args) {
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

    public void compareUnsatConcepts(FileOutputStream resStream, OWLOntology ontology) {
 /*       TRexReasoner trex = new TRexReasoner(ontology);

        Set<OWLClass> trexIncoherentClasses = trex.getIncoherentClasses();*/



    }

    public void compareUnsatProperties(FileOutputStream resStream, OWLOntology ontology) {
   /*     TRexReasoner trex = new TRexReasoner(ontology);

        Set<OWLClass>*/
        PelletReasoner pellet = new PelletReasoner(ontology, BufferingMode.BUFFERING);
    }
}
