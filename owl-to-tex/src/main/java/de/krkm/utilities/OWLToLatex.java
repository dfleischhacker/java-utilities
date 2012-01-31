package de.krkm.utilities;

import de.krkm.util.owlparser.ParserException;
import org.coode.owlapi.latex.LatexOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Provides convenience methods to get the LaTeX representation
 */
public class OWLToLatex {
    /**
     * Returns the LaTeX string for the given axioms represented in OWL Functional syntax.
     *
     * @param owlString OWL Functional representation to convert to LaTeX
     * @return LaTeX representation of the given OWL string
     */
    public static String getLatexForOWL(String owlString)
        throws ParserException, OWLOntologyCreationException, OWLOntologyStorageException,
               UnsupportedEncodingException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        System.out.println("LALALALA");
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(owlString.getBytes()));
        System.out.println("LALALALA");
        for (OWLAxiom ax : ontology.getAxioms()) {
            System.out.println(ax);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OWLOntologyFormat format = new LatexOntologyFormat();
        manager.saveOntology(ontology, format, out);
        return out.toString("UTF-8");
    }

    public static void main(String[] args)
        throws ParserException, OWLOntologyCreationException, OWLOntologyStorageException,
               UnsupportedEncodingException {
        System.out.println(getLatexForOWL(
            "Prefix(:=<http://www.example.com/ontology1#>)\n" +
            "Ontology( <http://www.example.com/ontology1>\n" +
            "DisjointObjectProperties(<http://dbpedia.org/ontology/parent> <http://dbpedia.org/ontology/predecessor> ) \n" +
            "ObjectPropertyAssertion(<http://dbpedia.org/ontology/parent> <http://dbpedia.org/resource/Elizabeth_II> <http://dbpedia.org/resource/George_VI_of_the_United_Kingdom>) \n" +
            "ObjectPropertyAssertion(<http://dbpedia.org/ontology/predecessor> <http://dbpedia.org/resource/Elizabeth_II> <http://dbpedia.org/resource/George_VI_of_the_United_Kingdom>)" +
            ")"));


        "Prefix(:=<http://www.example.com/ontology1#>)\n" +
        "Ontology( <http://www.example.com/ontology1>\n" +
        "DisjointObjectProperties(<http://dbpedia.org/ontology/parent> <http://dbpedia.org/ontology/predecessor> ) \n" +
        "ObjectPropertyAssertion(<http://dbpedia.org/ontology/parent> <http://dbpedia.org/resource/Elizabeth_II> <http://dbpedia.org/resource/George_VI_of_the_United_Kingdom>) \n" +
        "ObjectPropertyAssertion(<http://dbpedia.org/ontology/predecessor> <http://dbpedia.org/resource/Elizabeth_II> <http://dbpedia.org/resource/George_VI_of_the_United_Kingdom>)" +
        ")
    }
}
