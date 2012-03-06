package de.krkm.utilities.owlrandompartitioner;

/**
 * Exception thrown on errors when partitioning ontologies
 */
public class OntologyPartitioningException extends Exception {
    public OntologyPartitioningException() {
    }

    public OntologyPartitioningException(String s) {
        super(s);
    }

    public OntologyPartitioningException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public OntologyPartitioningException(Throwable throwable) {
        super(throwable);
    }
}
