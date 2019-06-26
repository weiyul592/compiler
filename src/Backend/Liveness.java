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
public class Liveness {
    HashMap<BasicBlock, Set<Integer> > BlockLiveIn;
    HashMap<BasicBlock, Set<Integer> > BlockLiveOut;
    HashMap<Instruction, Set<Integer> > InstLiveOut;
    
    public Liveness() {
        BlockLiveIn = new HashMap<>();
        BlockLiveOut = new HashMap<>();
        // compute live out for each instruction is enough
        InstLiveOut = new HashMap<>();
    }
    
    public HashMap<Instruction, Set<Integer> > computeLiveSets(ControlFlowGraph cfg) {
        getBlockLiveSet(cfg);
        getInstructionLiveSet(cfg);
        return InstLiveOut;
    }
    
    /* Compute live in and live out for each basic block */
    private void getBlockLiveSet(ControlFlowGraph cfg) {
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
            BlockLiveIn.put(block, new HashSet<>());
        }
        
        BlockLiveOut.put(exitBlock, new HashSet<>());
        Set<Integer> exitIn = use(exitBlock);
        exitIn.removeAll( def(exitBlock) );
        BlockLiveIn.put(exitBlock, exitIn );
        
        changed.addAll(cfg.getBasicBlocks());
        changed.remove(exitBlock);
        
        while (!changed.isEmpty()) {
            // pick one block from the set of basic blocks
            BasicBlock currBlock = null;
            for (BasicBlock block : changed) {
                currBlock = block;
                changed.remove(block);
                break;
            }
            
            Set<Integer> currBlockLiveOut = new HashSet<>();
            List<Instruction> phiInsts = new ArrayList<>();
            for (BasicBlock successor : currBlock.getChildrenBlock()) {
                currBlockLiveOut.addAll( BlockLiveIn.get(successor) );
                
                // collect phi instructions for later "partial" removal
                phiInsts.addAll( successor.getPhiInstructions() );
            }
            
            for (Instruction phi : phiInsts) {
                Integer notDefinedInThisPath = checkPhi(currBlock, phi);
                currBlockLiveOut.remove(notDefinedInThisPath);
            }
            
            BlockLiveOut.put(currBlock, currBlockLiveOut);
            
            Set<Integer> currBlockLiveIn = new HashSet(currBlockLiveOut);
            currBlockLiveIn.addAll( use(currBlock) );
            currBlockLiveIn.removeAll( def(currBlock) );
            
            Set<Integer> oldIn = BlockLiveIn.get(currBlock);
            BlockLiveIn.put(currBlock, currBlockLiveIn);
            
            if ( !setContentEqual(oldIn, currBlockLiveIn) ) {
                // System.out.println("old in not equal to new in");
                for (BasicBlock predecessor : currBlock.getParent()) {
                    changed.add(predecessor);
                }
            } 
        }
    }
    
    /* Compute live in and live out for each instruction */
    private void getInstructionLiveSet(ControlFlowGraph cfg) {
        Set<Integer> currBlockLiveIn = new HashSet<>();
        Set<Integer> currBlockLiveOut = new HashSet<>();
                
        for (BasicBlock block : cfg.getBasicBlocks()) {
            currBlockLiveIn = BlockLiveIn.get(block);
            currBlockLiveOut = BlockLiveOut.get(block);
            
            Instruction lastInst = block.getLastInst();
            if (lastInst != null) {
                InstLiveOut.put(lastInst, currBlockLiveOut);
                //lastInst.print();
                //System.out.println(currBlockLiveOut);
            } 
            
            Instruction currInst = lastInst;
            while ( currInst != null && currInst != block.getFirstInst().getPrevious() ) {
                if (currInst == lastInst) {
                    currInst = currInst.getPrevious();
                    continue;
                }
                            
                Instruction nextInst = currInst.getNext();
                Set<Integer> nextInstLiveOut = InstLiveOut.get(nextInst);
                Set<Integer> currInstLiveOut = new HashSet<>(nextInstLiveOut);
                currInstLiveOut.addAll( use(nextInst) );
                currInstLiveOut.removeAll( def(nextInst) );
                
                InstLiveOut.put(currInst, currInstLiveOut);
                
                currInst = currInst.getPrevious();
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
    
    /* This function checks which operand of phiInst is defined in this basic block 
       and the path above.  The return value is the instruction number of the 
       operand not defined in this path.
    */
    private Integer checkPhi(BasicBlock block, Instruction phiInst) {
        BasicBlock currBlock = block;
        Instruction currInst;
        Result op1 = phiInst.getOperand1();
        Result op2 = phiInst.getOperand2();
        Integer op1InstNum = null, op2InstNum = null;
        
        if (op1 != null) 
            op1InstNum = op1.getInstNumber();
        if (op2 != null) 
            op2InstNum = op2.getInstNumber();
        
        while (block != null) {
            currInst = block.getLastInst();
            
            while ( currInst != null && currInst != block.getFirstInst().getPrevious() ) {
                if (currInst.getInstNumber() == op1InstNum ) // op1 is defined in this path
                    return op2InstNum;
                else if (currInst.getInstNumber() == op2InstNum)
                    return op1InstNum;
                
                currInst = currInst.getPrevious();
            }
            
            block = block.getImmeDominator();
        }
        
        return null;
    }
    
    public void printBlockInOut() {
        for (BasicBlock key : BlockLiveIn.keySet()) {
            System.out.println(key.toStr());
            System.out.println("In: " + BlockLiveIn.get(key));
            System.out.println("Out: " + BlockLiveOut.get(key));
        }
    }
    
    public void printInstOut() {
        for (Instruction key : InstLiveOut.keySet()) {
            key.print();
            System.out.println("Out: " + InstLiveOut.get(key));
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
