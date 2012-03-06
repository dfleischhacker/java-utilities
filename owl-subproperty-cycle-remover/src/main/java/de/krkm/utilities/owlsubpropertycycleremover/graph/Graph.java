package de.krkm.utilities.owlsubpropertycycleremover.graph;

import java.util.*;

/**
 * Simple implementation of a directed graph including a Dijkstra implementation
 */
public class Graph {

    private ArrayList<Node> nodes;
    private HashMap<String, Node> nameToNode;

    /**
     * Initializes an empty graph.
     */
    public Graph() {
        nodes = new ArrayList<Node>();
        nameToNode = new HashMap<String, Node>();
    }

    /**
     * Returns a list of all nodes in this graph.
     *
     * @return list of all nodes contained in this graph
     */
    public ArrayList<Node> getNodes() {
        return nodes;
    }

    /**
     * Returns the node with the given <code>name</code> or null if no such node exists.
     *
     * @param name name to get node for
     * @return node with given name or null of no such node is contained in this graph
     */
    public Node getNodeByName(String name) {
        return nameToNode.get(name);
    }

    /**
     * Returns the shortest path from <code>start</code> to <code>end</code>. If no such path exists, <code>null</code>
     * is returned. A path returned by this method has at least a length of 1.
     *
     * @param start start node
     * @param end   goal node
     * @return shortest path between both nodes, null if no such path exists
     */
    public List<Node> getShortestPath(Node start, Node end) {
        HashMap<Node, Integer> distances = new HashMap<Node, Integer>();
        HashMap<Node, Node> previous = new HashMap<Node, Node>();

        for (Node n : nodes) {
            distances.put(n, Integer.MAX_VALUE);
        }

        ArrayList<Node> queue = new ArrayList<Node>();
        // ignore paths of length 0 by treating the start node here
        for (Node succ : start.getOutEdges()) {
            distances.put(succ, 1);
            previous.put(succ, start);
            queue.add(succ);
        }

        while (!queue.isEmpty()) {
            Node minNode = getMinNode(queue, distances);
            int minNodeDist = distances.get(minNode);
            for (Node succ : minNode.getOutEdges()) {
                if (minNodeDist + 1 < distances.get(succ)) {
                    distances.put(succ, minNodeDist + 1);
                    previous.put(succ, minNode);
                    queue.add(succ);
                }
            }
            if (minNode == end) {
                List<Node> res = new LinkedList<Node>();
                res.add(end);
                Node anc = end;
                do {
                    anc = previous.get(anc);
                    res.add(0, anc);
                    if (anc == null) {
                        throw new RuntimeException("ERROR");
                    }
                } while (anc != start);
                return res;
            }
        }

        return null;
    }

    /**
     * Returns the node from <code>nodes</code> having the smallest distance value assigned in <code>map</code>.
     *
     * @param nodes list to traverse
     * @param map map to get distance values from
     * @return node from <code>nodes</code> having the smallest distance according to <code>map</code>
     */
    private Node getMinNode(ArrayList<Node> nodes, final HashMap<Node, Integer> map) {
        Node res = Collections.min(nodes, new Comparator<Node>() {
            public int compare(Node node, Node node1) {
                return map.get(node).compareTo(map.get(node1));
            }
        });
        nodes.remove(res);
        return res;
    }

    /**
     * Returns the node named <code>nodeName</code>. The node is generated if it is not yet existing in this graph.
     *
     * @param nodeName name of node to return
     * @return node with given name
     */
    public Node getNode(String nodeName) {
        if (nameToNode.containsKey(nodeName)) {
            return nameToNode.get(nodeName);
        }
        Node node = new Node(nodeName);
        nodes.add(node);
        nameToNode.put(nodeName, node);

        return node;
    }

    public static void __main(String[] args) {
        Graph g = new Graph();
        Node n1 = g.getNode("n1");
        Node n2 = g.getNode("n2");
        Node n3 = g.getNode("n3");
        Node n4 = g.getNode("n4");
        Node n5 = g.getNode("n5");
        Node n6 = g.getNode("n6");
        Node n7 = new Node("n7");
        n1.addOutEdge(n2);
        n1.addOutEdge(n3);
        n1.addOutEdge(n6);
        n2.addOutEdge(n3);
        n2.addOutEdge(n4);
        n3.addOutEdge(n4);
        n3.addOutEdge(n1);
        n4.addOutEdge(n5);
        n5.addOutEdge(n1);
        n6.addOutEdge(n5);

        List<Node> path = g.getShortestPath(n1, n1);


        if (path == null) {
            System.out.println("No path found");
        } else {
            System.out.println("Length: " + path.size());
            for (Node n : path) {
                System.out.println(n);
            }
        }
    }
}
