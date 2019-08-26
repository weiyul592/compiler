/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import SSA.Instruction;
import SSA.Opcode;
import CFG.ControlFlowGraph;
import CFG.BasicBlock;
import Lex.Result;
import Lex.Result.ResultType;
import Backend.Graph.Node;
/**
 *
 * @author Weiyu, Amir
 */
public class RegisterAllocator {
    private HashMap< Instruction, Set<Integer> > liveRanges;
    private Graph interGraph;

    public RegisterAllocator() {
        interGraph = new Graph();
    }

    public void execute(ControlFlowGraph cfg) {
        Liveness analysis = new Liveness();
        liveRanges = analysis.computeLiveSets(cfg);
        // analysis.printInstOut();
        
        // build interference graph
        buildGraph();
        coalesce(cfg);
        interGraph.dumpGraph(null);
        
        List<Integer> elim_order = MCS();
        
        HashMap<Integer, Integer> coloring = greedy_coloring(elim_order);
        //interGraph.dumpGraph(coloring);
    }

    // build interference graph
    private void buildGraph() {
        for (Set<Integer> liveSet : liveRanges.values()) {
            List<Integer> liveList = new ArrayList<>(liveSet);
            
            for (int i = 0; i < liveList.size(); i++) {
                for (int j = i + 1; j < liveList.size(); j++) {
                    Integer node1 = liveList.get(i);
                    Integer node2 = liveList.get(j);
                    interGraph.addEdge(node1, node2);
                }
            }
            
            if ( liveList.size() == 1) {
                interGraph.addNode( liveList.get(0) );
            }
        }
    }
    
    /* Maximum Cardinality Search algorithm. 
     * Returns a Simplicial Elimination Ordering */
    private List<Integer> MCS() {
        HashMap<Node, Integer> cost_map = new HashMap<>();
        List<Integer> SimElimOrder = new ArrayList();   
        Set<Node> nodeSet = new HashSet<>(interGraph.getNodes());
        
        for (Node node : nodeSet)
            cost_map.put(node, 0);
        
        int nodeSetSize = nodeSet.size();
        for (int i = 0; i < nodeSetSize; i++) {
            // find the node with the max cost
            Node maxnode = null;
            int maxcost = -1;
            
            for (Node node : nodeSet) {
                int cost = cost_map.get(node);
                if ( maxcost < cost ) {
                    maxcost = cost;
                    maxnode = node;
                }
            }
            
            SimElimOrder.add(maxnode.getNodeName());
            for (Integer neighbor : maxnode.getEdges()) {
                Node neighborNode = interGraph.getNode(neighbor);
                
                if (nodeSet.contains(neighborNode)) {
                    Integer original_cost = cost_map.get(neighborNode);
                    cost_map.put(neighborNode, original_cost + 1);
                }
                nodeSet.remove(maxnode);
            }
        }
        
        return SimElimOrder;
    }
    
    /* greedy coloring algorithm */
    private HashMap<Integer, Integer> greedy_coloring(List<Integer> elim_order) {
        HashMap<Integer, Integer> coloring = new HashMap<>();
        
        for (Integer node_name : interGraph.getNodeNames()) {
            coloring.put(node_name, -1);
        }
        
        for (Integer node_id : interGraph.getNodeNames()) {
            Node node = interGraph.getNode(node_id);
            HashSet<Integer> neighbors = node.getEdges();
            
            HashSet<Integer> neighbor_colors = new HashSet<>();
            for (Integer neighbor : neighbors) {
                Integer color = coloring.get(neighbor);
                neighbor_colors.add(color);
            }
            
            // find the lowest color not used by neighbors
            Integer lowest_color = 0;
            while (neighbor_colors.contains(lowest_color)) {
                lowest_color++;
            }
            coloring.put(node_id, lowest_color);
        }
        
        return coloring;
    }
    
    // coalsce live ranges for Phi nodes
    private void coalesce(ControlFlowGraph cfg) {
        List<Instruction> PhiInsts = new ArrayList<>();
        
        for (BasicBlock block : cfg.getBasicBlocks()) {
            for (Instruction inst : block.PHIInsts.values()) {
                PhiInsts.add(inst);
            }
        }
        
        // algorithm from class note
        for (Instruction inst : PhiInsts) {
            HashSet<Integer> cluster = new HashSet<>();
            HashSet<Integer> cluster_edges = new HashSet<>();
            
            Integer inst_number = inst.getInstNumber();
            cluster.add(inst_number);
            cluster_edges.addAll( interGraph.getOutEdges(inst_number) );
            
            for (Result operand : inst.getOperands()) {
                if (operand.getType() == ResultType.CONSTANT)
                    continue;
                
                Integer op_inst_number = operand.getInstNumber();
                if ( !cluster_edges.contains(op_inst_number) ) {
                    HashSet<Integer> out_edges = interGraph.getOutEdges(op_inst_number);
                    cluster.add(op_inst_number);
                    
                    if (out_edges != null)
                        cluster_edges.addAll( out_edges );
                    
                    interGraph.removeNode(op_inst_number);
                }
            }
            
            // remove the original nove
            interGraph.removeNode(inst_number);
            
            // create cluster node
            Integer clusterID = interGraph.getClusterCounter();
            interGraph.addNode(clusterID);
            for (Integer edge : cluster_edges) {
                interGraph.addEdge(clusterID, edge);
            }
            interGraph.setCluster(clusterID);
            interGraph.addCluster(clusterID, cluster);
        }
    }
    
    private void mergeClusters() {
    
    }
}