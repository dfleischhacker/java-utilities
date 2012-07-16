package de.krkm.utilities.owlcompare;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import de.krkm.trex.reasoner.Reasoner;
import de.krkm.utilities.collectiontostring.CollectionToStringWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.BufferingMode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;

public class SimpleTest {
    public static void main(String[] args) throws FileNotFoundException, OWLOntologyCreationException {
        OWLOntologyManager manager1 = OWLManager.createOWLOntologyManager();
        OWLOntology baseOntology = manager1.loadOntologyFromOntologyDocument(
                new FileInputStream("/home/daniel/temp/ontologies/uma-random-0.05-arctan.owl_cleaned"));
        Reasoner reasoner = new Reasoner(baseOntology);
        OWLDataFactory dataFactory = manager1.getOWLDataFactory();
        OWLAxiom ax = checkSubClass(dataFactory, reasoner, "http://dbpedia.org/ontology/FloweringPlant",
                "http://dbpedia.org/ontology/Species");
        checkSubClass(dataFactory, reasoner, "http://dbpedia.org/ontology/Plant",
                "http://dbpedia.org/ontology/Species");
        checkSubClass(dataFactory, reasoner, "http://dbpedia.org/ontology/Eukaryote",
                "http://dbpedia.org/ontology/Species");
        checkSubClass(dataFactory, reasoner, "http://dbpedia.org/ontology/Plant",
                "http://dbpedia.org/ontology/Eukaryote");
        checkSubClass(dataFactory, reasoner, "http://dbpedia.org/ontology/FloweringPlant",
                "http://dbpedia.org/ontology/Plant");

        if (reasoner.isEntailed(ax)) {
            for (Set<OWLAxiom> ex : reasoner.getExplanation(ax).getDisjunction()) {
                System.out.println(" - " +new CollectionToStringWrapper(ex));
            }
        }

        PelletExplanation.setup();
        PelletReasoner r = new PelletReasoner(baseOntology, BufferingMode.BUFFERING);
        PelletExplanation expl = new PelletExplanation(r);
        System.out.println(r.isEntailed(ax));

        for (Set<OWLAxiom> ex : expl.getEntailmentExplanations(ax)) {
            System.out.println(" - " +new CollectionToStringWrapper(ex));
        }
    }

    public static OWLAxiom checkSubClass(OWLDataFactory dataFactory, Reasoner reasoner, String iri1, String iri2) {
        OWLAxiom ax = dataFactory
                .getOWLSubClassOfAxiom(dataFactory.getOWLClass(
                        IRI.create(iri1)),
                        dataFactory.getOWLClass(
                                IRI.create(iri2)
                        ));
        boolean entailed = reasoner.isEntailed(ax);
        System.out.println(ax + " : " + entailed);
        return ax;
    }
}
