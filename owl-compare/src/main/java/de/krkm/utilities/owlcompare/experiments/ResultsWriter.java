package de.krkm.utilities.owlcompare.experiments;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides methods to manage results tables and create files out of it.
 */
public class ResultsWriter {
    private HashMap<String,HashMap<String,String>> data;
    private String[] colHeaders;

    public ResultsWriter(String[] colHeaders) {
        data = new HashMap<String, HashMap<String,String>>();
        this.colHeaders = colHeaders;
    }

    public void addData(String rowName, String colName, String value) {
        if (!data.containsKey(rowName)) {
            data.put(rowName, new HashMap<String, String>());
        }
        data.get(rowName).put(colName, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (data.size() == 0 || data.entrySet().iterator().next().getValue().size() == 0) {
            return "";
        }

        sb.append("| ");
        for (String key : colHeaders) {
            sb.append(" | ").append(key);
        }
        sb.append(" |\n");

        for (Map.Entry<String, HashMap<String, String>> row : data.entrySet()) {
            sb.append(row.getKey()).append(" | ");
            for (String key : colHeaders) {
                sb.append(" | ").append(row.getValue().get(key));
            }
            sb.append(" | \n");
        }

        return sb.toString();
    }
}
