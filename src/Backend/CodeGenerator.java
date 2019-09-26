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
        
        /*
        programs.add(DLX.assemble(DLX.ADDI, 1, DF, -16));
        programs.add(DLX.assemble(DLX.ADDI, RP1, 0, 51));
        programs.add(DLX.assemble(DLX.STW, RP1, 1, 0));
        programs.add(DLX.assemble(DLX.LDW, 1, 1, 0));
        programs.add(DLX.assemble(DLX.MULI, 1, 1, 2));
        
        programs.add(DLX.assemble(DLX.ADDI, 2, DF, -20));
        programs.add(DLX.assemble(DLX.STW, 1, 2, 0));
        programs.add(DLX.assemble(DLX.LDW, 1, 2, 0));
        programs.add(DLX.assemble(DLX.WRD, 1));
        */
        
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
            if (i == 3) break;
            
            Instruction currInst = instructions.get(i);
            Opcode opcode = currInst.getOpcode();
            Result operand1 = currInst.getOperand1();
            Result operand2 = currInst.getOperand2();
            HashMap<Integer, Integer> registers = currScope.getRegisters();
            Integer MachineCode = null;
            
            switch (opcode) {
                case ADD:
                    MachineCode = generateMathCode(currInst, DLX.ADD);
                    break;
                case ADDA:
                case SUB:
                case MUL:
                case DIV:
                    System.out.println("math op not implemented");
                    break;
                case STORE:
                    if (operand1.getType() == ResultType.CONSTANT) {
                        programs.add( DLX.assemble(DLX.ADDI, RP1, 0, operand1.getConstValue()) );
                        Integer R1 = registers.get(operand2.getInstNumber());
                        MachineCode = DLX.assemble(DLX.STW, RP1, R1, 0);
                    } else if (operand1.getType() == ResultType.INSTRUCTION) {
                        
                    }
                    
                    break;
                case LOAD:
                    Integer destReg = registers.get(currInst.getInstNumber());
                    Integer R1 = registers.get(operand1.getInstNumber());
                    MachineCode = DLX.assemble(DLX.LDW, destReg, R1, 0);
                    break;
                default:
                    break;
            }
            
            if (MachineCode != null) {
                programs.add(MachineCode);
            } else {
                System.out.println("Woops: " + opcode);
            }
        }
    }
    
    public Integer generateMathCode(Instruction inst, int opcode) {
        Result operand1 = inst.getOperand1();
        Result operand2 = inst.getOperand2();
        ResultType op1_type = operand1.getType();
        ResultType op2_type = operand2.getType();
        HashMap<Integer, Integer> registers = currScope.getRegisters();
        
        // colors are assigned from 0
        Integer destReg = registers.get( inst.getInstNumber() ) + 1;
        
        if (op1_type == ResultType.ADDRESS) {
            // get address of global variables
            if (op2_type == ResultType.CONSTANT) {
                System.out.println(destReg);
                return DLX.assemble(opcode + 16, destReg, DF, operand2.getConstValue());
            } else {
                System.out.println("Add DF nonconstant");
            }
        } else if (op1_type == ResultType.VARIABLE && op2_type == ResultType.CONSTANT) {
            Integer R1 = registers.get( operand1.getInstNumber() );
            return DLX.assemble(opcode, destReg, R1, operand2.getConstValue());
        } else if (op1_type == ResultType.CONSTANT && op2_type == ResultType.VARIABLE) {
            System.out.println("ADD CONST VAR");
        }
        
        return null;
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