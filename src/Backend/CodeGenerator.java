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
    HashMap<Integer, Integer> registers;
    
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
        loadRegisters(currScope);
        
        generateMachineCode();
        //printMachineCodes();
        
        stopProcessing();
    }
    
    private void initialize() {
        Integer globalSize = MemoryAllocator.getInstance().getGlobalCounter();
        programs.add(DLX.assemble(DLX.ADDI, FP, 30, globalSize * -4));
        programs.add(DLX.assemble(DLX.ADD, SP, FP, 0));
    }   
    
    private void stopProcessing() {
        programs.add(DLX.assemble(DLX.RET, 0));
    }
    
    private void generateControlFlow(ControlFlowGraph cfg) {
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
    
    private void addInstructions(BasicBlock block) {
        Instruction currInst = block.getFirstInst();
        while (currInst != null && currInst != block.getLastInst().getNext()) {
            instructions.add(currInst);
            currInst = currInst.getNext();
        }
    }
    
    private void generateMachineCode() {
        for (int i = 0; i < instructions.size(); i++) {
            //if (i == 14) break;
            
            Instruction currInst = instructions.get(i);
            Opcode opcode = currInst.getOpcode();
            Result operand1 = currInst.getOperand1();
            Result operand2 = currInst.getOperand2();
            Integer MachineCode = null;
            Integer R1, R2, destReg;
            
            switch (opcode) {
                case ADD:
                    MachineCode = generateMathCode(currInst, DLX.ADD);
                    break;
                case ADDA:
                    MachineCode = generateMathCode(currInst, DLX.ADD);
                    break;
                case SUB:
                    MachineCode = generateMathCode(currInst, DLX.SUB);
                    break;
                case MUL:
                    MachineCode = generateMathCode(currInst, DLX.MUL);
                    break;
                case DIV:
                    MachineCode = generateMathCode(currInst, DLX.DIV);
                    break;
                case STORE:
                    if (operand1.getType() == ResultType.CONSTANT) {
                        programs.add( DLX.assemble(DLX.ADDI, RP1, 0, operand1.getConstValue()) );
                        R2 = getRegister(operand2.getInstNumber());
                        MachineCode = DLX.assemble(DLX.STW, RP1, R2, 0);
                    } else if (operand1.getType() == ResultType.INSTRUCTION) {
                        R1 = getRegister(operand1.getInstNumber());
                        R2 = getRegister(operand2.getInstNumber());
                        MachineCode = DLX.assemble(DLX.STW, R1, R2, 0);
                    }
                    
                    break;
                case LOAD:
                    destReg = getRegister(currInst.getInstNumber());
                    R1 = getRegister(operand1.getInstNumber());
                    MachineCode = DLX.assemble(DLX.LDW, destReg, R1, 0);
                    break;
                case WRITE:
                    R1 = getRegister(operand1.getInstNumber());
                    MachineCode = DLX.assemble(DLX.WRD, R1);
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
    
    private Integer generateMathCode(Instruction inst, int opcode) {
        Result operand1 = inst.getOperand1();
        Result operand2 = inst.getOperand2();
        ResultType op1_type = operand1.getType();
        ResultType op2_type = operand2.getType();
        
        Integer destReg = getRegister( inst.getInstNumber() );
        
        if (op1_type == ResultType.ADDRESS) {
            // get address of global variables
            if (op2_type == ResultType.CONSTANT) {
                return DLX.assemble(opcode + 16, destReg, DF, operand2.getConstValue());
            } else {
                System.out.println("Add DF nonconstant");
            }
        } else if ((op1_type == ResultType.VARIABLE || op1_type == ResultType.INSTRUCTION)
                && op2_type == ResultType.CONSTANT) {
            Integer R1 = getRegister( operand1.getInstNumber() );
            return DLX.assemble(opcode + 16, destReg, R1, operand2.getConstValue());
        } else if (op1_type == ResultType.CONSTANT && 
                (op2_type == ResultType.VARIABLE || op2_type == ResultType.INSTRUCTION)) {
            
            Integer R2 = getRegister( operand2.getInstNumber() );
            if (opcode == DLX.ADD || opcode == DLX.MUL) {
                /* Add and Mul are commutative */
                return DLX.assemble(opcode + 16, destReg, R2, operand1.getConstValue());
            } else if (opcode == DLX.SUB) {
                programs.add(DLX.assemble(opcode + 16, destReg, R2, operand1.getConstValue()));
                return DLX.assemble(DLX.SUB, destReg, 0, destReg);
            } else if (opcode == DLX.DIV) {
                System.out.println("DIV num VAR");
                return null;
            }
        } else {
            Integer R1 = getRegister( operand1.getInstNumber() );
            Integer R2 = getRegister( operand2.getInstNumber() );
            return DLX.assemble(opcode, destReg, R1, R2);
        }
        
        return null;
    }
    
    public void execute() throws IOException {
        DLX.load( convertIntegers(programs) );
        DLX.execute();
    }
    
    private void loadRegisters(ControlFlowGraph scope) {
        registers = scope.getRegisters();
    }
    
    private Integer getRegister(Integer key) {
        if (registers.containsKey(key)) {
            return registers.get(key) + 1;
        }
        
        return null;
    }
    
    public void printInsts() {
        for (Instruction inst : instructions) {
            inst.print();
        }
    }
    
    public void printMachineCodes() {
        for (Integer code : programs) {
            System.out.print(DLX.disassemble(code));
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
