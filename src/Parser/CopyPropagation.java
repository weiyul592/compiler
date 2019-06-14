/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Parser;

import java.util.HashMap;

import CFG.BasicBlock;
import CFG.ControlFlowGraph;
import SSA.Instruction;
import SSA.Opcode;
import SSA.DefUseChain;
import Lex.Result;
import Lex.Result.ResultType;
/**
 *
 * @author Weiyu, Amir
 */
public class CopyPropagation {
    // store the list of copy propagations
    private HashMap<Result, Result> copyMap;
    
    public CopyPropagation() {
        copyMap = new HashMap();
    }
    
    public void execute(ControlFlowGraph cfg) {
        collectCopy(cfg);
        replaceOperands(cfg);
        //viewResult(cfg);
        
        //DefUseChain.getInstance().print();
    }
    
    private void collectCopy(ControlFlowGraph cfg) {
        DefUseChain.getInstance().clear();
        
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            Instruction currInst = basicBlock.getFirstInst();

            while (currInst != null && currInst != basicBlock.getLastInst().getNext()) {
                Opcode opcode = currInst.getOpcode();
                Result op1 = currInst.getOperand1();
                Result op2 = currInst.getOperand2();

                if (op1 == null || op2 == null) {
                    currInst = currInst.getNext();
                    continue;
                } else if (opcode == Opcode.MOVE /*&& 
                        op1.getType() != ResultType.CONSTANT &&
                        op2.getType() != ResultType.CONSTANT*/) {
                    copyMap.put(op2, op1);

                    if ( copyMap.containsKey(op1) ) {
                        Result end = copyMap.get(op1);
                        copyMap.replace(op2, end);
                    }

                    //System.out.print( op2.getName() + op2.getInstNumber() + ":-- " + copyMap.get(op2).getName() + "\n" );
                    //System.out.print( op2 + ":-> " + copyMap.get(op2) + "\n" );

                    basicBlock.removeInst(currInst);
                }

                currInst = currInst.getNext();
            }
        }
    }
    
    private void replaceOperands(ControlFlowGraph cfg) {
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            if (basicBlock == null)
                return;
            
            Instruction currInst = basicBlock.getFirstInst();
            while (currInst != null && currInst != basicBlock.getLastInst().getNext()) {
                Opcode opcode = currInst.getOpcode();
                Result op1 = currInst.getOperand1();
                Result op2 = currInst.getOperand2();

                if (opcode == Opcode.MOVE && op2.getType() == ResultType.VARIABLE) {
                    currInst = currInst.getNext();
                    continue;
                }
                
                // replace both operands if needed
                if (copyMap.containsKey(op1)) {
                    Result replacement = copyMap.get(op1);
                    currInst.setOperand1( replacement );
                }
                
                if (copyMap.containsKey(op2)) {
                    Result replacement = copyMap.get(op2);
                    currInst.setOperand2( replacement );
                }

                // update def use chains
                DefUseChain.getInstance().addUse(currInst.getOperand1(), currInst);
                DefUseChain.getInstance().addUse(currInst.getOperand2(), currInst);
                
                currInst = currInst.getNext();
            }
        }
    }
    
    public void viewResult(ControlFlowGraph cfg) {
        BasicBlock entryBlock = cfg.getEntryBlock();
        viewResult(entryBlock);
    }
    
    public void viewResult(BasicBlock block) {
        System.out.println( block.toStr() );
        block.print();
        System.out.print("\n");
        
        for (BasicBlock child : block.getImmediateDominations()) {
            viewResult(child);
        }
    }
}