package de.krkm.utilities.ontologyminimizer;

import de.krkm.utilities.annotatedaxiomextractor.AnnotatedAxiomExtractor;
import de.krkm.utilities.annotatedaxiomextractor.AxiomConfidencePair;
import de.krkm.utilities.collectiontostring.CollectionToStringWrapper;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Provides method to determine an ontology's core.
 */
public class OntologyMinimizer {
    private final static Logger log = LoggerFactory.getLogger(OntologyMinimizer.class);

    private File snapShotDir;

    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    private int snapShotCounter = 0;
    private int removedAxioms = 0;
    private int readdedAxioms = 0;
    private int axiomsNotInGenerated = 0;
    private final PriorityQueue<AxiomConfidencePair> pairs;

    /**
     * Extracts the relevant annotations from the given <code>annotatedOntology</code> using the pre-defined set of
     * annotation properties.
     *
     * @param annotatedOntology ontology to extract annotations from
     */
    public OntologyMinimizer(OWLOntology annotatedOntology) {
        this(annotatedOntology,
             new String[]{"http://ki.informatik.uni-mannheim.de/gold-miner/annotations#confidence",
                     "http://www.dl-learner.org/enrichment.owl#confidence"});
    }

    /**
     * Extracts the relevant annotations from the given <code>annotatedOntology</code> using the given annotations
     * properties.
     *
     * @param annotatedOntology    ontology to extract annotations from
     * @param annotationProperties property IRIs containing confidence values ordered by priority
     */
    public OntologyMinimizer(OWLOntology annotatedOntology, String[] annotationProperties) {
        IRI[] iriList = new IRI[annotationProperties.length];
        for (int i = 0; i < annotationProperties.length; i++) {
            String iri = annotationProperties[i];
            iriList[i] = IRI.create(iri);
        }
        AnnotatedAxiomExtractor extractor = new AnnotatedAxiomExtractor(
                new ArrayList<OWLAnnotationProperty>(
                        AnnotatedAxiomExtractor.getAnnotationsProperties(manager.getOWLDataFactory(), iriList)));
        extractor.setPreserveAnnotations(true);
        pairs = extractor.extract(annotatedOntology);
        log.info("Extracted {} pairs", pairs.size());
    }

    /**
     * Sets the directory to write snapshots of the generated ontology to
     *
     * @param snapshotDir directory to write snapshots of generated ontology to
     */
    public void setSnapShotDir(File snapshotDir) {
        this.snapShotDir = snapshotDir;
    }

    /**
     * Starts the minimization process for the given <code>ontology</code> and returns the resulting ontology.
     *
     * @param ontology       ontology to minimize
     * @param snapshotPrefix prefix for writing snapshots
     * @return minimized ontology
     */
    public OWLOntology startMinimization(OWLOntology ontology, String snapshotPrefix)
            throws OWLOntologyCreationException {
        return startMinimization(ontology, snapshotPrefix, null);
    }

    /**
     * Starts the minimization process for the given <code>ontology</code> and returns the resulting ontology.
     *
     * @param ontology ontology to minimize
     * @return minimized ontology
     */
    public OWLOntology startMinimization(OWLOntology ontology)
            throws OWLOntologyCreationException {
        return startMinimization(ontology, "generated", null);
    }

    /**
     * Starts the minimization process for the given <code>ontology</code> and returns the resulting ontology.
     *
     * @param ontology            ontology to minimize
     * @param removedAxiomsStream stream to write removed axioms to or null to not log such axioms
     * @return minimized ontology
     */
    public OWLOntology startMinimization(OWLOntology ontology, OutputStream removedAxiomsStream)
            throws OWLOntologyCreationException {
        return startMinimization(ontology, "generated", removedAxiomsStream);
    }

