/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CFG;

/**
 *
 * @author Weiyu, Amir
 */

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import Lex.Result;
import SSA.DefUseChain;
import SSA.Instruction;
import SSA.Opcode;

public class BasicBlock {    
    private static BasicBlock currentBBl;
    private Instruction FirstInst;
    private Instruction LastInst;
    public HashMap<String, Instruction> PHIInsts;
    private List<BasicBlock> ImmediateDominations;
    private BasicBlock dominator;
    private ControlFlowGraph cfg;
    
    public Integer BBNum;
    private BasicBlock FallThroughBl;
    private BasicBlock BranchBl;
    private BasicBlock joinBl;
    private List<BasicBlock> parentBlock;
    private List<BasicBlock> childrenBlock;
	
    public BasicBlock() {
        FirstInst = null;
        LastInst = null;
        PHIInsts = new HashMap<>();
        ImmediateDominations = new ArrayList<>();
        FallThroughBl = null;
        BranchBl = null;
        joinBl = null;
        
        parentBlock = new ArrayList<>();
        childrenBlock = new ArrayList<>();
    }

    public static BasicBlock newBBl() {
        BasicBlock BBl = new BasicBlock();
        ControlFlowGraph currentCFG = ControlFlowGraph.getCurrent();
        currentCFG.addBasicBlock(BBl);
        BBl.cfg = currentCFG;
        
        return BBl;
    }

    public void addEmptyInst() {
        if (FirstInst == null) {
            BasicBlock initialBBl = this.currentBBl;
            BasicBlock.setCurrent(this);
            Instruction.empty();
            BasicBlock.setCurrent(initialBBl);
        }
    }
    
    public void addImmediateDomination(BasicBlock Bbl) {
        this.ImmediateDominations.add(Bbl);
    }
    
    // check if the basic block is empty
    public boolean isEmpty() {
        return FirstInst == null && LastInst == null;
    }
    
    // check if a basic block contains empty instruction in the beginning
    public boolean hasEmptyInst() {
        if ( isEmpty() )
            return false;
        else if ( FirstInst.getOpcode() == Opcode.EMPTY )
            return true;
        else
            return false;
    }
    
    public static void setCurrent(BasicBlock BBl) {
        currentBBl = BBl;
    }
	
    public void setFallThroughBl(BasicBlock fallBl) {
        this.FallThroughBl = fallBl;
    }
	
    public void setBranchBlock(BasicBlock BranchBl) {
        this.BranchBl = BranchBl;
    }

    public void setJoinBlock(BasicBlock JoinBl) {
        this.joinBl = JoinBl;
    }

    public void setNum(Integer num) {
        this.BBNum = num;
    }

    public void addParentBlock (BasicBlock parentBlock) {
        this.parentBlock.add(parentBlock);
    }
    
    public void addChildBlock (BasicBlock childBlock) {
        this.childrenBlock.add(childBlock);
    }    
    
    public void setDominator (BasicBlock dominator) {
        this.dominator = dominator;
    }
    
    public void setFirstInst (Instruction inst) {
        this.FirstInst = inst;
    }
    
    public void setLastInst (Instruction inst) {
        this.LastInst = inst;
    }
    
    public void EmptyInstToPhiInst(Instruction Inst) {
        PHIInsts.put(Inst.affectedVariable, Inst);
    }

    //Store all the phi instructions in a current basic block in a array named PhiList
    public List<Instruction> getPhiInstructions() {
        List<Instruction> PhiList = new ArrayList<>();
        Instruction Inst = FirstInst;
        while (Inst != null && 
               Inst.getBBl() == this &&
               Inst.getOpcode() == Opcode.PHI) {
            PhiList.add(Inst);
            Inst = Inst.getNext();
        }
        return PhiList;
    }
    
