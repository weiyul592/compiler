/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SSA;

import Lex.Result;
import Lex.Result.ResultType;
import java.util.ArrayList;
import java.util.List;

import CFG.BasicBlock;
import CFG.ControlFlowGraph;
import SSA.DefUseChain;
/**
 *
 * @author Weiyu, Amir
 */
public class Instruction {
    private int InstNumber;
    private Opcode opcode;
    private Result operand1;
    private Result operand2;
    private BasicBlock BBl;
    private Instruction next;
    private Instruction previous;
    public Integer PHIBefValueListSize;
    public String affectedVariable;
    public List<Result> parameters;
    public List<Instruction> BranchDestinationsList;
    
    public Instruction(BasicBlock BBl, Opcode opcode, Result operand1, Result operand2) {
        ControlFlowGraph cfg = BBl.getCFG();
        this.InstNumber = cfg.getInstCounter();
        cfg.incrInstCounter();
        
        this.opcode = opcode;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.BBl = BBl;
        this.BranchDestinationsList = new ArrayList<>();
        BBl.AddInst(this);
        cfg.addInstruction(this);
    }

    public static Instruction addNewInst(BasicBlock bbl, Opcode opcode, Result operand1, Result operand2) {
        Instruction instruction;
        if (bbl.hasEmptyInst()) {
            instruction = bbl.getFirstInst();
            //instruction.setInstNumber(InstCounter++);
            instruction.setOpcode(opcode);
            instruction.setOperand1(operand1);
            instruction.setOperand2(operand2);
        } else {
            instruction = new Instruction(bbl, opcode, operand1, operand2);
        }

        // add operands (variables, immediate results) to def use chains
        DefUseChain.getInstance().addUse(operand1, instruction);
        DefUseChain.getInstance().addUse(operand2, instruction);
        
        return instruction;
    }

    public static Instruction addNewInst(Opcode opcode, Result operand1, Result operand2) {
        Instruction instruction;
        BasicBlock bbl = BasicBlock.getCurrent();
        
        if (bbl.hasEmptyInst()) {
            instruction = bbl.getFirstInst();
            //instruction.setInstNumber(InstCounter++);
            instruction.setOpcode(opcode);
            instruction.setOperand1(operand1);
            instruction.setOperand2(operand2);
        } else {
            instruction = new Instruction(bbl, opcode, operand1, operand2);
        }
        
        // add operands (variables, immediate results) to def use chains
        DefUseChain.getInstance().addUse(operand1, instruction);
        DefUseChain.getInstance().addUse(operand2, instruction);
        
        return instruction;
    }

    // currently not being used
    public static Instruction empty() {
        Instruction emptyInst = addNewInst(Opcode.EMPTY , null, null);
        // InstCounter--;
        return emptyInst;
    }

    public static Instruction branch(Result target) {
        Instruction branchInst = addNewInst(Opcode.BRA, target, null);
        //***control flow graph
        return branchInst;
    }
    
    public static Instruction mul(Result operand1, Result operand2) {
        return addNewInst(Opcode.MUL, operand1, operand2);
    }
    
    public static Instruction div(Result operand1, Result operand2) {
        return addNewInst(Opcode.DIV, operand1, operand2);
    }
    
    public static Instruction add(Result operand1, Result operand2) {
        return addNewInst(Opcode.ADD, operand1, operand2);
    }
    
    public static Instruction sub(Result operand1, Result operand2) {
        return addNewInst(Opcode.SUB, operand1, operand2);
    }
    
    public static Instruction adda(Result operand1, Result operand2) {
        return addNewInst(Opcode.ADDA, operand1, operand2);
    }
    
    public static Instruction load(Result operand1) {
        return addNewInst(Opcode.LOAD, operand1, null);
    }
    
    public static Instruction store(Result operand1, Result operand2) {
        return addNewInst(Opcode.STORE, operand1, operand2);
    }
    
    public static Instruction move(Result operand1, Result operand2) {
        return addNewInst(Opcode.MOVE, operand1, operand2);
    }
    
    public static Instruction call(Result function, List<Result> params) {
        Instruction callInst = addNewInst(Opcode.CALL, function, null);
        callInst.parameters = params;
        return callInst;
    }

    public static Instruction ret(Result retResult) {
        return addNewInst(Opcode.RET, retResult, null);
    }
    
    public static Instruction cmp(Result operand1, Result operand2) {
        return addNewInst(Opcode.CMP, operand1, operand2);
    }
    
    public static Instruction conditionalBranch(Opcode opcode, Result condition) {
        return addNewInst(opcode, condition, null);
    }
    
    public static Instruction read() {
        return addNewInst(Opcode.READ, null, null);
    }

    public static Instruction write(Result param) {
        return addNewInst(Opcode.WRITE, param, null);
    }

    public static Instruction writeLine() {
        return addNewInst(Opcode.WRITENL, null, null);
    }
    
    public static Instruction PHI(BasicBlock BB, Result designator, Result op1, Result op2) {
        Instruction PhiInstruction = addNewInst(BB, Opcode.PHI, op1, op2);
        PhiInstruction.affectedVariable = designator.getName();
        BB.EmptyInstToPhiInst(PhiInstruction);
        return PhiInstruction;
    }
    
    public void connectTo(Instruction Inst) {
        this.setNext(Inst);
        Inst.setPrevious(this);
    }
    
