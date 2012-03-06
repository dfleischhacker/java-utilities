package de.krkm.utilities.owlsubpropertycycleremover.cycletype;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

/**
 * Cycle type for detecting cycles of subPropertyOf axioms between object properties
 */
public class SubPropertyOfCycleType implements CycleType<OWLSubObjectPropertyOfAxiom> {
    @Override
    public AxiomType<OWLSubObjectPropertyOfAxiom> getEdgeType() {
        return AxiomType.SUB_OBJECT_PROPERTY;
    }

    @Override
    public String getSubject(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSubProperty().asOWLObjectProperty().getIRI().toString();
    }

    @Override
    public String getObject(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSuperProperty().asOWLObjectProperty().getIRI().toString();
    }
}