    public void AddInst(Instruction Inst) {
        if (FirstInst == null) {//We are in the first of job
            FirstInst = Inst;
            LastInst = Inst;
            return ;
        } 
        
        if (Inst.getOpcode() == Opcode.PHI) {
            if (FirstInst.getOpcode() != Opcode.PHI) {
                // Updating branch instructions that have this instruction as operand
                for (Instruction k: FirstInst.BranchDestinationsList) {
                    if (k.getOpcode() == Opcode.BRA) {
                        k.setOperand1( Result.newResult(Result.ResultType.VARIABLE, Inst.getInstNumber()) );
                    } else {
                        k.setOperand2( Result.newResult(Result.ResultType.VARIABLE, Inst.getInstNumber() )) ;
                    }
                }

                FirstInst.BranchDestinationsList = new ArrayList<>();

                Inst.setNext(FirstInst);
                Inst.setPrevious(FirstInst.getPrevious());
                if (FirstInst.getPrevious() != null) {
                    FirstInst.getPrevious().setNext(Inst);
                }
                FirstInst.setPrevious(Inst);
                FirstInst = Inst;
            } else {
                Instruction InsertAfter = FirstInst;
                while (InsertAfter != LastInst && 
                       InsertAfter.getNext() != null &&
                       InsertAfter.getNext().getOpcode() == Opcode.PHI) {
                    InsertAfter = InsertAfter.getNext();
                }
                if (InsertAfter == LastInst || InsertAfter.getNext() == null) {
                    LastInst = Inst;
                }

                if (InsertAfter.getNext() != null){
                    Inst.setNext(InsertAfter.getNext());
                    InsertAfter.getNext().setPrevious(Inst);
                }
                InsertAfter.setNext(Inst);
                Inst.setPrevious(InsertAfter);
            }
        } else {
            if (LastInst.getNext() != null) {
                Inst.setNext(LastInst.getNext());
                LastInst.getNext().setPrevious(Inst);
            }
            LastInst.setNext(Inst);
            Inst.setPrevious(LastInst);
            LastInst = Inst;
        }
    }

    public void removeInst(Instruction inst) {
        BasicBlock block = inst.getBBl();
        if (this != block) {
            System.out.println("instruction not in this block");
            System.out.println("try to remove " + inst.getInstNumber() + " from " + this.toStr());
            return;
        }
        
        Instruction prevInst = inst.getPrevious();
        Instruction nextInst = inst.getNext();
        
        if ( inst == block.getFirstInst() && inst == block.getLastInst()) {
            inst.setOpcode(Opcode.EMPTY);
            inst.setOperand1(null);
            inst.setOperand2(null);
        } else if ( inst == block.getFirstInst() && inst != block.getLastInst()) {
            nextInst.setPrevious(prevInst);
            if (prevInst != null) {
                prevInst.setNext(nextInst);
            }
            
            block.setFirstInst(nextInst);
        } else if ( inst != block.getFirstInst() && inst == block.getLastInst()) {
            prevInst.setNext(nextInst);
            if (nextInst != null) {
                nextInst.setPrevious(prevInst);
            }
            
            block.setLastInst(prevInst);
        } else {
            prevInst.setNext(nextInst);
            nextInst.setPrevious(prevInst);
        }
    }
    
    // insert a new instruction before position
    public void InsertBefore(Instruction newInst, Instruction position) {
        BasicBlock block1 = newInst.getBBl();
        BasicBlock block2 = position.getBBl();

        if (block1 != block2) {
            System.out.println("Invalid insertion\n");
            return;
        }

        // link instructions
        Instruction before_position = position.getPrevious();

        newInst.setPrevious(before_position);
        if (before_position != null)
            before_position.setNext(newInst);
        newInst.setNext(position);
        position.setPrevious(newInst);

        if (position == block2.getFirstInst()) {
            block2.setFirstInst(newInst);

            // update branch destinations
            DefUseChain defUseChain = DefUseChain.getInstance();
            List<Instruction> uses = defUseChain.getUse(position.getInstNumber());

            for (Instruction inst : uses) {
                Opcode opcode = inst.getOpcode();

                if (opcode == Opcode.BRA) {
                    inst.setOperand1( Result.InstResult(newInst.getInstNumber()) );
                } else if (opcode == Opcode.BEQ || opcode == Opcode.BGE
                    || opcode == Opcode.BGT || opcode == Opcode.BLE
                    || opcode == Opcode.BLT || opcode == Opcode.BNE) {

                    inst.setOperand2( Result.InstResult(newInst.getInstNumber()) );
                }
            }
        }
    }

    public static BasicBlock getCurrent() { return currentBBl; }
    public Instruction getFirstInst() { return this.FirstInst; }
    public Instruction getLastInst() { return this.LastInst; }
    public BasicBlock getFallThroughBl() { return this.FallThroughBl; }
    public BasicBlock getBranchBl() { return this.BranchBl; }
    public BasicBlock getJoinBl() { return this.joinBl; }
    public List<BasicBlock> getParent() { return this.parentBlock; }
    public List<BasicBlock> getChildrenBlock() { return this.childrenBlock; }
    public BasicBlock getImmeDominator() { return this.dominator; }
    public List<BasicBlock> getImmediateDominations() { return this.ImmediateDominations; }
    public ControlFlowGraph getCFG() { return this.cfg; }
    
    public void print() {
        if (FirstInst == null || LastInst == null) {
            System.out.println( "the first Instruction or the last Instruction is null" );
            return;
        }
        
        Instruction currInst = FirstInst;
        while (currInst != null && currInst != this.getLastInst().getNext()) {
            currInst.print();
            currInst = currInst.getNext();
        }
    }
    
    public String toStr() {
        return "Basic Block " + BBNum;
    }
}
