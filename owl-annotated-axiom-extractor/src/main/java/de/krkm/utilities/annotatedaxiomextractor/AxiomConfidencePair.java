package de.krkm.utilities.annotatedaxiomextractor;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * A pair representing an OWL axiom and a corresponding confidence value.
 * <p/>
 * Note: this class has a natural ordering that is inconsistent with equals. Actually, compareTo=0 is required but not
 * sufficient for equivalence.
 */
@SuppressWarnings("RedundantIfStatement")
public class AxiomConfidencePair implements Comparable<AxiomConfidencePair> {
    private OWLAxiom axiom;
    private Double confidence;

    public AxiomConfidencePair(OWLAxiom axiom, Double confidence) {
        this.axiom = axiom;
        this.confidence = confidence;
    }

    public int compareTo(AxiomConfidencePair axiomConfidencePair) {
        if (this.confidence < axiomConfidencePair.confidence) {
            return -1;
        }
        if (this.confidence > axiomConfidencePair.confidence) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AxiomConfidencePair that = (AxiomConfidencePair) o;

        if (!axiom.equals(that.axiom)) {
            return false;
        }
        if (!confidence.equals(that.confidence)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "AxiomConfidencePair{" +
               "axiom=" + axiom +
               ", confidence=" + confidence +
               '}';
    }

    @Override
    public int hashCode() {
        int result = axiom.hashCode();
        result = 31 * result + confidence.hashCode();
        return result;
    }

    /**
     * Returns the axiom for this pair
     * @return axiom for this pair
     */
    public OWLAxiom getAxiom() {
        return axiom;
    }

    /**
     * Returns the confidence value for this pair
     * @return confidence for this pair
     */
    public Double getConfidence() {
        return confidence;
    }
}
