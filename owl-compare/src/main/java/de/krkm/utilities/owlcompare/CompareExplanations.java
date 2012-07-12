package de.krkm.utilities.owlcompare;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import de.krkm.patterndebug.booleanexpressions.OrExpression;
import de.krkm.patterndebug.reasoner.Reasoner;
import de.krkm.utilities.collectiontostring.CollectionToStringWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.Node;

import java.io.File;
import java.util.Set;

public class CompareExplanations {
    public static void main(String[] args) throws OWLOntologyCreationException {
        File ontologyDirectory = new File(args[0]);

        File[] files = ontologyDirectory.listFiles();

        if (files == null) {
            throw new RuntimeException("No ontologies found");
        }
        for (File ontologyFile : files) {
            if (!ontologyFile.getName().endsWith("owl")) {
                continue;
            }
            compareExplanations(ontologyFile.getAbsolutePath());
        }
    }

    private static void compareExplanations(String ontologyFile) throws OWLOntologyCreationException {
        System.out.println("Comparing ontology: " + ontologyFile);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        OWLOntology cleanedOntology = TimedOWLCompare
                .getCleanedOntology(ontology);
        OWLDataFactory df = manager.getOWLDataFactory();
        PelletExplanation.setup();
        Reasoner pattern = new Reasoner(cleanedOntology);
        PelletReasoner full = new PelletReasoner(ontology, BufferingMode.BUFFERING);
        PelletExplanation fullExpl = new PelletExplanation(full);
        Node<OWLClass> fullUnsat = full.getUnsatisfiableClasses();
        System.out.println("Full ontology unsat classes: " + fullUnsat.getSize());

        for (OWLClass c : fullUnsat) {
            System.out.println(c);
            System.out.println(new CollectionToStringWrapper(fullExpl.getUnsatisfiableExplanation(c)));
        }

        PelletReasoner pellet = new PelletReasoner(cleanedOntology, BufferingMode.BUFFERING);
        PelletExplanation explgen = new PelletExplanation(pellet);
        Set<OWLClass> incoherentClasses = pattern.getIncoherentClasses();

        System.out.println("Pattern incoherent: " + incoherentClasses.size());
        System.out.println("Pellet is ");
        System.out.println("Pellet incoherent: " + pellet.getUnsatisfiableClasses().getSize());

        for (OWLClass c : incoherentClasses) {
            System.out.println("Incoherent class: " + c.toString());
            OWLDisjointClassesAxiom axiom = df.getOWLDisjointClassesAxiom(c, c);
            OrExpression patternExplanation = pattern.getExplanation(axiom);
            Set<Set<OWLAxiom>> pelletExplanation = explgen.getEntailmentExplanations(axiom);
            System.out.println(
                    "Pattern Explanation:\n" + new CollectionToStringWrapper(patternExplanation.getDisjunction()));
            System.out.println("Pellet Explanation:\n" + new CollectionToStringWrapper(pelletExplanation));
            System.out.println("Is different: " + patternExplanation.getDisjunction().equals(pelletExplanation));
        }
    }
}
