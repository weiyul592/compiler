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

/**
 *
 * @author Weiyu, Amir
 */

/* All the nodes are represented by integers */
public class InterferenceGraph {
    private HashMap<Integer, Node> nodes;
    
    public class Node {
        Integer nodeNumber;
        List<Integer> outEdges;
        
        public Node(Integer number) {
            nodeNumber = number;
            outEdges = new ArrayList<>();
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
    }
    
    public InterferenceGraph() {
        nodes = new HashMap<>();
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
}