package de.krkm.utilities.owlcompare;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.sun.javaws.exceptions.InvalidArgumentException;
import de.krkm.patterndebug.reasoner.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.InferenceType;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TimedOWLCompare {
    public final static AxiomType[] TYPES = new AxiomType[]{AxiomType.SUBCLASS_OF, AxiomType.DISJOINT_CLASSES,
            AxiomType.DISJOINT_OBJECT_PROPERTIES, AxiomType.SUB_OBJECT_PROPERTY, AxiomType.OBJECT_PROPERTY_DOMAIN,
            AxiomType.OBJECT_PROPERTY_RANGE};

    public TimedOWLCompare(String ontologyFileName, String inPatternFile, String inPelletFile)
            throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, InvalidArgumentException {


        OWLOntologyManager manager1 = OWLManager.createOWLOntologyManager();
        OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
        OWLOntology baseOntology = manager1.loadOntologyFromOntologyDocument(new FileInputStream(ontologyFileName));
        OWLOntology compareOntology = manager2
                .loadOntologyFromOntologyDocument(new FileInputStream(ontologyFileName));
        OWLOntology cleanedOntology = manager2.createOntology();
        Set<AxiomType> typeSet = new HashSet<AxiomType>();
        Collections.addAll(typeSet, TYPES);

        for (OWLAxiom ax : compareOntology.getAxioms()) {
            if (typeSet.contains(ax.getAxiomType()) && ax.getSignature().size() > 1 && isAtomic(ax)) {
                manager2.addAxiom(cleanedOntology, ax);
            }
        }
        File cleanedFile = new File(ontologyFileName + "_cleaned");
        System.out.println(cleanedFile.getAbsolutePath());
        FileOutputStream cleanedStream = new FileOutputStream(cleanedFile);
        manager2.saveOntology(cleanedOntology, cleanedStream);
        cleanedStream.close();

        FileWriter inPatternWriter = new FileWriter(inPatternFile);
        FileWriter inPelletWriter = new FileWriter(inPelletFile);

        long patternStart = System.currentTimeMillis();
        Reasoner patternReasoner = new Reasoner(cleanedOntology);
        long patternRuntime = System.currentTimeMillis() - patternStart;

        long pelletStart = System.currentTimeMillis();
        PelletReasoner reasoner = new PelletReasoner(cleanedOntology, BufferingMode.BUFFERING);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.OBJECT_PROPERTY_HIERARCHY,
                InferenceType.DISJOINT_CLASSES);
        long pelletRuntime = System.currentTimeMillis() - pelletStart;

        System.out.println("Axioms contained in baseOntology but not in compared ontology");
        patternStart = System.currentTimeMillis();
        Set<OWLAxiom> patternReasonerAxioms = patternReasoner.getAxioms();
        System.out.println("Pattern Reasoner inferred: " + patternReasonerAxioms.size());
        patternRuntime += (System.currentTimeMillis() - patternStart);
        for (OWLAxiom ax : patternReasonerAxioms) {
            pelletStart = System.currentTimeMillis();
            boolean isEntailed = reasoner.isEntailed(ax);
            pelletRuntime += System.currentTimeMillis() - pelletStart;
            if (!isEntailed) {
                inPatternWriter.write(ax.toString());
                inPatternWriter.write(patternReasoner.getExplanation(ax).toString());
                inPatternWriter.write("\n");
            }
        }
        inPatternWriter.close();

        System.out.println(
                "=======================================================\nAxioms contained in compared ontology but " +
                        "not baseOntology");
        PelletExplanation.setup();
        PelletExplanation expl = new PelletExplanation(cleanedOntology);
        for (AxiomType t : TYPES) {
            pelletStart = System.currentTimeMillis();
            //noinspection unchecked
            Set<OWLAxiom> pelletAxioms = (Set<OWLAxiom>) getAllAxioms(reasoner, t);
            pelletRuntime += System.currentTimeMillis() - pelletStart;
            for (OWLAxiom a : pelletAxioms) {
                patternStart = System.currentTimeMillis();
                boolean isEntailed = patternReasoner.isEntailed(a);
                patternRuntime += System.currentTimeMillis() - patternStart;
                if (!isEntailed) {
                    inPelletWriter.write(a.toString() + " -- ");
                    try {
                        for (Set<OWLAxiom> e : expl.getEntailmentExplanations(a)) {
                            for (OWLAxiom e2 : e) {
                                inPelletWriter.write(e2.toString() + ", ");
                            }
                        }
                    }
                    catch (OWLRuntimeException e) {
                        e.printStackTrace();
                    }
                    inPelletWriter.write("\n");
                }
            }
        }
        inPelletWriter.close();

        System.out.println("Runtime Pattern: " + patternRuntime);
        System.out.println("Runtime Pellet: " + pelletRuntime);

        OWLDataFactory dataFactory = manager2.getOWLDataFactory();
        OWLAxiom ax = dataFactory
                .getOWLDisjointClassesAxiom(dataFactory.getOWLClass(
                        IRI.create("http://dbpedia.org/ontology/BadmintonPlayer")),
                        dataFactory.getOWLClass(
                                IRI.create("http://dbpedia.org/ontology/SpeedwayLeague")
                        ));
        System.out.println(reasoner.isEntailed(ax));
    }

    public static void main(String[] args)
            throws OWLOntologyCreationException, IOException, OWLOntologyStorageException, InvalidArgumentException {
        new TimedOWLCompare(args[0], args[1], args[2]);
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
