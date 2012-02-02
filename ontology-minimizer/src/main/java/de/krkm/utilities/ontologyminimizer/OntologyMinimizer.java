package de.krkm.utilities.ontologyminimizer;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import de.krkm.utilities.annotatedaxiomextractor.AnnotatedAxiomExtractor;
import de.krkm.utilities.annotatedaxiomextractor.AxiomConfidencePair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Provides method to determine an ontology's core.
 */
public class OntologyMinimizer {
    private final static Logger log = LoggerFactory.getLogger(OntologyMinimizer.class);

    private File snapShotDir;

    private OWLOntologyManager manager;
    private OWLOntology generatedOntology;

    private int snapShotCounter = 0;
    private PriorityQueue<AxiomConfidencePair> pairs;
    private OutputStream outputStream;


    /**
     * Initializes the minimizer using the given input and output streams.
     *
     * @param coherentStream  inputstream for getting the coherent ontology
     * @param annotatedStream inputstream for getting the ontology containing axioms annotated with their confidence
     *                        values
     * @param outputStream    stream to write generated ontology to
     * @param annotationIRIs  array list of all IRIs of confidence annotations
     * @param snapShotDir     directory to regularly dump intermediate versions of the generated ontology. if null, no
     *                        snapshots are created
     * @throws OntologyMinimizationException on an error initializing the minimizer
     */
    public OntologyMinimizer(InputStream coherentStream,
                             InputStream annotatedStream,
                             OutputStream outputStream,
                             ArrayList<IRI> annotationIRIs,
                             File snapShotDir) throws OntologyMinimizationException {
        this.outputStream = outputStream;
        this.manager = OWLManager.createOWLOntologyManager();
        ArrayList<OWLAnnotationProperty> annotationProperties =
            AnnotatedAxiomExtractor.getAnnotationsProperties(manager.getOWLDataFactory(),
                                                             annotationIRIs.toArray(
                                                                 new IRI[annotationIRIs
                                                                     .size()]));

        this.snapShotDir = snapShotDir;

        OWLOntology annotatedOntology;
        try {
            annotatedOntology = manager.loadOntologyFromOntologyDocument(annotatedStream);
        }
        catch (OWLOntologyCreationException e) {
            throw new OntologyMinimizationException("Unable to load annotated ontology", e);
        }

        AnnotatedAxiomExtractor extractor = new AnnotatedAxiomExtractor(annotationProperties);
        pairs = extractor.extract(annotatedOntology);
        log.info("Extracted {} pairs", pairs.size());

        manager.getOWLDataFactory().purge();
        manager = OWLManager.createOWLOntologyManager();

        try {
            generatedOntology = manager.loadOntologyFromOntologyDocument(coherentStream);
        }
        catch (OWLOntologyCreationException e) {
            throw new OntologyMinimizationException("Unable to load original ontology", e);
        }
        log.info("Original ontology contains {} axioms", generatedOntology.getAxiomCount());
    }

    /**
     * Starts the minimization process
     */
    public void startMinimization() {
        log.info("Starting minimization...");
        PelletReasoner reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(generatedOntology);
        manager.addOntologyChangeListener(reasoner);
        log.debug("Reasoner initialized");
        int counter = 0;
        for (AxiomConfidencePair pair : pairs) {
            log.debug("Progress: {}", counter);
            log.debug("Trying to remove axiom '{}' having confidence of {}", pair.getAxiom(), pair.getConfidence());
            counter++;
            if (!generatedOntology.containsAxiom(pair.getAxiom().getAxiomWithoutAnnotations())) {
                continue;
            }
            try {
                manager.removeAxiom(generatedOntology, pair.getAxiom());
                if (!reasoner.isEntailed(pair.getAxiom())) {
                    log.debug("Axiom '{}' is not entailed by ontology, add it again", pair.getAxiom());
                    manager.addAxiom(generatedOntology, pair.getAxiom());
                }
                else if (counter % 1000 == 0) {
                    log.info("Reached axiom {}, trying to write snapshot", counter);
                    try {
                        createSnapShot();
                    }
                    catch (OntologyMinimizationException e) {
                        log.error("Unable to create snapshot", e);
                    }
                }
            }
            catch (OWLOntologyChangeException e) {
                log.error("Unable to remove axiom '{}'", pair.getAxiom(), e);
            }
        }
        log.info("Minimizaion done...");
    }

    /**
     * Creates a snapshot of the current generated ontology. If snapShotDir is not set, this is a no-op.
     *
     * @throws OntologyMinimizationException when unable to write snapshot
     */
    private void createSnapShot() throws OntologyMinimizationException {
        if (snapShotDir == null) {
            log.debug("Not creating snapshot since disabled");
            return;
        }

        String snapShotFileName =
            snapShotDir.getAbsolutePath() + File.separator + "generated_" + snapShotCounter + ".owl";

        /*
         * sure this is a race condition when other applications interfere with this one, but this solution is
         * sufficient
         */
        while (new File(snapShotFileName).exists()) {
            snapShotCounter++;
            snapShotFileName = snapShotDir.getAbsolutePath() + File.separator + "generated_" + snapShotCounter + ".owl";
        }

        log.info("Writing snapshot to '{}'", snapShotFileName);
        FileOutputStream out;
        try {
            out = new FileOutputStream(
                snapShotFileName);
        }
        catch (FileNotFoundException e) {
            throw new OntologyMinimizationException("Unable to open file for creating snapshot", e);
        }
        try {
            manager.saveOntology(generatedOntology, out);
        }
        catch (OWLOntologyStorageException e) {
            throw new OntologyMinimizationException("Unable to save snapshot", e);
        }
        snapShotCounter++;
    }

    /**
     * Writes the generated ontology into the outputstream provided at initialization time
     *
     * @throws OWLOntologyStorageException on an error saving the generated ontology
     */
    public void saveGeneratedOntology() throws OWLOntologyStorageException {
        manager.saveOntology(generatedOntology, outputStream);
    }
}
