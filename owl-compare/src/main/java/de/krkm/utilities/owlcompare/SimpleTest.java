package de.krkm.utilities.owlcompare;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import de.krkm.patterndebug.reasoner.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.BufferingMode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class SimpleTest {
    public static void main(String[] args) throws FileNotFoundException, OWLOntologyCreationException {
        OWLOntologyManager manager1 = OWLManager.createOWLOntologyManager();
        OWLOntology baseOntology = manager1.loadOntologyFromOntologyDocument(
                new FileInputStream("/home/daniel/temp/ontologies/uma-random-0.05-arctan.owl"));
        Reasoner reasoner = new Reasoner(baseOntology);
        OWLDataFactory dataFactory = manager1.getOWLDataFactory();
        OWLAxiom ax = dataFactory
                .getOWLDisjointClassesAxiom(dataFactory.getOWLClass(
                        IRI.create("http://dbpedia.org/ontology/BadmintonPlayer")),
                        dataFactory.getOWLClass(
                                IRI.create("http://dbpedia.org/ontology/SpeedwayLeague")
                        ));
        System.out.println(reasoner.isEntailed(ax));
        System.out.println(reasoner.getExplanation(ax));

        PelletReasoner r = new PelletReasoner(baseOntology, BufferingMode.BUFFERING);
        System.out.println(r.isEntailed(ax));
    }
}
