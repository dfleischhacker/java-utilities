package de.krkm.utilities.owlcompare.experiments;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CleanupAllOntologies {
    public final static AxiomType[] TYPES = new AxiomType[]{AxiomType.SUBCLASS_OF, AxiomType.DISJOINT_CLASSES,
            AxiomType.DISJOINT_OBJECT_PROPERTIES, AxiomType.SUB_OBJECT_PROPERTY, AxiomType.OBJECT_PROPERTY_DOMAIN,
            AxiomType.OBJECT_PROPERTY_RANGE};

    public static OWLOntology getCleanedOntology(OWLOntology base) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology cleanedOntology = manager.createOntology();
        Set<AxiomType> typeSet = new HashSet<AxiomType>();
        Collections.addAll(typeSet, TYPES);

        for (OWLAxiom ax : base.getAxioms()) {
            if (typeSet.contains(ax.getAxiomType()) && ax.getSignature().size() > 1 && isAtomic(ax)) {
                manager.addAxiom(cleanedOntology, ax);
            }
        }
        return cleanedOntology;
    }

    /**
     * Checks if the axiom only uses atomic concepts
     *
     * @param axiom
     * @return
     */
    public static boolean isAtomic(OWLAxiom axiom) {
        if (axiom instanceof OWLSubClassOfAxiom) {
            OWLSubClassOfAxiom casted = (OWLSubClassOfAxiom) axiom;
            return !casted.getSubClass().isAnonymous() && !casted.getSuperClass().isAnonymous();
        }

        if (axiom instanceof OWLSubObjectPropertyOfAxiom) {
            OWLSubObjectPropertyOfAxiom casted = (OWLSubObjectPropertyOfAxiom) axiom;
            return !casted.getSubProperty().isAnonymous() && !casted.getSuperProperty().isAnonymous();
        }

        if (axiom instanceof OWLDisjointClassesAxiom) {
            OWLDisjointClassesAxiom casted = (OWLDisjointClassesAxiom) axiom;
            for (OWLClassExpression cls : casted.getClassExpressions()) {
                if (!(cls instanceof OWLClass)) {
                    return false;
                }
            }
            return true;
        }

        if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
            OWLDisjointObjectPropertiesAxiom casted = (OWLDisjointObjectPropertiesAxiom) axiom;
            for (OWLObjectPropertyExpression prop : casted.getProperties()) {
                if (!(prop instanceof OWLObjectProperty)) {
                    return false;
                }
            }
            return true;
        }

        if (axiom instanceof OWLObjectPropertyDomainAxiom) {
            OWLObjectPropertyDomainAxiom casted = (OWLObjectPropertyDomainAxiom) axiom;

            boolean isOP = casted.getProperty() instanceof OWLObjectProperty;
            boolean isC = casted.getDomain() instanceof OWLClass;
            return isOP && isC;
        }

        if (axiom instanceof OWLObjectPropertyRangeAxiom) {
            OWLObjectPropertyRangeAxiom casted = (OWLObjectPropertyRangeAxiom) axiom;

            boolean isOP = casted.getProperty() instanceof OWLObjectProperty;
            boolean isC = casted.getRange() instanceof OWLClass;
            return isOP && isC;
        }

        throw new RuntimeException(axiom.getAxiomType().toString() + " not supported");
    }

    public static void main(String[] args)
            throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
        File cleanDir = new File("/home/daniel/cleaned_ontologies");
        cleanDir.mkdirs();
        File dir = new File("/home/daniel/temp/ontologies");
        for (File ontologyFile : dir.listFiles()) {
            if (!ontologyFile.getAbsolutePath().endsWith(".owl")) {
                continue;
            }
            String cleanName = cleanDir.getAbsolutePath() + File.separator + ontologyFile.getName();
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology unclean = manager.loadOntologyFromOntologyDocument(ontologyFile);
            OWLOntology cleaned = getCleanedOntology(unclean);
            FileOutputStream outputStream = new FileOutputStream(cleanName);
            manager.saveOntology(cleaned, new RDFXMLOntologyFormat(),outputStream);
            outputStream.close();
        }
    }
}
