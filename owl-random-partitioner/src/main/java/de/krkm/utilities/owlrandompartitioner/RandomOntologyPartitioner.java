package de.krkm.utilities.owlrandompartitioner;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Provides methods to randomly partition an ontology into a given number of sub-ontologies
 */
public class RandomOntologyPartitioner {
    private final static Logger logger = LoggerFactory.getLogger(RandomOntologyPartitioner.class);

    /**
     * stores the ontology which should be added to all ontology partitions
     */
    private OWLOntology baseOntology;

    /**
     * stores the ontology which should be partitioned
     */
    private OWLOntology originalOntology;

    private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    /**
     * Initializes the partitioner to partition the ontology <code>originalOntology</code> and add
     * <code>baseOntology</code> to each generated partition.
     *
     * @param originalOntology ontology to partition
     * @param baseOntology     ontology to add to all partitions
     */
    public RandomOntologyPartitioner(OWLOntology originalOntology, OWLOntology baseOntology) {
        this.baseOntology = baseOntology;
        this.originalOntology = originalOntology;
    }

    /**
     * Initializes the partitioner to partition the ontology <code>originalOntology</code>.
     *
     * @param originalOntology ontology to partition
     */
    public RandomOntologyPartitioner(OWLOntology originalOntology) {
        this.originalOntology = originalOntology;
    }

    /**
     * Creates <code>num</code> partitions of <code>originalOntology</code>. These ontologies are axiom-wise disjoint
     * except for axioms contained in a possibly given <code>baseOntology</code>. Partitioning is done by using a
     *
     * @param num number of partitions to create
     * @return created partitions in no particular order
     */
    public Set<OWLOntology> partition(int num) throws OntologyPartitioningException {
        logger.info("Start partitioning to get {} partitions", num);
        OWLOntology[] partitions = new OWLOntology[num];

        // create empty ontologies
        for (int i = 0; i < partitions.length; i++) {
            try {
                partitions[i] = manager.createOntology();
            }
            catch (OWLOntologyCreationException e) {
                throw new OntologyPartitioningException(e);
            }
        }

        logger.info("Done creating empty ontologies");

        // add baseOntology if required
        if (baseOntology != null) {
            for (OWLOntology partition : partitions) {
                manager.addAxioms(partition, baseOntology.getAxioms());
            }
            logger.info("Done adding baseOntology");
        }

        logger.info("Start actual partitioning");
        Random rand = new Random();

        for (OWLAxiom ax : originalOntology.getAxioms()) {
            // do not try to add axioms from baseOntology again
            if (baseOntology != null && baseOntology.containsAxiom(ax)) {
                continue;
            }

            int chosenPartition = rand.nextInt(num);

            logger.debug("Add axiom '{}' to partition {}", ax, chosenPartition);

            manager.addAxiom(partitions[chosenPartition], ax);

        }
        logger.info("Done partitioning");

        return new HashSet<OWLOntology>(Arrays.asList(partitions));
    }
}
