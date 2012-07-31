package de.krkm.utilities.owlcompare;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.sun.javaws.exceptions.InvalidArgumentException;
import de.krkm.trex.reasoner.TRexReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
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

    public TimedOWLCompare(File ontologyDirectory, File resultDirectory)
            throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, InvalidArgumentException {

        if (!resultDirectory.exists()) {
            resultDirectory.mkdirs();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(resultDirectory + File.separator + "timing.txt"));


        File[] files = ontologyDirectory.listFiles();

        if (files == null) {
            throw new RuntimeException("No ontologies found");
        }
        for (File ontologyFile : files) {
            if (!ontologyFile.getAbsolutePath().contains("0.02")) {
                continue;
            }
            if (!ontologyFile.getName().endsWith("owl")) {
                continue;
            }
            String inPatternFile = resultDirectory.getAbsolutePath() + File.separator + ontologyFile
                    .getName() + "_inpattern";
            String inPelletFile = resultDirectory.getAbsolutePath() + File.separator + ontologyFile
                    .getName() + "_inpellet";
            long[] times = runTiming(ontologyFile.getAbsolutePath(), inPatternFile, inPelletFile);
            writer.write(String.format("%s: pellet %d, pattern %d", ontologyFile, times[1], times[0]));

        }
        writer.close();
    }

    public long[] runTiming(String ontologyFileName, String inPatternFile, String inPelletFile)
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

        FileWriter inPatternWriter = new FileWriter(inPatternFile);
        FileWriter inPelletWriter = new FileWriter(inPelletFile);

        long patternStart = System.currentTimeMillis();
        TRexReasoner patternReasoner = new TRexReasoner(cleanedOntology);
        long patternRuntime = System.currentTimeMillis() - patternStart;

        long pelletStart = System.currentTimeMillis();
        PelletReasoner reasoner = new PelletReasoner(cleanedOntology, BufferingMode.BUFFERING);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.OBJECT_PROPERTY_HIERARCHY,
                InferenceType.DISJOINT_CLASSES);
        long pelletRuntime = System.currentTimeMillis() - pelletStart;

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
//                        System.out.println("Unable to get explanation");
                        e.printStackTrace();
                    }
                    inPelletWriter.write("\n");
                }
            }
        }
        inPelletWriter.close();

        System.out.println("Runtime Pattern: " + patternRuntime);
        System.out.println("Runtime Pellet: " + pelletRuntime);

        return new long[]{patternRuntime, pelletRuntime};
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
        TimedOWLCompare compare = new TimedOWLCompare(new File(args[0]), new File(args[1]));
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
