/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import CFG.BasicBlock;
import CFG.ControlFlowGraph;
import Lex.Result;
import SSA.Instruction;
import SSA.Opcode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Weiyu, Amir
 */
public class Liveness {
    private List< List<BasicBlock> > globalPaths;
    private HashMap< Integer, Set<Integer> > globalLiveRanges;
    
    public Liveness() {
        globalPaths = new ArrayList<>();
        globalLiveRanges = new HashMap<>();
    }
    
    public HashMap<Integer, Set<Integer> > computeLiveRanges(ControlFlowGraph cfg) {
        getReversePath(cfg);
        getLiveRanges();
        
        return globalLiveRanges;
    }
    
    private void getReversePath(ControlFlowGraph cfg) {
        List<BasicBlock> basicBlocks = cfg.getBasicBlocks();
        List<BasicBlock> endBlocks = new ArrayList<>();
        
        // find all blocks that have no successors (children)
        for (BasicBlock block : basicBlocks) {
            if (block.getChildrenBlock().isEmpty()) {
                System.out.println(block.toStr());
                endBlocks.add(block);
            }
        }

        for (BasicBlock endBlock : endBlocks) {
            List<BasicBlock> path = new ArrayList<>();
            List<BasicBlock> visited = new ArrayList<>();
            
            path.add(endBlock);
            visited.add(endBlock);
            getReversePath(endBlock, path, visited);
        }
    }
    
    private void getReversePath(BasicBlock basicBlock, List<BasicBlock> path, List<BasicBlock> visited) {
        List<BasicBlock> parentBlocks = basicBlock.getParent();

        for (BasicBlock parent : parentBlocks) {
            List<BasicBlock> path_copy = new ArrayList<>(path);
            List<BasicBlock> visited_copy = new ArrayList<>(visited);

            if (visited.contains(parent)) {
                path_copy.add(parent);
                globalPaths.add(path_copy);
                continue;
            }

            path_copy.add(parent);
            visited_copy.add(parent);
            getReversePath(parent, path_copy, visited_copy);
        }

        if (parentBlocks.isEmpty()) {
            globalPaths.add(path);
        }
    }
    
    /* should only be called after getReversePath */
    private void getLiveRanges() {
        // get live ranges for each path, and then merge them
        for (List<BasicBlock> path : globalPaths) {
            HashMap< Integer, Set<Integer> > localLiveRanges = getLiveRanges(path);
            mergeWithGlobal(localLiveRanges);
        }
    }

    /* should only be called after getReversePath */
    private HashMap< Integer, Set<Integer> > getLiveRanges(List<BasicBlock> path) {
        Set<Integer> liveSet = new HashSet<>();
        HashMap< Integer, Set<Integer> > liveSetMap = new HashMap<>();
        
        // for each basic block along a path, traverse from bottom to top
        for (BasicBlock block : path) {
            Instruction currInst = block.getLastInst();

            while (currInst != null && currInst != block.getFirstInst().getPrevious()) {
                Result operand1 = currInst.getOperand1();
                Result operand2 = currInst.getOperand2();

                Opcode opcode = currInst.getOpcode();
                if (opcode == Opcode.BRA) {
                    // do nothing
                } else if (opcode == Opcode.BEQ || opcode == Opcode.BGE
                        || opcode == Opcode.BGT || opcode == Opcode.BLE
                        || opcode == Opcode.BLT || opcode == Opcode.BNE) {
                    addOperand(operand1, liveSet);
                } else if (opcode == Opcode.PHI) {
                    Integer oneside_instNum = checkPhi(path, currInst);
                    if (oneside_instNum != null) {
                        liveSet.add(oneside_instNum);
                    }
                } else if (opcode == Opcode.MOVE) {
                    addOperand(operand1, liveSet);
                    addOperand(operand2, liveSet);
                } else {
                    addOperand(operand1, liveSet);
                    addOperand(operand2, liveSet);
                }

                /* if opcode is move, add the version of liveSet before removing the current instNum
                   to liveSetMap */
                Set<Integer> liveSet_copy = null;
                if (opcode == Opcode.MOVE) {
                    liveSet_copy = new HashSet<>(liveSet);
                    liveSet.remove(currInst.getInstNumber());
                } else {
                    liveSet.remove(currInst.getInstNumber());
                    liveSet_copy = new HashSet<>(liveSet);
                }
                
                liveSetMap.put(currInst.getInstNumber(), liveSet_copy);

                currInst = currInst.getPrevious();
            }
        }
        
        if (!liveSet.isEmpty()) {
            // System.out.println("liveSet not empty!");
        }
        
        return liveSetMap;
    }

