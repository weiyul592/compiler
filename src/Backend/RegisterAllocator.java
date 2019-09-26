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
import SSA.DefUseChain;
import java.util.Objects;
/**
 *
 * @author Weiyu, Amir
 */
public class RegisterAllocator {
    private HashMap< Instruction, Set<Integer> > liveRanges;
    private Graph interGraph;
    private HashMap<Integer, Integer> coloring;
    private List<Instruction> PhiInsts;
    private List<Instruction> PhiInstsToBeEliminated;
    DefUseChain defUseChain;

    public RegisterAllocator() {
        interGraph = new Graph();
        coloring = new HashMap<>();
        PhiInsts = new ArrayList<>();
        PhiInstsToBeEliminated = new ArrayList<>();
        defUseChain = DefUseChain.getInstance();
    }

    public void execute(ControlFlowGraph cfg) {
        Liveness analysis = new Liveness();
        liveRanges = analysis.computeLiveSets(cfg);
        // analysis.printInstOut();
        
        // build interference graph
        buildGraph();
        coalesce(cfg);
        //interGraph.dumpGraph(null);
        
        List<Integer> elim_order = MCS();
        greedy_coloring(elim_order);
        //interGraph.dumpGraph(coloring);
        
        eliminatedPhi(coloring, cfg);
        //defUseChain.print();
        
        cfg.attachRegisters(coloring);
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
    private void greedy_coloring(List<Integer> elim_order) {
        for (Integer node_name : interGraph.getNodeNames()) {
            coloring.put(node_name, -1);
            
            Node node = interGraph.getNode(node_name);
            if (node.isCluster()) {
                HashSet<Integer> cluster = interGraph.getCluster(node_name);
                for (Integer member : cluster) {
                    coloring.put(member, -1);
                }
            }
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
            if (node.isCluster()) {
                HashSet<Integer> cluster = interGraph.getCluster(node_id);
                for (Integer member : cluster) {
                    coloring.put(member, lowest_color);
                }
            }
        }
    }
    
    // coalsce live ranges for Phi nodes
    private void coalesce(ControlFlowGraph cfg) {
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
                    if (out_edges == null)
                        continue;
                    
                    cluster.add(op_inst_number);
                    cluster_edges.addAll( out_edges );
                    
                    interGraph.removeNode(op_inst_number);
                }
            }
            
            // replace the original node by cluster
            interGraph.removeNode(inst_number);
            
            Integer clusterID = interGraph.getClusterCounter();
            interGraph.addNode(clusterID);
            for (Integer edge : cluster_edges) {
                interGraph.addEdge(clusterID, edge);
            }
            interGraph.setCluster(clusterID);
            interGraph.addCluster(clusterID, cluster);
            
