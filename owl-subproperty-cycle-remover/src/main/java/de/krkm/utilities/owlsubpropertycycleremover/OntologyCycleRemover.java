package de.krkm.utilities.owlsubpropertycycleremover;

import de.krkm.utilities.collectiontostring.CollectionToStringWrapper;
import de.krkm.utilities.owlsubpropertycycleremover.cycletype.CycleType;
import de.krkm.utilities.owlsubpropertycycleremover.graph.Graph;
import de.krkm.utilities.owlsubpropertycycleremover.graph.Node;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides methods to detect and resolve cycles formed by OWL constructs such as subPropertyOf.
 */
public class OntologyCycleRemover {
    private final static Logger log = LoggerFactory.getLogger(OntologyCycleRemover.class);

    private BufferedWriter removedWriter;
    private OWLOntology ontology;
    private List<OWLAnnotationProperty> annotationProperties;
    private OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
    private OutputStreamWriter streamWriter;

    /**
     * Initializes the OntologyCycleRemover to cleanup the ontology contained in the given inputstream
     * <code>input</code>.
     *
     * IRIs to search for confidence values default to gold-miner and dl-learner IRIs. To change these IRIs use
     * {@link OntologyCycleRemover#setConfidenceURIs(java.util.List)}.
     *
     * @param input stream to read ontology from
     */
    public OntologyCycleRemover(InputStream input) throws OWLOntologyCreationException {
        annotationProperties = new ArrayList<OWLAnnotationProperty>();

        ArrayList<IRI> iris = new ArrayList<IRI>();
        iris.add(IRI.create("http://ki.informatik.uni-mannheim.de/gold-miner/annotations#confidence"));
        iris.add(IRI.create("http://www.dl-learner.org/enrichment.owl#confidence"));

        for (IRI iri : iris) {
            annotationProperties.add(ontologyManager.getOWLDataFactory().getOWLAnnotationProperty(iri));
        }

        System.out.println("Loading ontology");
        ontology = ontologyManager.loadOntologyFromOntologyDocument(input);
        System.out.println("Loaded ontology");
    }

    /**
     * Sets the list of URIs which are used in confidence annotations. The order of these URIs defines their priority,
     * i.e., the first one found for a specific axiom is considered to be the one holding the correct confidence value.
     *
     * @param iris list of URIs used for confidence annotations
     */
    public void setConfidenceURIs(List<IRI> iris) {
        annotationProperties = new ArrayList<OWLAnnotationProperty>();
        for (IRI iri : iris) {
            annotationProperties.add(ontologyManager.getOWLDataFactory().getOWLAnnotationProperty(iri));
        }
    }

    /**
     * Cleans the loaded ontology from cycles created by the given <code>property</code> (edges) between
     * <code>instance</code> (nodes).
     * <p/>
     * For this purpose, only axioms having a confidence annotations (see {@link }
     * <p/>
     * Example: clean("http://www.w3.org/2002/07/owl#ObjectProperty", "http://www.w3.org/2002/07/owl#subPropertyOf")
     * would remove cyclic subproperty definitions between object properties.
     *
     * @param type cycle type to use in graph
     */
    public <T extends OWLAxiom> void clean(CycleType<T> type) throws OWLOntologyStorageException {
        log.info("Starting cleaning process for type {}", type);
        Set<T> axioms = ontology.getAxioms(type.getEdgeType());

        Graph g = new Graph();

        for (T ax : axioms) {
            String subject = type.getSubject(ax);
            if (!subject.startsWith("http://dbpedia.org")) {
                System.out.println("Skipping " + ax);
                continue;
            }
            String object = type.getObject(ax);
            if (!object.startsWith("http://dbpedia.org")) {
                System.out.println("Skipping " + ax);
                continue;
            }

            Double confidenceValue = null;
            for (OWLAnnotationProperty prop : annotationProperties) {
                Set<OWLAnnotation> annotations = ax.getAnnotations(prop);

                if (!annotations.isEmpty()) {
                    OWLAnnotation annotation = annotations.iterator().next();
                    confidenceValue = Double.parseDouble(annotation.getValue().toString().split("\"")[1]);
                    break;
                }
            }

            Node sNode = g.getNode(subject);
            Node oNode = g.getNode(object);

            if (confidenceValue != null) {
                sNode.addOutEdge(oNode, confidenceValue);
            } else {
                sNode.addOutEdge(oNode);
            }
        }

        for (Node curNode : g.getNodes()) {
            List<Node> cyclePath = g.getShortestPath(curNode, curNode);
            while (cyclePath != null) {
                Node startNode = cyclePath.get(0);
                Node minStart = null;
                Node minEnd = null;
                Double minWeight = 9999999d;

                log.info("Found cycle: {}", new CollectionToStringWrapper(cyclePath));

                for (int i = 1; i < cyclePath.size(); i++) {
                    Node nextNode = cyclePath.get(i);
                    Double curWeight = startNode.getWeight(nextNode);
                    if (curWeight != null) {
                        if (curWeight < minWeight) {
                            minStart = startNode;
                            minEnd = nextNode;
                            minWeight = curWeight;
                        }
                    }
                    startNode = nextNode;
                }


                //TODO: adjust to cycle type definition
                Set<OWLSubObjectPropertyOfAxiom> corrAxioms = ontology
                        .getObjectSubPropertyAxiomsForSuperProperty(
                                ontologyManager.getOWLDataFactory()
                                               .getOWLObjectProperty(IRI.create(minStart.getName())));

                for (OWLSubObjectPropertyOfAxiom ax : corrAxioms) {
                    if (ax.getSubProperty().asOWLObjectProperty().getIRI().toString().equals(minEnd.getName())) {
                        ontologyManager.removeAxiom(ontology, ax);
                        logRemoved(ax);
                        break;
                    }
                }

                minStart.removeOutEdge(minEnd);
                cyclePath = g.getShortestPath(curNode, curNode);
            }
        }
    }

    /**
     * Saves the cleaned up ontology into the given stream <code>out</code>.
     *
     * @param out stream to write result to
     * @throws OWLOntologyStorageException
     */
    public void saveOntology(OutputStream out) throws OWLOntologyStorageException {
        ontologyManager.saveOntology(ontology, out);
        try {
            removedWriter.flush();
        }
        catch (IOException ignored) {
        }
    }

    /**
     * Sets the stream for logging removed axioms
     *
     * @param stream for for writing removed axioms to
     */
    public void setRemovedAxiomsStream(OutputStream stream) {
        streamWriter = new OutputStreamWriter(stream);
        this.removedWriter = new BufferedWriter(streamWriter);
    }

    /**
     * Logs the given axiom as removed from the ontology
     *
     * @param axiom axiom to log as removed
     */
    public void logRemoved(OWLAxiom axiom) {
        log.info("Removing axiom: {}", axiom);
        if (removedWriter == null) {
            return;
        }
        try {
            removedWriter.write(axiom.toString());
            removedWriter.newLine();
        }
        catch (IOException e) {
            log.warn("Unable to log removed axiom {}", axiom, e);
        }
    }

    /**
     * Flushes and closes the log output stream
     */
    public void closeLog() throws IOException {
        removedWriter.flush();
        removedWriter.close();
    }
}