    /**
     * Starts the minimization process for the given <code>ontology</code> and returns the resulting ontology.
     *
     * @param ontology            ontology to minimize
     * @param removedAxiomsStream stream to write removed axioms to or null to not log such axioms
     * @param snapshotPrefix      prefix for writing snapshots
     * @return minimized ontology
     */
    public OWLOntology startMinimization(OWLOntology ontology, String snapshotPrefix, OutputStream removedAxiomsStream)
            throws OWLOntologyCreationException {
        BufferedWriter removedAxiomsWriter = null;
        if (removedAxiomsStream != null) {
            removedAxiomsWriter = new BufferedWriter(new OutputStreamWriter(removedAxiomsStream));
        }
        log.info("Starting minimization...");

        OWLOntology generatedOntology;
        synchronized (manager) {
            generatedOntology = manager.createOntology(ontology.getAxioms());
        }
        Configuration config = new Configuration();
        config.ignoreUnsupportedDatatypes = true;
        OWLReasoner reasoner = new Reasoner(config, generatedOntology);
        log.debug("Reasoner initialized");
        int counter = 0;
        PriorityQueue<AxiomConfidencePair> internalPairs;
        synchronized (pairs) {
            internalPairs = new PriorityQueue<AxiomConfidencePair>(pairs);
        }
        while (!internalPairs.isEmpty()) {
            AxiomConfidencePair pair = internalPairs.remove();
            log.debug("Progress: {} (Removed {} - Readded {} - Not In {})",
                      new Object[]{counter, removedAxioms, readdedAxioms, axiomsNotInGenerated});
            log.debug("Trying to remove axiom '{}' having confidence of {}", pair.getAxiom(), pair.getConfidence());
            counter++;
            if (!generatedOntology.containsAxiomIgnoreAnnotations(pair.getAxiom())) {
                axiomsNotInGenerated++;
                continue;
            }
            try {
                List<OWLOntologyChange> changes = manager.removeAxiom(generatedOntology, pair.getAxiom());
                log.debug("Changes that took place: {}", new CollectionToStringWrapper(changes));
                reasoner.flush();
                if (!reasoner.isEntailed(pair.getAxiom().getAxiomWithoutAnnotations())) {
                    log.debug("Axiom '{}' is not entailed by ontology, add it again", pair.getAxiom());
                    readdedAxioms++;
                    manager.addAxiom(generatedOntology, pair.getAxiom());
                } else {
                    log.debug("Axiom '{}' is still entailed", pair.getAxiom());
                    removedAxioms++;
                    logRemovedAxiom(removedAxiomsWriter, pair);
                    if (counter % 1000 == 0) {
                        log.debug("Progress: {} (Removed {} - Readded {} - Not In {})",
                                  new Object[]{counter, removedAxioms, readdedAxioms, axiomsNotInGenerated});
                        try {
                            createSnapShot(generatedOntology, snapshotPrefix);
                        }
                        catch (OntologyMinimizationException e) {
                            log.error("Unable to create snapshot", e);
                        }
                    }
                }
            }
            catch (OWLOntologyChangeException e) {
                log.error("Unable to remove axiom '{}'", pair.getAxiom(), e);
            }
        }
        log.info("Minimization done...");
        log.info("** Total axioms in result: {}", generatedOntology.getAxiomCount());
        log.info("** Axioms removed: {}", removedAxioms);
        log.info("** Axioms re-added: {}", readdedAxioms);

        if (removedAxiomsWriter != null) {
            try {
                removedAxiomsWriter.close();
            }
            catch (IOException e) {
                log.error("Unable to close writer for removed axioms", e);
            }
        }

        return generatedOntology;
    }

    /**
     * Creates a snapshot of the current generated ontology. If snapShotDir is not set, this is a no-op.
     *
     * @param ontology       ontology to save snapshot of
     * @param snapshotPrefix prefix used for snapshot file
     * @throws OntologyMinimizationException when unable to write snapshot
     */
    private synchronized void createSnapShot(OWLOntology ontology, String snapshotPrefix)
            throws OntologyMinimizationException {
        if (snapShotDir == null) {
            log.debug("Not creating snapshot since disabled");
            return;
        }

        String snapShotFileName =
                snapShotDir
                        .getAbsolutePath() + File.separator + snapshotPrefix + "_" + snapShotCounter + "" +
                        ".owl";

        /*
         * sure this is a race condition when other applications interfere with this one, but this solution is
         * sufficient
         */
        while (new File(snapShotFileName).exists()) {
            snapShotCounter++;
            snapShotFileName = snapShotDir
                    .getAbsolutePath() + File.separator + snapshotPrefix + "_" + snapShotCounter + "" +
                    ".owl";
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
            manager.saveOntology(ontology, out);
        }
        catch (OWLOntologyStorageException e) {
            throw new OntologyMinimizationException("Unable to save snapshot", e);
        }
        finally {
            try {
                out.close();
            }
            catch (IOException ignored) {

            }
        }
        snapShotCounter++;
    }

    /**
     * Writes the given axiom into the writer. If the writer is null, this is a no-op.
     *
     * @param removedAxiomWriter writer to log removed axioms
     * @param axiom              axiom to write to removed axiom stream
     */
    private void logRemovedAxiom(BufferedWriter removedAxiomWriter, AxiomConfidencePair axiom) {
        if (removedAxiomWriter == null) {
            return;
        }
        try {
            removedAxiomWriter.write(axiom.getAxiom().toString());
            removedAxiomWriter.newLine();
            removedAxiomWriter.flush();
        }
        catch (IOException e) {
            log.error("Unable to write removed axioms to log", e);
        }
    }
}
