package de.krkm.utilities.owlconfidencehistogram;

import de.krkm.utilities.annotatedaxiomextractor.AnnotatedAxiomExtractor;
import de.krkm.utilities.annotatedaxiomextractor.AxiomConfidencePair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.InputStream;
import java.util.*;

/**
 * Provides methods to extract the confidence values from annotated ontologies.
 */
public class ConfidenceValueExtractor {
    private HashMap<AxiomType<?>, List<Double>> confidenceLists;
    private List<Double> aggregatedConfidenceList;

    /**
     * Initialize the internal list of annotated axioms contained in the ontology given by <code>in</code>. Only axioms
     * having one of the annotation properties contained in <code>properties</code> are considered. The confidence 
     * value
     * is taken from the first property in this list which exists for the specific axiom.
     *
     * @param in         stream containing the ontology to read
     * @param properties annotation properties which contain confidence values
     * @throws OWLOntologyCreationException error loading the ontology
     */
    public ConfidenceValueExtractor(InputStream in, ArrayList<OWLAnnotationProperty> properties)
            throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(in);
        AnnotatedAxiomExtractor aae = new AnnotatedAxiomExtractor(properties);

        confidenceLists = new HashMap<AxiomType<?>, List<Double>>();
        aggregatedConfidenceList = new LinkedList<Double>();

        PriorityQueue<AxiomConfidencePair> axioms = aae.extract(ontology);

        // we just ignore the order in the priority queue as we do not need it
        for (AxiomConfidencePair pair : axioms) {
            OWLAxiom axiom = pair.getAxiom();
            Double conf = pair.getConfidence();

            AxiomType<?> axiomType = axiom.getAxiomType();
            if (!confidenceLists.containsKey(axiomType)) {
                confidenceLists.put(axiomType, new LinkedList<Double>());
            }

            confidenceLists.get(axiomType).add(conf);
            aggregatedConfidenceList.add(conf);
        }
    }

    /**
     * Returns an immutable list of all confidence values contained in the ontology
     *
     * @return immutable list of all confidence values contained in the ontology
     */
    public List<Double> getConfidenceValues() {
        return Collections.unmodifiableList(aggregatedConfidenceList);
    }

    /**
     * Returns an immutable list of all confidence values for the given OWLAPI AxiomType <code>type</code>.
     *
     * @param type axiom type to return values for
     * @return immutable list of confidence values for given axiom type
     */
    public List<Double> getConfidenceValue(AxiomType<?> type) {
        if (!confidenceLists.containsKey(type)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(confidenceLists.get(type));
    }

    /**
     * Return all axiom types which are contained in the ontology and annotated with confidence values.
     *
     * @return immutable set of all axiom types used in the ontology in conjunction with confidence values 
     */
    public Set<AxiomType<?>> getUsedAxiomTypes() {
        return Collections.unmodifiableSet(confidenceLists.keySet());
    }
    
}
