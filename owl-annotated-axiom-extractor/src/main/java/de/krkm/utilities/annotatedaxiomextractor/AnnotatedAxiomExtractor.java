package de.krkm.utilities.annotatedaxiomextractor;

import de.krkm.utilities.collectiontostring.CollectionToStringWrapper;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Provides methods to extract all axioms annotated by a given annotation from an OWL ontology
 */
public class AnnotatedAxiomExtractor {
    private final static Logger log = LoggerFactory.getLogger(AnnotatedAxiomExtractor.class);

    private ArrayList<OWLAnnotationProperty> annotationIRIs;

    /**
     * Initializes an extractor using the given IRIs to extract a confidence value from. The IRIs are checked in the
     * order they are given in the array. The first annotation with an IRI contained in <code>annotationIRIs</code>.
     *
     * @param annotationIRIs IRIs to check for confidence annotations
     */
    public AnnotatedAxiomExtractor(ArrayList<OWLAnnotationProperty> annotationIRIs) {
        log.info("Initializing the extractor with IRIs '{}'", new CollectionToStringWrapper(annotationIRIs));
        this.annotationIRIs = annotationIRIs;
    }

    /**
     * Extracts all axioms which have one of the defined annotations containing confidence values.
     *
     * @param ontology ontology to extract axioms from
     * @return queue containing all extracted axioms ordered by descending confidence
     */
    public PriorityQueue<AxiomConfidencePair> extract(OWLOntology ontology) {
        PriorityQueue<AxiomConfidencePair> queue =
            new PriorityQueue<AxiomConfidencePair>(100, new Comparator<AxiomConfidencePair>() {
                public int compare(AxiomConfidencePair axiomConfidencePair, AxiomConfidencePair axiomConfidencePair1) {
                    return axiomConfidencePair.compareTo(axiomConfidencePair1);
                }
            });

        for (OWLAxiom ax : ontology.getAxioms()) {
            log.debug("Reviewing axiom '{}'", ax);
            for (OWLAnnotationProperty annotationProperty : annotationIRIs) {
                Set<OWLAnnotation> annotations = ax.getAnnotations(annotationProperty);
                log.debug("Got annotations: '{}'", new CollectionToStringWrapper(annotations));
                if (!annotations.isEmpty()) {
                    OWLAnnotation annotation = annotations.iterator().next();
                    Double confidence = Double.parseDouble(annotation.getValue().toString().split("\"")[1]);

                    queue.add(new AxiomConfidencePair(ax.getAxiomWithoutAnnotations(), confidence));
                    log.debug("Added axiom '{}'", ax.getAxiomWithoutAnnotations());
                    break;
                }
            }
        }

        return queue;
    }

    /**
     * Takes a collection of IRIs and encapsulates them in an OWLAnnotationProperty instance
     *
     * @param df             datafactory to use for creating annotation properties
     * @param annotationIRIs IRIs to encapsulate
     * @return list of the given annotationIRIs encapsulated in OWLAnnotationProperty instances
     */
    public static ArrayList<OWLAnnotationProperty> getAnnotationsProperties(OWLDataFactory df, IRI[] annotationIRIs) {
        ArrayList<OWLAnnotationProperty> properties = new ArrayList<OWLAnnotationProperty>();
        for (IRI iri : annotationIRIs) {
            properties.add(df.getOWLAnnotationProperty(iri));
        }

        return properties;
    }
}
