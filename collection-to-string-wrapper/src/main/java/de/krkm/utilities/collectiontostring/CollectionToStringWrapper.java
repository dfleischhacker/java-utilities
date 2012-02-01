package de.krkm.utilities.collectiontostring;

import java.util.Collection;

/**
 * A simple wrapper class providing toString functionality for collection classes
 */
public class CollectionToStringWrapper {
    private Collection<?> coll;

    /**
     * Initializes this class to wrap the given collection
     * @param coll collection to wrap
     */
    public CollectionToStringWrapper(Collection<?> coll) {
        this.coll = coll;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Object o : coll) {
            sb.append("\"").append(o.toString()).append("\",");
        }
        sb.append("}");

        return sb.toString();
    }
}
