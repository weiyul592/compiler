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
    
    // private Integer[][] LifeRanges = new Integer[10000][10000];

    public RegisterAllocator() {
        interGraph = new Graph();
    }

    public void execute(ControlFlowGraph cfg) {
        Liveness analysis = new Liveness();
        liveRanges = analysis.computeLiveSets(cfg);
        
//        analysis.printInstOut();
        
        buildGraph();
        interGraph.print();
        MCS();
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
    List<Integer> MCS() {
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
}