package de.krkm.utilities.annotatedaxiomextractor;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Entry point for standalone execution of the extractor
 */
public class Main {
    public static void main(String[] args) throws OWLOntologyCreationException {
        if (args.length != 1) {
            System.out.println("Usage: <ontologyfile>");
            System.exit(1);
        }
        ArrayList<IRI> iris = new ArrayList<IRI>();
        iris.add(IRI.create("http://ki.informatik.uni-mannheim.de/gold-miner/annotations#confidence"));
        iris.add(IRI.create("http://www.dl-learner.org/enrichment.owl#confidence"));
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        AnnotatedAxiomExtractor extractor =
            new AnnotatedAxiomExtractor(AnnotatedAxiomExtractor.getAnnotationsProperties(
                OWLManager.getOWLDataFactory(), iris.toArray(new IRI[iris.size()])));

        OWLOntology ontology = manager.loadOntology(IRI.create(new File(args[0])));

        PriorityQueue<AxiomConfidencePair> pairs = extractor.extract(ontology);

        while (!pairs.isEmpty()) {
            System.out.println(pairs.remove());
        }
    }
}
