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
    private HashMap<Integer, Node> nodes;   // cluster number are numbered from 1000
    private HashMap<Integer, String> colors;
    private HashMap<Integer, HashSet<Integer>> clusters;
    private HashMap<Integer, Integer> nodeToClusterID;
    private Integer clusterID;
    
    public class Node {
        private Integer nodeNumber;
        private HashSet<Integer> outEdges;
        private boolean is_cluster;
        
        public Node(Integer number) {
            nodeNumber = number;
            outEdges = new HashSet<>();
            is_cluster = false;
        }
        
        public Node(Integer number, HashSet<Integer> edges) {
            nodeNumber = number;
            outEdges = new HashSet<>(edges);
        }
        
        public void addEdge(Integer x) {
            // add an edge if has not been added before
            if ( !outEdges.contains(x) ) {
                outEdges.add(x);
            }
        }
        
        public void removeEdge(Integer x) {
            outEdges.remove(x);
        }
        
        public void setCluster() {
            is_cluster = true;
        }
        
        public boolean isCluster() {
            return is_cluster;
        }
        
        public HashSet<Integer> getEdges() {
            return outEdges;
        }
        
        public Integer getNodeName() {
            return nodeNumber;
        }
    }
    
    public Graph() {
        nodes = new HashMap<>();
        colors = new HashMap<>();
        clusters = new HashMap<>();
        nodeToClusterID = new HashMap<>();
        clusterID = 1000;
        
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
    
    public void removeNode(Integer x) {
        Node node_x = nodes.get(x);
        if (node_x == null)
            return;
        
        HashSet<Integer> edges = node_x.getEdges();
        for (Integer out_edge : edges) {
            Node neighbor = nodes.get(out_edge);
            neighbor.removeEdge(x);
        }
        
        nodes.remove(x);
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

    public void addCluster(Integer clusterID, HashSet<Integer> members) {
        clusters.put(clusterID, members);
        
        for (Integer member : members) {
            nodeToClusterID.put(member, clusterID);
        }
    }
    
    public void setCluster(Integer clusterID) {
        Node cluster_node = nodes.get(clusterID);
        cluster_node.setCluster();
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
    public HashSet<Integer> getOutEdges(Integer x) {
        Node node_x = nodes.get(x);
        
        if (node_x == null)
            return null;
        
        return node_x.getEdges();
    }
    
    public Integer getClusterCounter() {
        return clusterID++;
    }
    
    public Integer getClusterID(Integer nodeID) {
        return nodeToClusterID.get(nodeID);
    }
    
    public HashSet<Integer> getCluster(Integer clusterID) {
        return clusters.get(clusterID);
    }
    
    public void print() {
        for (Integer nodeName : nodes.keySet()) {
            Node node = nodes.get(nodeName);
            HashSet<Integer> outEdges = node.getEdges();
            
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
            if (original.is_cluster) {
                new_node.setCluster();
            }
            
            nodes_copy.put(node_name, new_node);
        }
        
        for (Integer nodeName : nodes_copy.keySet()) {
            Node node = nodes_copy.get(nodeName);
            if (node.is_cluster) {
                HashSet<Integer> cluster = clusters.get(nodeName);
                
                StringBuilder retString = new StringBuilder();
                retString.append("\"" + nodeName + "\"");
                retString.append("[label=\"");
                retString.append(cluster.toString());
                retString.append("\"]");
                System.out.println(retString.toString());
                
            }
        }
        
        for (Integer nodeName : nodes_copy.keySet()) {
            Node node = nodes_copy.get(nodeName);
            HashSet<Integer> outEdges = node.getEdges();
        
            for (Integer out_edge : outEdges) {
                System.out.println(nodeName + "--" + out_edge);
                
                // do not print redundant edges
                Node other_node = nodes_copy.get(out_edge);
                HashSet<Integer> other_node_edges = other_node.getEdges();
                other_node_edges.remove(nodeName);
            }
        }
        System.out.println("}");
    }
}