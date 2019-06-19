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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Weiyu, Amir
 */
public class Liveness2 {
    HashMap<BasicBlock, Set<Integer> > BlockIn;
    HashMap<BasicBlock, Set<Integer> > BlockOut;
    
    public Liveness2() {
        BlockIn = new HashMap<>();
        BlockOut = new HashMap<>();
    }
    
    public void getBlockLiveSet(ControlFlowGraph cfg) {
        Set<BasicBlock> changed = new HashSet<>();
        BasicBlock exitBlock = null;
        // find exit block
        for (BasicBlock block : cfg.getBasicBlocks()) {
            if (block.getChildrenBlock().isEmpty()) {
                exitBlock = block;
                break;
            }
        }
        
        if (exitBlock == null) {
            System.out.println("exit block not found\n");
            return;
        }
        
        // initializtion
        for (BasicBlock block : cfg.getBasicBlocks()) {
            BlockIn.put(block, new HashSet<>());
        }
        
        BlockOut.put(exitBlock, new HashSet<>());
        Set<Integer> exitIn = use(exitBlock);
        exitIn.removeAll( def(exitBlock) );
        BlockIn.put(exitBlock, exitIn );
        
        changed.addAll(cfg.getBasicBlocks());
        changed.remove(exitBlock);
        
        /*
        for (BasicBlock b : changed) {
            System.out.println(b.toStr());
        }
        */
        
        while (!changed.isEmpty()) {
            // pick one block from the set of basic blocks
            BasicBlock currBlock = null;
            for (BasicBlock block : changed) {
                currBlock = block;
                changed.remove(block);
                //System.out.println(currBlock.toStr());
                break;
            }
            
            Set<Integer> currBlockOut = new HashSet<>();
            List<Instruction> phiInsts = new ArrayList<>();
            for (BasicBlock successor : currBlock.getChildrenBlock()) {
                currBlockOut.addAll( BlockIn.get(successor) );
                
                // collect phi instructions for later "partial" removal
                phiInsts.addAll( successor.getPhiInstructions() );
            }
            
            System.out.println(currBlock.toStr() + " has phi: ");
            for (Instruction phi : phiInsts) {
                phi.print();
            }
            
            //System.out.println(currBlockOut);
            
            BlockOut.put(currBlock, currBlockOut);
            
            Set<Integer> currBlockIn = new HashSet(currBlockOut);
            //currBlockIn.removeAll( def(currBlock) );
            //currBlockIn.addAll( use(currBlock) );
            currBlockIn.addAll( use(currBlock) );
            currBlockIn.removeAll( def(currBlock) );
            
            Set<Integer> oldIn = BlockIn.get(currBlock);
            BlockIn.put(currBlock, currBlockIn);
            
            //System.out.println("old in: " + oldIn);
            //System.out.println("new in: " + currBlockIn);
            
            if ( !setContentEqual(oldIn, currBlockIn) ) {
                // System.out.println("old in not equal to new in");
                for (BasicBlock predecessor : currBlock.getParent()) {
                    changed.add(predecessor);
                }
            } else {
                // System.out.println("equals");
            }
        }
    }
    
    public Set<Integer> def(Instruction inst) {
        Set<Integer> defSet = new HashSet<>();
        
        Opcode opcode = inst.getOpcode();

        /*
            ADD, SUB, MUL, DIV,
            CMP, ADDA, LOAD,
            MOVE, PHI,
            READ, 
            CALL?
        */
        
        switch(opcode) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case CMP:
            case ADDA:
            case LOAD:
            case PHI:
            case MOVE:
                defSet.add( inst.getInstNumber() );
                break;
            case CALL: System.out.println("function call detected\n"); 
                break;
            case READ: System.out.println("read detected\n"); 
                break;
            default: break; // do nothing
        }
        
        return defSet;
    }
    
    public Set<Integer> def(BasicBlock block) {
        Set<Integer> defSet = new HashSet<>();
        Instruction currInst = block.getFirstInst();
        
        while (currInst != null && currInst != block.getLastInst().getNext()) {
            Set<Integer> defOfCurrInst = def(currInst);
            defSet.addAll( defOfCurrInst );
                
            currInst = currInst.getNext();
        }
        
        return defSet;
    }
    
    /* return the uses of an instruction */
    public Set<Integer> use(Instruction inst) {
        Set<Integer> useSet = new HashSet<>();
        
        Result operand1 = inst.getOperand1();
        Result operand2 = inst.getOperand2();

        Opcode opcode = inst.getOpcode();
        
        if (opcode == Opcode.BRA) {
            // do nothing
        } else if (opcode == Opcode.BEQ || opcode == Opcode.BGE
                || opcode == Opcode.BGT || opcode == Opcode.BLE
                || opcode == Opcode.BLT || opcode == Opcode.BNE) {
            addOperand(operand1, useSet);
        } else if (opcode == Opcode.MOVE) {
            // Move a_3 b_4 is equivalent to b_4 = a_3
            addOperand(operand1, useSet);
        } else {
            addOperand(operand1, useSet);
            addOperand(operand2, useSet);
        }
        
        return useSet;
    }
    
    /* return the uses of a basic block */
    public Set<Integer> use(BasicBlock block) {
        Set<Integer> useSet = new HashSet<>();
        Instruction currInst = block.getFirstInst();
        
        while (currInst != null && currInst != block.getLastInst().getNext()) {
            Set<Integer> useOfCurrInst = use(currInst);
            useSet.addAll( useOfCurrInst );
                
            currInst = currInst.getNext();
        }
        
        return useSet;
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

    private boolean setContentEqual(Set<Integer> first, Set<Integer> second) {
        if (first == null || second == null) {
            System.out.println("One set or both sets are null");
            return false;
        }
        
        if (first.size() != second.size())
            return false;
        
        Set<Integer> localFirst = new HashSet<>(first);
        localFirst.removeAll(second);
        
        /* Given that two sets are of the same size, if first - second == empty,
           then they are equal. */
        if (localFirst.size() == 0)
            return true;
        
        return false;
    }
    
    public void printBlockInOut() {
        for (BasicBlock key : BlockIn.keySet()) {
            System.out.println(key.toStr());
            System.out.println("In: " + BlockIn.get(key));
            System.out.println("Out: " + BlockOut.get(key));
        }
    }
    
    public void print(ControlFlowGraph cfg) {
        Set<Integer> useSet = new HashSet<>();
        Set<Integer> defSet = new HashSet<>();
        for (BasicBlock block : cfg.getBasicBlocks()) {
            useSet = use(block);
            defSet = def(block);
            
            System.out.println(block.toStr() + " has uses: ");
            System.out.println(useSet);
            System.out.println("    and defs: ");
            System.out.println(defSet);
        }
    }
    
}
