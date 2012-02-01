package de.krkm.utilities.annotatedaxiomextractor;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;

import static junit.framework.Assert.assertEquals;

public class AnnotatedAxiomExtractorTest {
    @Test
    public void testExtract() throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();
        ArrayList<OWLAnnotationProperty> properties =
            AnnotatedAxiomExtractor.getAnnotationsProperties(df,
                                                             new IRI[]{IRI.create("http://example.com/conf1"),
                                                                       IRI.create("http://example.com/conf2")});
        AnnotatedAxiomExtractor extractor = new AnnotatedAxiomExtractor(properties);

        OWLOntology ontology = manager.createOntology();
        HashSet<OWLAnnotation> annotations = new HashSet<OWLAnnotation>();
        annotations.add(df.getOWLAnnotation(properties.get(0), df.getOWLLiteral(0.988)));
        OWLAxiom ax = df.getOWLSubClassOfAxiom(df.getOWLClass(IRI.create("http://df.de/c1")),
                                 df.getOWLClass(IRI.create("http://df.de/c2")),
                                 annotations);

        manager.addAxiom(ontology, ax);

        annotations = new HashSet<OWLAnnotation>();
        annotations.add(df.getOWLAnnotation(properties.get(1), df.getOWLLiteral(0.8)));
        ax = df.getOWLSubClassOfAxiom(df.getOWLClass(IRI.create("http://df.de/c3")),
                                 df.getOWLClass(IRI.create("http://df.de/c4")),
                                 annotations);

        manager.addAxiom(ontology, ax);

        annotations = new HashSet<OWLAnnotation>();
        annotations.add(df.getOWLAnnotation(properties.get(1), df.getOWLLiteral(0.7)));
        annotations.add(df.getOWLAnnotation(properties.get(0), df.getOWLLiteral(0.1)));
        ax = df.getOWLSubClassOfAxiom(df.getOWLClass(IRI.create("http://df.de/c5")),
                                      df.getOWLClass(IRI.create("http://df.de/c6")),
                                      annotations);

        manager.addAxiom(ontology, ax);

        PriorityQueue<AxiomConfidencePair> q = extractor.extract(ontology);

        LinkedList<Double> reference = new LinkedList<Double>();
        Collections.addAll(reference, 0.988, 0.8, 0.1);
        
        while (!q.isEmpty()) {
            Double ret = q.poll().getConfidence();
            assertEquals(reference.removeFirst(), ret);
        }
    }
}
