package de.krkm.utilities.owlsubpropertycycleremover.cycletype;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * Interface for implementing a specific type of cycle for cycle detection
 */
public interface CycleType<T extends OWLAxiom> {
    /**
     * Returns the AxiomType which should be used as edges in the graph.
     *
     * @return axiom type for edges
     */
    public AxiomType<T> getEdgeType();

    /**
     * Returns the string to be used as a node identifier for the subject in the graph. The given axiom is guaranteed
     * to be of the axiom type returned by
     * {@link de.krkm.utilities.owlsubpropertycycleremover.cycletype.CycleType#getEdgeType()}.
     *
     *
     * @param axiom axiom for whose subject the identifier should be returned
     * @return identifier for subject
     */
    public String getSubject(T axiom);

    /**
     * Returns the string to be used as a node identifier for the object in the graph. The given axiom is guaranteed to
     * be of the axiom type returned by
     *
     * @param axiom axiom for whose object the identifier should be returned
     * @return identifier for object
     */
    public String getObject(T axiom);
}
