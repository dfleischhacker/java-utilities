package de.krkm.utilities.owlsubpropertycycleremover.cycletype;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class SubClassOfCycleType implements CycleType<OWLSubClassOfAxiom> {
    @Override
    public AxiomType<OWLSubClassOfAxiom> getEdgeType() {
        return AxiomType.SUBCLASS_OF;
    }

    @Override
    public String getSubject(OWLSubClassOfAxiom axiom) {
        return axiom.getSubClass().asOWLClass().toString();
    }

    @Override
    public String getObject(OWLSubClassOfAxiom axiom) {
        return axiom.getSuperClass().asOWLClass().toString();
    }
}
