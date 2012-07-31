package de.krkm.utilities.owlcompare;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.sun.javaws.exceptions.InvalidArgumentException;
import de.krkm.trex.reasoner.TRexReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.BufferingMode;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides methods to compare a given ontology to the axiom inferrable by Pellet.
 */
public class OWLCompare {
    public final static AxiomType[] TYPES = new AxiomType[]{AxiomType.SUBCLASS_OF, AxiomType.DISJOINT_CLASSES,
            AxiomType.DISJOINT_OBJECT_PROPERTIES, AxiomType.SUB_OBJECT_PROPERTY, AxiomType.OBJECT_PROPERTY_DOMAIN,
            AxiomType.OBJECT_PROPERTY_RANGE};

    public OWLCompare(String baseOntologyName, String compareToOntologyName)
            throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, InvalidArgumentException {


        OWLOntologyManager manager1 = OWLManager.createOWLOntologyManager();
        OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
        OWLOntology baseOntology = manager1.loadOntologyFromOntologyDocument(new FileInputStream(baseOntologyName));
        OWLOntology compareOntology = manager2
                .loadOntologyFromOntologyDocument(new FileInputStream(compareToOntologyName));
        OWLOntology cleanedOntology = manager2.createOntology();
        Set<AxiomType> typeSet = new HashSet<AxiomType>();
        Collections.addAll(typeSet, TYPES);

        for (OWLAxiom ax : compareOntology.getAxioms()) {
            if (typeSet.contains(ax.getAxiomType()) && ax.getSignature().size() > 1 && isAtomic(ax)) {
                manager2.addAxiom(cleanedOntology, ax);
            }
        }
        File cleanedFile = new File(compareToOntologyName + "_cleaned");
        System.out.println(cleanedFile.getAbsolutePath());
        FileOutputStream cleanedStream = new FileOutputStream(cleanedFile);
        manager2.saveOntology(cleanedOntology, cleanedStream);


        TRexReasoner patternReasoner = new TRexReasoner(baseOntology);


        PelletReasoner reasoner = new PelletReasoner(cleanedOntology, BufferingMode.BUFFERING);
        cleanedStream.close();

        System.out.println("Axioms contained in baseOntology but not in compared ontology");
        for (OWLAxiom ax : patternReasoner.getAxioms()) {
            if (!reasoner.isEntailed(ax)) {
                System.out.println("- " + ax);
            }
        }

        System.out.println(
                "=======================================================\nAxioms contained in compared ontology but " +
                        "not baseOntology");
        for (AxiomType t : TYPES) {
            //noinspection unchecked
            for (OWLAxiom a : (Set<OWLAxiom>) getAllAxioms(reasoner, t)) {
                if (!patternReasoner.isEntailed(a)) {
                    System.out.println("- " + a);
                }
            }
        }
    }

    public static void main(String[] args)
            throws OWLOntologyCreationException, IOException, OWLOntologyStorageException, InvalidArgumentException {
        new OWLCompare(args[0], args[0]);
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
    public boolean isAtomic(OWLAxiom axiom) throws InvalidArgumentException {
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
            for (OWLClass cls : casted.getClassesInSignature()) {
                if (!cls.isAnonymous()) {
                    return false;
                }
            }
            return true;
        }

        if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
            OWLDisjointObjectPropertiesAxiom casted = (OWLDisjointObjectPropertiesAxiom) axiom;
            for (OWLObjectProperty prop : casted.getObjectPropertiesInSignature()) {
                if (!prop.isAnonymous()) {
                    return false;
                }
            }
            return true;
        }

        if (axiom instanceof OWLObjectPropertyDomainAxiom) {
            OWLObjectPropertyDomainAxiom casted = (OWLObjectPropertyDomainAxiom) axiom;
            for (OWLObjectProperty prop : casted.getObjectPropertiesInSignature()) {
                if (!prop.isAnonymous()) {
                    return false;
                }
            }

            return !casted.getClass().isAnonymousClass();
        }

        if (axiom instanceof OWLObjectPropertyRangeAxiom) {
            OWLObjectPropertyRangeAxiom casted = (OWLObjectPropertyRangeAxiom) axiom;
            for (OWLObjectProperty prop : casted.getObjectPropertiesInSignature()) {
                if (!prop.isAnonymous()) {
                    return false;
                }
            }

            return !casted.getClass().isAnonymousClass();
        }

        throw new InvalidArgumentException(new String[]{axiom.getAxiomType().toString() + " not supported"});
    }
}
