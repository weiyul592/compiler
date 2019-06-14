/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Parser;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import CFG.ControlFlowGraph;
import CFG.BasicBlock;
import SSA.Instruction;
import SSA.Opcode;
import Lex.Result;
import Lex.Result.ResultType;

/**
 *
 * @author Weiyu, Amir
 */

// defusechain is not maintained after CSE optimization
public class CSEPass {
    List< HashMap<Opcode, List<Instruction> > > instGroups;
    
    // only map inst number to inst number. 
    // create instruction result when replacing operands.
    HashMap<Integer, Integer> instMap;
    
    public CSEPass() {
        instGroups = new ArrayList<>();
        instMap = new HashMap<>();
    }
    
    public void execute(ControlFlowGraph cfg) {
        groupInstructions(cfg);
        //printGroup();
        
        buildInstMap();
//        printInstMap();
        
        replaceOperands(cfg);
    }
    
    private void groupInstructions (ControlFlowGraph cfg) {
        BasicBlock entryBlock = cfg.getEntryBlock();
        HashMap<Opcode, List<Instruction> > instGroup = new HashMap<>();
        
        groupInstructions(entryBlock, instGroup);
    }
    
    // collect instructions with the same opcode along domination paths
    private void groupInstructions(BasicBlock block, HashMap<Opcode, List<Instruction> > instGroup) {
        List<BasicBlock> immeDominations = block.getImmediateDominations();
        HashMap<Opcode, List<Instruction> > instGroup_copy = clone(instGroup);
        
        Instruction currInst = block.getFirstInst();
        
        while (currInst != null && currInst != block.getLastInst().getNext()) {
            Opcode op = currInst.getOpcode();
            
            if ( instGroup_copy.containsKey(op) ) {
                instGroup_copy.get(op).add(currInst);
            } else {
                instGroup_copy.put(op, new ArrayList<>());
                instGroup_copy.get(op).add(currInst);
            }
            
            currInst = currInst.getNext();
        }
        
        for (BasicBlock child_dom : immeDominations) {
            groupInstructions(child_dom, instGroup_copy);
        }
        
        if (immeDominations.isEmpty()) {
            instGroups.add(instGroup_copy);
        }
    }
    
    private void buildInstMap() {
        for (HashMap<Opcode, List<Instruction> > instGroup : instGroups) {
            for (List<Instruction> instList : instGroup.values()) {
                // traverse list looking for instructions with the same operands
                for (int i = 0; i < instList.size(); i++) {
                    // get current instruction
                    Instruction currInst = instList.get(i);
                    
                    // compare it with the rest instructions in the instruction list
                    for (int j = i + 1; j < instList.size(); j++) {
                        Instruction cmpInst = instList.get(j);
                        
                        if ( equal(currInst, cmpInst) ) {
                            // only map inst number to inst number. 
                            // create instruction result when replacing operands.
                            Integer cmpInstNum = cmpInst.getInstNumber();
                            Integer currInstNum = currInst.getInstNumber();
                            instMap.put(cmpInstNum, currInstNum);
                            
                            if (instMap.containsKey(currInstNum)) {
                                Integer finalInstNum = instMap.get(currInstNum);
                                instMap.replace(cmpInstNum, finalInstNum);
                            }
                            
                            // remove the duplicated instruction from its basicblock
                            BasicBlock block = cmpInst.getBBl();
                            block.removeInst(cmpInst);
                        }
                    }
                    
                }
            }
        }
    }
    
    public void replaceOperands(ControlFlowGraph cfg) {
        for (BasicBlock block : cfg.getBasicBlocks()) {
            Instruction currInst = block.getFirstInst();
            
//            System.out.print("\n");
//            System.out.println(block.toStr());
            while (currInst != null && currInst != block.getLastInst().getNext()) {
                Opcode opcode = currInst.getOpcode();
                Result operand1 = currInst.getOperand1();
                Result operand2 = currInst.getOperand2();
                
                // deal with branch instructions;
                if (opcode == Opcode.BRA) {
                    Integer branchTo = operand1.getInstNumber();
                    Instruction originalBranchTo = cfg.getInstruction( branchTo );
                    
                    BasicBlock branchToBlock = originalBranchTo.getBBl();
                    Instruction newBranchTo = branchToBlock.getFirstInst();
                    
                    currInst.setOperand1( Result.InstResult(newBranchTo.getInstNumber()) );
                    
//                    currInst.print();
                    currInst = currInst.getNext();
                    continue;
                } else if (opcode == Opcode.BEQ || opcode == Opcode.BGE ||
                        opcode == Opcode.BGT || opcode == Opcode.BLE ||
                        opcode == Opcode.BLT || opcode == Opcode.BNE ) {
                
                    Integer branchTo = operand2.getInstNumber();
                    Instruction originalBranchTo = cfg.getInstruction( branchTo );
                    
                    BasicBlock branchToBlock = originalBranchTo.getBBl();
                    Instruction newBranchTo = branchToBlock.getFirstInst();
                    
                    currInst.setOperand2( Result.InstResult(newBranchTo.getInstNumber()) );
                    
//                    currInst.print();
                    currInst = currInst.getNext();
                    continue;
                }
                
                if (operand1 != null && operand1.getType() == ResultType.INSTRUCTION) {
                    Integer instNum = operand1.getInstNumber();
                    if (instMap.containsKey( instNum )) {
                        Integer replaceInstNum = instMap.get( instNum );
                        Result replacement = Result.InstResult(replaceInstNum);
                        
                        currInst.setOperand1(replacement);
                    }
                }
                
                if (operand2 != null && operand2.getType() == ResultType.INSTRUCTION) {
                    Integer instNum = operand2.getInstNumber();
                    if (instMap.containsKey( instNum )) {
                        Integer replaceInstNum = instMap.get( instNum );
                        Result replacement = Result.InstResult(replaceInstNum);
                        
                        currInst.setOperand2(replacement);
                    }
                }
//                currInst.print();
                
                currInst = currInst.getNext();
            }
        }
        
    }
    
    
    private HashMap<Opcode, List<Instruction> > clone(HashMap<Opcode, List<Instruction> > original) {
        HashMap<Opcode, List<Instruction> > newMap = new HashMap<>();
        
        for (Opcode key : original.keySet()) {
            List<Instruction> newList = new ArrayList<>( original.get(key) );
            newMap.put(key, newList);
        }
        
        return newMap;
    }
    
    private boolean equal(Instruction A, Instruction B) {
        if (A == null || B == null) {
            return false;
        }
        
        Result operandA1 = A.getOperand1();
        Result operandA2 = A.getOperand2();
        
        Result operandB1 = B.getOperand1();
        Result operandB2 = B.getOperand2();
        
        boolean bool1 = Result.equal(operandA1, operandB1) && Result.equal(operandA2, operandB2);
        boolean bool2 = Result.equal(operandA1, operandB2) && Result.equal(operandA2, operandB1);
        
        return bool1 || bool2;
    }
    
    public void printGroup() {
        for (int i = 0; i < instGroups.size(); i++) {
            System.out.println("path " + i);
            HashMap<Opcode, List<Instruction> > instGroup = instGroups.get(i);
            
            for (Opcode key : instGroup.keySet()) {
                //if (key == Opcode.MOVE) {
                    System.out.println(key);

                    for (Instruction inst : instGroup.get(key)) {
                        inst.print();
                    }
                //}
            }
        }
    }
    
    public void printInstMap() {
        System.out.println(instMap);
    }
}