    /* @ localLiveRanges: the live ranges along one path */
    private void mergeWithGlobal( HashMap< Integer, Set<Integer> > localLiveRanges ) {
        if (globalLiveRanges.isEmpty()) {
            globalLiveRanges = localLiveRanges;
            return;
        }
        
        for (Integer key : localLiveRanges.keySet()) {
            Set<Integer> localLiveRange = localLiveRanges.get(key);
            
            if (globalLiveRanges.containsKey(key)) {
                Set<Integer> globalLiveRange = globalLiveRanges.get(key);
                
                // do union
                Set<Integer> union = new HashSet<>(globalLiveRange);
                union.addAll(localLiveRange);
                
                globalLiveRanges.replace(key, union);
            } else {
                Set<Integer> union = new HashSet<>(localLiveRange);
                globalLiveRanges.put(key, union);
            }
        }
    }
    
    /* @ inst: a phi instruction
     * this function checks which operand of the phi instruction is defined along 
     * the given path
     */
    private Integer checkPhi(List<BasicBlock> path, Instruction inst) {
        List<Instruction> instList = new ArrayList<>();
        
        Result operand1 = inst.getOperand1();
        Result operand2 = inst.getOperand2();
        
        if (operand1 == null || operand2 == null)
            return null;
        
        Integer op1_instNum = operand1.getInstNumber();
        Integer op2_instNum = operand2.getInstNumber();
        
        // put all instructions along the path in a list
        for (BasicBlock block : path) {
            Instruction currInst = block.getLastInst();
            
            while (currInst != null && currInst != block.getFirstInst().getPrevious()) {
                instList.add(currInst);
                currInst = currInst.getPrevious();
            }
        }
        
        // make sure inst is in the path of instructions
        int indexOfInst = instList.indexOf(inst);
        if (indexOfInst != -1) {
            for (int i = indexOfInst; i < instList.size(); i++) {
                Instruction currInst = instList.get(i);
                
                Integer currInstNum = currInst.getInstNumber();
                if (op1_instNum == currInstNum) {
                    return op1_instNum;
                } else if (op2_instNum == currInstNum) {
                    return op2_instNum;
                }
            }
        }
        
        return null;
    }
    
    /* add an operand to a liveSet; liveSet is modified because java uses pass by references */
    private void addOperand(Result operand, Set<Integer> liveSet) {
        if (operand != null) {
            Result.ResultType opType = operand.getType();
            if (opType == Result.ResultType.INSTRUCTION || opType == Result.ResultType.VARIABLE) {
                liveSet.add(operand.getInstNumber());
            }
        }
    }

    
    public void printPaths() {
        for (List<BasicBlock> path : globalPaths) {
            for (int index = 0; index < path.size() - 1; index++) {
                System.out.print(path.get(index).BBNum + " -> ");
            }

            if (path.size() != 0) {
                int last_index = path.size() - 1;
                System.out.print(path.get(last_index).BBNum);
                System.out.print("\n");
            }
        }
    }
    
    public void printLiveRanges() {
        for (Integer key : globalLiveRanges.keySet()) {
            System.out.print("At instruction " + key + ": ");
            System.out.println( globalLiveRanges.get(key) );
        }
    }
    
    
    /* incorrect
    private void getReversePath(ControlFlowGraph cfg) {
        BasicBlock entryBlock = cfg.getEntryBlock();

        List<BasicBlock> path = new ArrayList<>();
        List<BasicBlock> visited = new ArrayList<>();
        path.add(entryBlock);
        visited.add(entryBlock);

        getReversePath(entryBlock, path, visited);
    }

    // incorrect
    private void getReversePath(BasicBlock basicBlock, List<BasicBlock> path, List<BasicBlock> visited) {
        List<BasicBlock> childrenBlocks = basicBlock.getChildrenBlock();

        for (BasicBlock child : childrenBlocks) {
            List<BasicBlock> path_copy = new ArrayList<>(path);
            List<BasicBlock> visited_copy = new ArrayList<>(visited);

            if (visited.contains(child)) {
                path_copy.add(child);
                Collections.reverse(path_copy);
                globalPaths.add(path_copy);
                continue;
            }

            path_copy.add(child);
            visited_copy.add(child);
            getReversePath(child, path_copy, visited_copy);
        }

        if (childrenBlocks.isEmpty()) {
            Collections.reverse(path);
            globalPaths.add(path);
        }
    }
    */   
}