            // This Phi Instruction can be eliminated
            if (cluster.size() == 3) {
                PhiInstsToBeEliminated.add(inst);
            }
        }
    }
    
    private void eliminatedPhi(HashMap<Integer, Integer> coloring, ControlFlowGraph cfg) {
        // HashSet<Instruction> eliminatedInsts = new HashSet<>();
                
        // Insert move instructions
        for (Instruction phi : PhiInsts) {
            if ( !PhiInstsToBeEliminated.contains(phi) ) {
                int inst_color = coloring.get(phi.getInstNumber());
                
                List<Result> operands = phi.getOperands();
                List<Integer> colors = new ArrayList<>();
                for (Result operand : operands)
                    colors.add( coloring.get(operand.getInstNumber()) );
                
                if (colors.size() == 2) {
                    int op1_color = colors.get(0);
                    if (op1_color != inst_color) {
                        Instruction op1_def = cfg.getInstruction(operands.get(0).getInstNumber());
                        BasicBlock phiBlock = phi.getBBl();
                        BasicBlock op1_def_block = op1_def.getBBl();
                        Integer index = search(phiBlock, op1_def_block);
                        
                        BasicBlock parent = phiBlock.getParent().get(index);
			Instruction old_last = parent.getLastInst();
                        Instruction newInst = Instruction.addNewInst(parent, Opcode.MOVE, operands.get(0), Result.InstResult(phi.getInstNumber()));

                        Opcode opcode = old_last.getOpcode();
                        if (opcode == Opcode.BEQ || opcode == Opcode.BGE
                            || opcode == Opcode.BGT || opcode == Opcode.BLE
                            || opcode == Opcode.BLT || opcode == Opcode.BNE
                            || opcode == Opcode.BRA) {
                            parent.InsertBefore(newInst, old_last);
                        }
                    }

                    int op2_color = colors.get(1);
                    if (op2_color != inst_color) {
                        Instruction op2_def = cfg.getInstruction(operands.get(1).getInstNumber());
                        BasicBlock phiBlock = phi.getBBl();
                        BasicBlock op2_def_block = op2_def.getBBl();
                        Integer index = search(phiBlock, op2_def_block);
                        
                        BasicBlock parent = phiBlock.getParent().get(index);
			Instruction old_last = parent.getLastInst();
                        Instruction newInst = Instruction.addNewInst(parent, Opcode.MOVE, operands.get(1), Result.InstResult(phi.getInstNumber()));
                        
                        Opcode opcode = old_last.getOpcode();
                        if (opcode == Opcode.BEQ || opcode == Opcode.BGE
                            || opcode == Opcode.BGT || opcode == Opcode.BLE
                            || opcode == Opcode.BLT || opcode == Opcode.BNE
                            || opcode == Opcode.BRA) {
                            parent.InsertBefore(newInst, old_last);
                        }
                    }
                }
            }
        }

        for (Instruction phi : PhiInsts) {
            BasicBlock block = phi.getBBl();
            block.removeInst(phi);
            fixBranchDestination(phi);
        }
    }
    
    /* phiBlock: the basic block containing the phi instruction
     * op_def_block: the basic block containing the definition of one of the phi instruction's operands
     *
     * return: the index of the phiBlock's parent that is dominated by op_def_block
     */
    private Integer search(BasicBlock phiBlock, BasicBlock op_def_block) {
        BasicBlock dominator = phiBlock.getImmeDominator();
        List<BasicBlock> parents = phiBlock.getParent();
        for (int index = 0; index < parents.size(); index++) {
            BasicBlock currBlock = parents.get(index);
            while (currBlock != dominator) {
                if (currBlock == op_def_block)
                   return index;

                currBlock = currBlock.getImmeDominator();
            }
            
            if (currBlock == op_def_block)
                return index;
        }
        
        return null;
    }
    
    // When phi instructions are eliminated, destinations of branch instructions need to be fixed
    private void fixBranchDestination(Instruction phi) {
        List<Instruction> uses = defUseChain.getUse(phi.getInstNumber());
        HashMap<Result, List<Instruction> > removeMap = new HashMap<>();
        
        for (Instruction inst : uses) {
            Opcode opcode = inst.getOpcode();
            Result oldBranchToResult = null;
            
            if (opcode == Opcode.BRA) {
                BasicBlock branchToBlock = phi.getBBl();
                Instruction newBranchTo = branchToBlock.getFirstInst();
                oldBranchToResult = inst.getOperand1();
                Result newBranchToResult = Result.InstResult(newBranchTo.getInstNumber());
                inst.setOperand1(newBranchToResult);
                
                defUseChain.addUse(newBranchToResult, inst);
            } else if (opcode == Opcode.BEQ || opcode == Opcode.BGE
                || opcode == Opcode.BGT || opcode == Opcode.BLE
                || opcode == Opcode.BLT || opcode == Opcode.BNE) {
                
                BasicBlock branchToBlock = phi.getBBl();
                Instruction newBranchTo = branchToBlock.getFirstInst();
                oldBranchToResult = inst.getOperand2();
                Result newBranchToResult = Result.InstResult(newBranchTo.getInstNumber());
                inst.setOperand2(newBranchToResult);
                
                defUseChain.addUse(newBranchToResult, inst);
            }
            
            // add to removeMap
            List<Instruction> removeList = removeMap.get(oldBranchToResult);
            if (removeList == null) {
                removeList = new ArrayList<>();
                removeMap.put(oldBranchToResult, removeList);
            }
            removeList.add(inst);
        }
        
        for (Result key : removeMap.keySet()) {
            List<Instruction> removeList = removeMap.get(key);
            for (Instruction inst : removeList) {
                defUseChain.removeUse(key, inst);
            }
        }
    }
    
    public HashMap<Integer, Integer> getColoring() { 
        return coloring;
    }
}
