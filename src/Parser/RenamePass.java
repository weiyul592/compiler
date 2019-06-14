/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Parser;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import Lex.Result;
import Lex.Result.ResultType;
import CFG.ControlFlowGraph;
import CFG.BasicBlock;
import SSA.Instruction;
import SSA.Opcode;

/**
 *
 * @author Weiyu, Amir
 */

// defusechain is not maintained
public class RenamePass {
    private HashMap< Integer, HashMap<String, Result> > valueTables;
    
    public RenamePass () {
        valueTables = new HashMap<>();
    }
    
    public void renameOperands(ControlFlowGraph cfg) {
        BasicBlock entryBlock = cfg.getEntryBlock();
        
        HashMap<String, Result> table = new HashMap<>();
        List<BasicBlock> visited = new ArrayList<>();
        visited.add(entryBlock);
        
        renameBasedOnPhi(entryBlock, table, visited);
        renamePhi(entryBlock, visited);
    }
    
    /* this function collects value table for each basic block and rename
     * operands of non-phi instructions
     */
    private void renameBasedOnPhi(BasicBlock basicBlock, HashMap<String, Result>table, List<BasicBlock>visited ) {
        List<BasicBlock> childrenBlocks = basicBlock.getChildrenBlock();
        HashMap<String, Result> table_copy = new HashMap<>(table);
        
        Instruction currInst = basicBlock.getFirstInst();
        
        while ( currInst != null && currInst != basicBlock.getLastInst().getNext() ) {
            Result operand1 = currInst.getOperand1();
            Result operand2 = currInst.getOperand2();
            
            // add new definitions to table
            if (currInst.getOpcode() == Opcode.MOVE) {
                if (operand2 != null && operand2.getType() == ResultType.VARIABLE) {
                    String name = operand2.getName();
                    table_copy.put(name, operand2);
                }
            }
            
            if (currInst.getOpcode() == Opcode.PHI) {
                String name = currInst.affectedVariable;
                
                Result newValue = Result.VarResult(name);
                newValue.setInstNumber( currInst.getInstNumber() );
                table_copy.put(name, newValue);
            }
            
            // replace operands; for any opcode
            if (operand1 != null && table_copy.containsKey(operand1.getName())) {
                Result replace = table_copy.get(operand1.getName());
                currInst.setOperand1(replace);
            }
            
            if (operand2 != null && table_copy.containsKey(operand2.getName())) {
                Result replace = table_copy.get(operand2.getName());
                currInst.setOperand2(replace);
            }
            
            currInst = currInst.getNext();
        }
        
        // recursively call this function on its children
        for ( BasicBlock child : childrenBlocks) {
            List<BasicBlock> visited_copy = new ArrayList<>(visited);
            if (visited.contains(child)) {
                continue;
            }
            
            visited_copy.add(child);
            renameBasedOnPhi(child, table_copy, visited_copy);
        }
        
        valueTables.put(basicBlock.BBNum, table_copy);
    }
    
    /* 
     * must be called after renameBasedOnPhi
     */
    private void renamePhi(BasicBlock basicBlock, List<BasicBlock> visited) {
        List<BasicBlock> childrenBlocks = basicBlock.getChildrenBlock();
        
        Instruction currInst = basicBlock.getFirstInst();
        
        while ( currInst != null && currInst != basicBlock.getLastInst().getNext() ) {
            if (currInst.getOpcode() == Opcode.PHI) {
                String name = currInst.affectedVariable;
                List<Result> results = new ArrayList<>();
                
                for ( BasicBlock parent : basicBlock.getParent() ) {
                    Integer blockNum = parent.BBNum;
                    
                    HashMap<String, Result> table = valueTables.get(blockNum);
                    results.add( table.get(name) );
                }
                
                if (results.size() == 0) {
                    continue;
                } else if (results.size() == 1) {
                    currInst.setOperand1( results.get(0) );
                } else if (results.size() == 2) {
                    currInst.setOperand1( results.get(0) );
                    currInst.setOperand2( results.get(1) );
                } else {
                    System.out.println("Woops, this block has more than two parents");
                }
            }
            
            currInst = currInst.getNext();
        }
        
        // recursively call this function on its children
        for (BasicBlock child : childrenBlocks) {
            List<BasicBlock> visited_copy = new ArrayList<>(visited);
            if (visited.contains(child)) {
                continue;
            }
            
            visited_copy.add(child);
            renamePhi(child, visited_copy);
        }
    }
    
    public void print() {
        for (Integer B : valueTables.keySet()) {
            System.out.print("BB " + B + ": ");
            
            HashMap<String, Result> table = valueTables.get(B);
            for (Result r : table.values()) {
                System.out.print(r.getName() + "_" + r.getInstNumber() + " ");
            }
            System.out.print("\n");
        }
    }
}
