/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SSA;

/**
 *
 * @author Weiyu, Amir
 */

public enum Opcode {
    NEG,
    ADD,
    SUB,
    MUL,
    DIV,
    CMP,
    ADDA,
    LOAD,
    STORE,
    MOVE,
    PHI,
    END,
    BRA,
    BNE,
    BEQ,
    BLE,
    BLT,
    BGE,
    BGT,
    READ,
    WRITE,
    WRITENL,
    CALL,
    RET,
    
    EMPTY
}