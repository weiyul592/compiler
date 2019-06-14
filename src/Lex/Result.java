/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Lex;

import SSA.Instruction;
import SSA.Opcode;
import java.util.List;

/**
 *
 * @author Weiyu, Amir
 */
public class Result {
    
    private ResultType type;
    public int constValue;      // if a constant
    private int instNumber;      // if an instruction
    public int register;        // if a register
    public int baseAddr;        // if a variable
    private String varName;      // if a variable
    public AddrType addrType;   // if a frame pointer, etc.
    public List<Result> ArrayIndexes;
    
    public enum ResultType {
        CONSTANT,
        VARIABLE,
        PROCEDURE,
        ADDRESS,
        SELECTOR,   // for index access
        INSTRUCTION,
    }

    public enum AddrType {
        DF, // global variables
        SP, // stack pointer
        FP; // frame pointer
        
        public static String toString(AddrType addrtype) {
            switch (addrtype) {
                case DF:    return "DF";
                case SP:    return "SP";
                case FP:    return "FP";
                default:    return "Unknown Address Type";
            }
        }
    }
    
    // 
    public static Result newResult(ResultType type, int instNumber) {
        Result result = new Result();
        result.type = type;
        result.instNumber = instNumber;
        return result;
    }
    
    public static Result ConstResult(int constant) {
        Result result = new Result();
        result.setType(ResultType.CONSTANT).setConstValue(constant);
        return result;
    }

    public static Result VarResult(String name) {
        Result result = new Result();
        result.setType(ResultType.VARIABLE).setName(name);
        return result;
    }
    
    public static Result InstResult(int instNumber) {
        Result result = new Result();
        result.setType(ResultType.INSTRUCTION).setInstNumber(instNumber);
        return result;
    }

    public static Result AddrResult(AddrType addrType) {
        Result result = new Result();
        result.setType(ResultType.ADDRESS).setAddrType(addrType);
        return result;
    }

    public static Result ProcResult(String name) {
        Result result = new Result();
        result.setType(ResultType.PROCEDURE).setName(name);
        return result;
    }
    
    public static Result Compute(Result x, Result y, Opcode op) {
        Result ret;
        if ( x.getType() == ResultType.CONSTANT &&
                y.getType() == ResultType.CONSTANT ) {
            int constValue = 0;
            if (op==Opcode.ADD || op==Opcode.ADDA) constValue =  x.constValue + y.constValue;
            else if (op==Opcode.SUB) constValue = x.constValue - y.constValue;
            else if (op==Opcode.MUL) constValue = x.constValue * y.constValue;
            else if (op==Opcode.DIV) constValue = x.constValue / y.constValue;
            else System.out.println("Wrong Opcode");

            ret = ConstResult(constValue);
        } else {
            Instruction inst = Instruction.addNewInst(op, x, y);
            ret = InstResult(inst.getInstNumber());
        }
        return ret;
    }
    
    // check if two results are equal 
    public static boolean equal(Result x, Result y) {
        // not comparable
        if (x == null || y == null) {
            return false;
        }
        
        if (x == y) {
            return true;
        } else if (x.getType() == y.getType()) {
            ResultType type = x.getType();
            switch (type) {
                case CONSTANT: 
                    return x.getConstValue() == y.getConstValue();
                case VARIABLE: 
                    return x.getName() == y.getName() &&
                            x.getInstNumber() == y.getInstNumber();
                case INSTRUCTION:
                    return x.getInstNumber() == y.getInstNumber();
                case PROCEDURE: 
                    System.out.println("procedure result comparison not implemented");
                    return false;
                case ADDRESS: 
                    return x.getAddrType() == y.getAddrType();
                case SELECTOR: 
                    System.out.println("selector result comparison not implemented");
                    return false;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }
    
    public Result setType(ResultType type) {
        this.type = type;
        return this;
    }
    
    public Result setConstValue(int constValue) {
        this.constValue = constValue;
        return this;
    }

    public Result setInstNumber(int instNumber) {
        this.instNumber = instNumber;
        return this;
    }
    
    public Result setBaseAddr(int baseAddr) {
        this.baseAddr = baseAddr;
        return this;
    }
    
    public Result setAddrType(AddrType addrType) {
        this.addrType = addrType;
        return this;
    }
    
    public Result setName(String name) {
        this.varName = name;
        return this;
    }
    
    public ResultType getType(){ return type; }
    public int getInstNumber() { return instNumber; }
    public int getConstValue() { return constValue; }
    public String getName() { return varName; }
    public int getReg() { return register; }
    public AddrType getAddrType() { return addrType; }
}