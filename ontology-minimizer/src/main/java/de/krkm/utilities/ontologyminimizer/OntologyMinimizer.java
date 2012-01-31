package de.krkm.utilities.ontologyminimizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Provides method to determine an ontology's core.
 */
public class OntologyMinimizer {
    private final static Logger log = LoggerFactory.getLogger(OntologyMinimizer.class);

    private InputStream coherentStream;
    private InputStream annotatedStream;
    private OutputStream outputStream;
    private File snapShotDir;

    private OWLOntologyManager manager;
    private OWLOntology generatedOntology;

    private int snapShotCounter = 0;

    /**
     * Initializes the minimizer using the given input and output streams.
     *
     * @param coherentStream  inputstream for getting the coherent ontology
     * @param annotatedStream inputstream for getting the ontology containing axioms annotated with their confidence
     *                        values
     * @param outputStream    stream to write generated ontology to
     * @param snapShotDir     directory to regularly dump intermediate versions of the generated ontology
     * @throws OntologyMinimizationException on an error initializing the minimizer
     */
    public OntologyMinimizer(InputStream coherentStream,
                             InputStream annotatedStream,
                             OutputStream outputStream,
                             File snapShotDir) throws OntologyMinimizationException {
        this.coherentStream = coherentStream;
        this.annotatedStream = annotatedStream;
        this.outputStream = outputStream;
        this.snapShotDir = snapShotDir;

        this.manager = OWLManager.createOWLOntologyManager();
        try {
            generatedOntology = manager.loadOntologyFromOntologyDocument(coherentStream);
        }
        catch (OWLOntologyCreationException e) {
            throw new OntologyMinimizationException("Unable to load original ontology", e);
        }
    }

    

    /**
     * Creates a snapshot of the current generated ontology. If snapShotDir is not set, this is a no-op.
     *
     * @throws OntologyMinimizationException when unable to write snapshot
     */
    private void createSnapShot() throws OntologyMinimizationException {
        if (snapShotDir == null) {
            log.debug("Not creating snapshot because disabled");
            return;
        }

        String snapShotFileName =
            snapShotDir.getAbsolutePath() + File.separator + "generated_" + snapShotCounter + ".owl";
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
}
