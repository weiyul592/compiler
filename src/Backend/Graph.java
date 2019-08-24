/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Weiyu, Amir
 */

/* All the nodes are represented by integers */
public class Graph {
    private HashMap<Integer, Node> nodes;
    private HashMap<Integer, String> colors;
    
    public class Node {
        private Integer nodeNumber;
        private List<Integer> outEdges;
        
        public Node(Integer number) {
            nodeNumber = number;
            outEdges = new ArrayList<>();
        }
        
        public Node(Integer number, List<Integer> edges) {
            nodeNumber = number;
            outEdges = new ArrayList<>(edges);
        }
        
        public void addEdge(Integer y) {
            // add an edge if has not been added before
            if ( !outEdges.contains(y) ) {
                outEdges.add(y);
            }
        }
        
        public List<Integer> getEdges() {
            return outEdges;
        }
        
        public Integer getNodeName() {
            return nodeNumber;
        }
    }
    
    public Graph() {
        nodes = new HashMap<>();
        colors = new HashMap<>();
        
        // predefined color schemes
        colors.put(0, "black");
        colors.put(1, "red");
        colors.put(2, "blue");
        colors.put(3, "aqua");
        colors.put(4, "green");
        colors.put(5, "darkgray");
        colors.put(6, "coral");
        colors.put(7, "lightcyan");
        colors.put(8, "mediumorchid");
        colors.put(9, "khaki");
        colors.put(10, "teal");
        colors.put(11, "tomato");
    }
    
    public void addNode(Integer x) {
        // only add a node if has not been added before
        if ( !nodes.containsKey(x) ) {
            Node newNode = new Node(x);
            nodes.put(x, newNode);
        }
    }
    
    public void addEdge(Integer x, Integer y) {
        /* make sure x and y are added. If they are already in nodes, nothing is done */
        addNode(x);
        addNode(y);
        
        Node node_x = nodes.get(x);
        Node node_y = nodes.get(y);
        
        node_x.addEdge(y);
        node_y.addEdge(x);
    }
 
    // return the node with corresponding name
    public Node getNode(Integer x) {
        if (nodes.containsKey(x)) {
            return nodes.get(x);
        } else {
            System.out.println("Node " + x + " does not exist");
            return null;
        }
    }
    
    /* return all the node names (integers) in the interference graph. */
    public Set<Integer> getNodeNames() {
        return nodes.keySet();
    }
    
    public Collection<Node> getNodes() {
        return nodes.values();
    }
    
    /* return all the nodes connected to node x */
    public List<Integer> getOutEdges(Integer x) {
        Node node_x = nodes.get(x);
        return node_x.getEdges();
    }
    
    public void print() {
        for (Integer nodeName : nodes.keySet()) {
            Node node = nodes.get(nodeName);
            List<Integer> outEdges = node.getEdges();
            
            System.out.print(nodeName + ": ");
            System.out.println(outEdges);
        }
    }
    
    /* Dump grpah in .dot symtax */
    public void dumpGraph(HashMap<Integer, Integer> coloring) {
        System.out.println("graph tmp {");
        
        // print colors
        if (coloring != null) {
            for (Integer node_id : coloring.keySet()) {
                Integer color_id = coloring.get(node_id);
                String color_name = colors.get(color_id);

                StringBuilder retString = new StringBuilder();
                retString.append("\"" + node_id + "\"");
                retString.append("[fontcolor=\"" + color_name + "\"");
                retString.append("color=\"" + color_name + "\"]");
                System.out.println(retString.toString());
            }
        }
        
        HashMap<Integer, Node> nodes_copy = new HashMap<>();
        for (Integer key : nodes.keySet()) {
            Node original = nodes.get(key);
            Integer node_name = original.getNodeName();
            Node new_node = new Node(node_name, original.getEdges());            
            nodes_copy.put(node_name, new_node);
        }
        
        for (Integer nodeName : nodes_copy.keySet()) {
            Node node = nodes_copy.get(nodeName);
            List<Integer> outEdges = node.getEdges();
        
            for (Integer out_edge : outEdges) {
                System.out.println(nodeName + "--" + out_edge);
                
                // do not print redundant edges
                Node other_node = nodes_copy.get(out_edge);
                List<Integer> other_node_edges = other_node.getEdges();
                other_node_edges.remove(nodeName);
            }
        }
        System.out.println("}");
    }
}