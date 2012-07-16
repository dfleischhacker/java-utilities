package de.krkm.utilities.owlcompare;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.sun.javaws.exceptions.InvalidArgumentException;
import de.krkm.trex.reasoner.Reasoner;
import de.krkm.utilities.collectiontostring.CollectionToStringWrapper;
import de.unima.ki.debug.srex.SREXReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BattleOfRexes {
    public final static AxiomType[] TYPES = new AxiomType[]{AxiomType.SUBCLASS_OF, AxiomType.DISJOINT_CLASSES,
            AxiomType.DISJOINT_OBJECT_PROPERTIES, AxiomType.SUB_OBJECT_PROPERTY, AxiomType.OBJECT_PROPERTY_DOMAIN,
            AxiomType.OBJECT_PROPERTY_RANGE};

    public BattleOfRexes()
            throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, InvalidArgumentException {

            runTiming("/home/daniel/temp/ontologies/uma-random-0.05-arctan.owl");
    }

    public void runTiming(String ontologyFileName)
            throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology compareOntology = manager
                .loadOntologyFromOntologyDocument(new FileInputStream(ontologyFileName));
        OWLOntology cleanedOntology = getCleanedOntology(compareOntology);

        File cleanedFile = new File(ontologyFileName + "_cleaned");
        System.out.println(cleanedFile.getAbsolutePath());
        FileOutputStream cleanedStream = new FileOutputStream(cleanedFile);
        manager.saveOntology(cleanedOntology, new RDFXMLOntologyFormat(), cleanedStream);
        cleanedStream.close();

        SREXReasoner srex = new SREXReasoner();
        srex.loadOntology("/home/daniel/temp/ontologies/uma-random-0.05-arctan.owl_cleaned");

        srex.init();
        srex.materialize();

        Set<Set<OWLAxiom>> conflictsSREX = new HashSet<Set<OWLAxiom>>(srex.getConflictSets());

        Set<Set<OWLAxiom>> conflictsTREX = new HashSet<Set<OWLAxiom>>();

        Reasoner trex = new Reasoner(cleanedOntology);
        for (int i = 0; i < trex.getConceptDisjointness().getDimensionCol(); i++) {
            if (trex.getConceptDisjointness().get(i, i)) {
                conflictsTREX.addAll(trex.getConceptDisjointness().getExplanation(i, i).getDisjunction());
            }
        }

        for (Set<OWLAxiom> c : conflictsSREX) {
            System.out.println("SREX: " + new CollectionToStringWrapper(c));
            System.out.println("In TREX? " + conflictsTREX.contains(c));
        }

        for (Set<OWLAxiom> c : conflictsTREX) {
            System.out.println("TREX: " + new CollectionToStringWrapper(c));
            System.out.println("In SREX? " + conflictsSREX.contains(c));
        }
    }

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

    public static void main(String[] args)
            throws OWLOntologyCreationException, IOException, OWLOntologyStorageException, InvalidArgumentException {
        BattleOfRexes compare = new BattleOfRexes();
    }

    public <T extends OWLAxiom> Set<T> getAllAxioms(PelletReasoner reasoner, AxiomType<T> type) {
        OWLOntology o = reasoner.getRootOntology();
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();

        Set<T> res = new HashSet<T>();

        if (type == AxiomType.SUBCLASS_OF) {
            Set<OWLClass> concepts = o.getClassesInSignature();
            for (OWLClass c : concepts) {
                for (OWLClass d : concepts) {
                    if (c.equals(d)) {
                        continue;
                    }
                    OWLSubClassOfAxiom a = df.getOWLSubClassOfAxiom(c, d);
                    if (reasoner.isEntailed(a)) {
                        res.add((T) a);
                    }
                }
            }
        } else if (type == AxiomType.SUB_OBJECT_PROPERTY) {
            Set<OWLObjectProperty> properties = o.getObjectPropertiesInSignature();
            for (OWLObjectProperty c : properties) {
                for (OWLObjectProperty d : properties) {
                    if (c.equals(d)) {
                        continue;
                    }
                    OWLSubObjectPropertyOfAxiom a = df.getOWLSubObjectPropertyOfAxiom(c, d);
                    if (reasoner.isEntailed(a)) {
                        res.add((T) a);
                    }
                }
            }
        } else if (type == AxiomType.DISJOINT_CLASSES) {
            Set<OWLClass> concepts = o.getClassesInSignature();
            for (OWLClass c : concepts) {
                for (OWLClass d : concepts) {
                    if (c.equals(d)) {
                        continue;
                    }
                    OWLDisjointClassesAxiom a = df.getOWLDisjointClassesAxiom(c, d);
                    if (reasoner.isEntailed(a)) {
                        res.add((T) a);
                    }
                }
            }
        } else if (type == AxiomType.DISJOINT_OBJECT_PROPERTIES) {
            Set<OWLObjectProperty> properties = o.getObjectPropertiesInSignature();
            for (OWLObjectProperty c : properties) {
                for (OWLObjectProperty d : properties) {
                    if (c.equals(d)) {
                        continue;
                    }
                    OWLDisjointObjectPropertiesAxiom a = df.getOWLDisjointObjectPropertiesAxiom(c, d);
                    if (reasoner.isEntailed(a)) {
                        res.add((T) a);
                    }
                }
            }
        } else if (type == AxiomType.OBJECT_PROPERTY_DOMAIN) {
            Set<OWLClass> concepts = o.getClassesInSignature();
            Set<OWLObjectProperty> properties = o.getObjectPropertiesInSignature();
            for (OWLObjectProperty p : properties) {
                for (OWLClass c : concepts) {
                    OWLObjectPropertyDomainAxiom a = df.getOWLObjectPropertyDomainAxiom(p, c);
                    if (reasoner.isEntailed(a)) {
                        res.add((T) a);
                    }
                }
            }
        } else if (type == AxiomType.OBJECT_PROPERTY_RANGE) {
            Set<OWLClass> concepts = o.getClassesInSignature();
            Set<OWLObjectProperty> properties = o.getObjectPropertiesInSignature();
            for (OWLObjectProperty p : properties) {
                for (OWLClass c : concepts) {
                    OWLObjectPropertyRangeAxiom a = df.getOWLObjectPropertyRangeAxiom(p, c);
                    if (reasoner.isEntailed(a)) {
                        res.add((T) a);
                    }
                }
            }
        }

        return res;
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
}
