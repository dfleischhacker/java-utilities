package de.krkm.utilities.owlcompare;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.sun.javaws.exceptions.InvalidArgumentException;
import de.krkm.trex.reasoner.TRexReasoner;
import de.unima.ki.debug.srex.SREXReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BattleOfRexes {
    public final static AxiomType[] TYPES = new AxiomType[]{AxiomType.SUBCLASS_OF, AxiomType.DISJOINT_CLASSES,
            AxiomType.DISJOINT_OBJECT_PROPERTIES, AxiomType.SUB_OBJECT_PROPERTY, AxiomType.OBJECT_PROPERTY_DOMAIN,
            AxiomType.OBJECT_PROPERTY_RANGE};

    public BattleOfRexes()
            throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, InvalidArgumentException,
            InterruptedException {
        System.out.println("Waiting for 10 secs to allow configuration of VisualVM");
        Thread.sleep(20000);
        System.out.println("Done waiting");
        runTiming("/home/daniel/temp/ontologies/uma-random-0.05-arctan.owl");
    }

    public void runTiming(String ontologyFileName)
            throws IOException, OWLOntologyStorageException,
            OWLOntologyCreationException {
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

//        Set<OWLObjectProperty> conflictsTREX = new HashSet<Set<OWLAxiom>>();

        TRexReasoner trex = new TRexReasoner(cleanedOntology);
        /*for (int i = 0; i < trex.getConceptDisjointness().dimensionCol; i++) {
            if (trex.conceptDisjointness.get(i, i)) {
                conflictsTREX.addAll(trex.getConceptDisjointness().getExplanation(i, i).getDisjunction());
            }
        }*/

        /*conflictsTREX = trex.getIncoherentProperties();
        System.out.println("Conflicts not in TRex but in SRex");


        for (Set<OWLAxiom> c : conflictsSREX) {
            if (!conflictsTREX.contains(c)) {
                System.out.println(new CollectionToStringWrapper(c));
            }
        }

        System.out.println("Conflicts not in SRex but in TRex");
        for (Set<OWLAxiom> c : conflictsTREX) {
            if (!conflictsSREX.contains(c)) {
                System.out.println(new CollectionToStringWrapper(c));
            }
        }*/
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
            throws OWLOntologyCreationException, IOException, OWLOntologyStorageException, InvalidArgumentException,
            InterruptedException {
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