    public void setPrevious(Instruction prv) {
        this.previous = prv;
    }
	
    public void setNext(Instruction next) {
        this.next = next;
    }
    
    public void ResetBranchDestinationsList() {
        this.BranchDestinationsList = new ArrayList<>();
    }
    
    public void setInstNumber(Integer instNumber) {
        this.InstNumber = instNumber;
    }
    
    public void setOpcode (Opcode opcode) {
        this.opcode = opcode;
    }
    
    public void setOperand1(Result op1) {
        if (opcode == Opcode.BRA) {
            //*** add it to the operand 1 of branch instructions list if it is branch
        }
        this.operand1 = op1;
    }
	
    public void setOperand2(Result op2) {
        if (op2 != null && 
            (opcode == Opcode.BEQ || opcode == Opcode.BNE || opcode == Opcode.BGE || opcode == Opcode.BGT ||
                opcode == Opcode.BLE || opcode == Opcode.BLT)
           ) {
                //*** add it to the operand 2 of branch instructions list if it is branch with 2 operands(with comparison)
        }
        this.operand2 = op2;
    }
    
    public void setBBl(BasicBlock BBl) {
        this.BBl = BBl;
    }
    
    public void print() {
        String op1 = null;
        String op2 = null;
        
        if (this.opcode == Opcode.CALL) {
            System.out.println(InstNumber + " CALL " + operand1.getName());
            return;
        }
        
        if (operand1 != null) 
            switch (operand1.getType()) {
            case VARIABLE:
                op1 = operand1.getName() + "_" + operand1.getInstNumber(); break;
            case CONSTANT:
                op1 = "#" + operand1.constValue; break;
            case INSTRUCTION:
                op1 = "" + operand1.getInstNumber(); break;
            case PROCEDURE:
                op1 = "" + operand1.getInstNumber(); break;
            case ADDRESS:
                op1 = Result.AddrType.toString(operand1.addrType); break;
            case SELECTOR:
                op1 = operand1.getName(); break;
            default:
                op1 = null;
                System.out.println("Unknown operand type"); break;
        }

        if (operand2 != null) 
            switch (operand2.getType()) {
            case VARIABLE:
                op2 = operand2.getName() + "_" + operand2.getInstNumber(); break;
            case CONSTANT:
                op2 = "#" + operand2.constValue; break;
            case INSTRUCTION:
                op2 = "" + operand2.getInstNumber(); break;
            case PROCEDURE:
                op2 = "" + operand2.getInstNumber(); break;
            case ADDRESS:
                op2 = Result.AddrType.toString(operand2.addrType); break;
            case SELECTOR:
                op2 = operand2.getName(); break;
            default:
                op2 = null;
                System.out.println("Unknown operand type"); break;
        }
        
        System.out.println(InstNumber + " " + opcode + " " + op1 + " " + op2);
    }

    public int getInstNumber () { return this.InstNumber; }
    public Opcode getOpcode() { return this.opcode; }
    public Result getOperand1 () { return this.operand1; }
    public Result getOperand2 () { return this.operand2; }
    public BasicBlock getBBl() { return this.BBl; }

    public Instruction getPrevious() { return this.previous; }
    public Instruction getNext() { return this.next; }
    public List<Result> getOperands() {
        List<Result> operands = new ArrayList<>();
        if (operand1 != null)
            operands.add(operand1);
        if (operand2 != null)
            operands.add(operand2);
        
        return operands;
    }
    
    public String toStr() {
        String op1 = null;
        String op2 = null;
        
        StringBuilder retString = new StringBuilder();
        retString.append(InstNumber + " " + opcode + " ");
        
        if (this.opcode == Opcode.CALL) {
            retString.append(operand1.getName());
            return retString.toString();
        }

        if (operand1 != null) {
            switch (operand1.getType()) {
            case VARIABLE:
                op1 = operand1.getName() + "_" + operand1.getInstNumber()/*+ "(" + operand1.baseAddr + ")"*/; break;
            case CONSTANT:
                op1 = "#" + operand1.constValue; break;
            case INSTRUCTION:
                op1 = "" + operand1.getInstNumber(); break;
            case PROCEDURE:
                op1 = "" + operand1.getInstNumber(); break;
            case ADDRESS:
                op1 = Result.AddrType.toString(operand1.addrType); break;
            case SELECTOR:
                op1 = operand1.getName(); break;
            default:
                System.out.println("Unknown operand type"); break;
            }
            
            retString.append(op1 + " ");
        }

        if (operand2 != null) {
            switch (operand2.getType()) {
            case VARIABLE:
                op2 = operand2.getName() + "_" + operand2.getInstNumber() /*+ "(" + operand2.baseAddr + ")"*/; break;
            case CONSTANT:
                op2 = "#" + operand2.constValue; break;
            case INSTRUCTION:
                op2 = "" + operand2.getInstNumber(); break;
            case PROCEDURE:
                op2 = "" + operand2.getInstNumber(); break;
            case ADDRESS:
                op2 = Result.AddrType.toString(operand2.addrType); break;
            case SELECTOR:
                op2 = operand2.getName(); break;
            default:
                op2 = null;
                System.out.println("Unknown operand type"); break;
            }
            retString.append(op2);
        }
        
        return retString.toString();
    }
}