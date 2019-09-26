/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import CFG.BasicBlock;
import CFG.ControlFlowGraph;
import Lex.Result;
import Lex.Result.ResultType;
import SSA.Instruction;
import SSA.MemoryAllocator;
import SSA.Opcode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author Weiyu, Amir
 */
public class CodeGenerator {
    ControlFlowGraph mainScope;
    ControlFlowGraph currScope;
    //List<ControlFlowGraph> functions;
    List<Integer> programs;
    List<Instruction> instructions;
    
    static final int DF = 30;
    static final int FP = 28;
    static final int SP = 29;
    static final int RP1 = 9;
    static final int RP2 = 10;
    
    public CodeGenerator() {
        mainScope = ControlFlowGraph.getMain();
        programs = new ArrayList<>();
        instructions = new ArrayList<>();
    }
    
    public void generate() throws IOException {
        initialize();
        generateControlFlow(mainScope);
        currScope = mainScope;
        
        printInsts();
        generateMachineCode();
        
        programs.add(DLX.assemble(DLX.ADDI, 1, DF, -16));
        programs.add(DLX.assemble(DLX.ADDI, RP1, 0, 51));
        programs.add(DLX.assemble(DLX.STW, RP1, 1, 0));
        programs.add(DLX.assemble(DLX.LDW, 1, 1, 0));
        programs.add(DLX.assemble(DLX.MULI, 1, 1, 2));
        
        programs.add(DLX.assemble(DLX.ADDI, 2, DF, -20));
        programs.add(DLX.assemble(DLX.STW, 1, 2, 0));
        programs.add(DLX.assemble(DLX.LDW, 1, 2, 0));
        programs.add(DLX.assemble(DLX.WRD, 1));
        
        programs.add(DLX.assemble(DLX.RET, 0));
    }
    
    public void initialize() {
        Integer globalSize = MemoryAllocator.getInstance().getGlobalCounter();
        programs.add(DLX.assemble(DLX.ADDI, FP, 30, globalSize * -4));
        programs.add(DLX.assemble(DLX.ADD, SP, FP, 0));
    }   
    
    public void generateControlFlow(ControlFlowGraph cfg) {
        BasicBlock entryBlock = cfg.getEntryBlock();
        Set<BasicBlock> visited = new HashSet<>();
        
        Stack<BasicBlock> stack = new Stack<>();
        stack.push(entryBlock);
        
        while (!stack.empty()) {
            BasicBlock currBlock = stack.pop();
            if (visited.contains(currBlock)) {
                continue;
            } else {
                visited.add(currBlock);
            }
            
            for (BasicBlock child : currBlock.getChildrenBlock()) {
                stack.add(child);
            }
            
            addInstructions(currBlock);
        }
    }
    
    public void addInstructions(BasicBlock block) {
        Instruction currInst = block.getFirstInst();
        while (currInst != null && currInst != block.getLastInst().getNext()) {
            instructions.add(currInst);
            currInst = currInst.getNext();
        }
    }
    
    public void generateMachineCode() {
        for (int i = 0; i < instructions.size(); i++) {
            Instruction currInst = instructions.get(i);
            Opcode opcode = currInst.getOpcode();
            
            if (opcode == Opcode.ADD) {
                generateMathCode(currInst, DLX.ADD);
            }
        }
    }
    
    public void generateMathCode(Instruction inst, int opcode) {
        Result operand1 = inst.getOperand1();
        Result operand2 = inst.getOperand2();
        HashMap<Integer, Integer> registers = currScope.getRegisters();
        
        if (operand1.getType() == ResultType.ADDRESS) {
            // get address of global variables
            if (operand2.getType() == ResultType.CONSTANT) {
                Integer destReg = registers.get( inst.getInstNumber() );
                programs.add(DLX.assemble(opcode, destReg, DF, operand2.getConstValue()));
            } else {
                System.out.println("Add DF nonconstant");
            }
        }
    }
    
    public void execute() throws IOException {
        DLX.load( convertIntegers(programs) );
        DLX.execute();
    }
    
    public void printInsts() {
        for (Instruction inst : instructions) {
            inst.print();
        }
    }
    
    public int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i);
        }
        
        return ret;
    }
}