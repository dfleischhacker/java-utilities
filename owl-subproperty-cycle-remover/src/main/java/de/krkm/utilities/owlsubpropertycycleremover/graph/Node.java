package de.krkm.utilities.owlsubpropertycycleremover.graph;

import java.util.HashMap;
import java.util.HashSet;

public class Node {
    private HashSet<Node> outEdges;
    private HashSet<Node> inEdges;
    private HashMap<Node, Double> edgeWeight;

    private String name;

    public Node(String name) {
        this.name = name;
        outEdges = new HashSet<Node>();
        inEdges = new HashSet<Node>();
        edgeWeight = new HashMap<Node, Double>();
    }

    /**
     * Adds an outgoing edge to the given node.
     *
     * @param node node which should be endpoint of edge
     */
    public void addOutEdge(Node node) {
        outEdges.add(node);
        node.addInEdge(this);
    }

    /**
     * Adds an outgoing edge to the given node having the given <code>weight</code>.
     *
     * @param node   node which should be endpoint of edge
     * @param weight weight for edge
     */
    public void addOutEdge(Node node, Double weight) {
        addOutEdge(node);
        edgeWeight.put(node, weight);
    }

    /**
     * Deletes the out edge to the given <code>node</code>. If an edge has been removed, true is returned, otherwise
     * false.
     *
     * @param node node being end point of edge to remove
     * @return true if an edge has been removed, otherwise false
     */
    public boolean removeOutEdge(Node node) {
        node.inEdges.remove(this);
        return outEdges.remove(node);
    }

    /**
     * Adds an edge to the given node
     *
     * @param node node to which edge should be added
     */
    private void addInEdge(Node node) {
        this.inEdges.add(node);
    }

    /**
     * Returns a list containing all nodes reachable by out edges
     *
     * @return list containing all nodes reachable by out edges
     */
    public HashSet<Node> getOutEdges() {
        return outEdges;
    }

    /**
     * Returns the weight for the edge to the given <code>node</code>. If no weight exists for this edge, null is
     * returned
     *
     * @param node weight of edge to given node or null if no such weight exists
     */
    public Double getWeight(Node node) {
        return edgeWeight.get(node);
    }

    /**
     * Returns a list containing all nodes having edges to this node
     *
     * @return list containing all nodes having edges to this node
     */
    public HashSet<Node> getInEdges() {
        return inEdges;
    }


    /**
     * This node's unique name
     *
     * @return node's unique name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        if (!name.equals(node.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
