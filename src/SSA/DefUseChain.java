/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SSA;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import Lex.Result;
import Lex.Result.ResultType;
/**
 *
 * @author Weiyu, Amir
 */
public class DefUseChain {
    private HashMap<Integer, List<Instruction> > chains;
    private HashMap<Integer, Result> resultMap;
    private static DefUseChain instance = new DefUseChain();
    
    private DefUseChain() {
        chains = new HashMap<>();
        resultMap = new HashMap<>();
    }
    
    public static DefUseChain getInstance() {
        return instance;   
    }
    
    public List<Instruction> getUse(Integer key) {
        return chains.get(key);
    }
    
    // append an instruction number (a use) to one chain
    private void append(Result key, Instruction inst) {
        List<Instruction> chain;
        if (!chains.containsKey(key.getInstNumber())) {
            chain = new ArrayList<>();
            chain.add(inst);
            
            chains.put(key.getInstNumber(), chain);
            resultMap.put(key.getInstNumber(), key);
        } else {
            chain = chains.get(key.getInstNumber());
            chain.add(inst);
        }
        // chains.get(key)
    }
    
    public void addUse(Result key, Instruction inst) {
        if (key == null)
            return;
        
        if (key.getType() == ResultType.VARIABLE || key.getType() == ResultType.INSTRUCTION) {
            append(key, inst);
        }
    }
    
    public void removeUse(Result key, Instruction use) {
        if (key == null)
            return;
        
        if (key.getType() == ResultType.VARIABLE || key.getType() == ResultType.INSTRUCTION) {
            if (!chains.containsKey(key.getInstNumber()))
                return;
            else {
                List<Instruction> chain = chains.get(key.getInstNumber());
                chain.remove(use);
            }
            
        }
    }
    
    public void clear() {
        chains.clear();;
    }
    
    public void print() {
        for (Integer inst_number: chains.keySet()) {
            Result key = resultMap.get(inst_number);
            StringBuilder retString = new StringBuilder();
            if (key.getType() == ResultType.VARIABLE) {
                retString.append(key.getName() + key.getInstNumber());
            } else if (key.getType() == ResultType.INSTRUCTION) {
                retString.append(key.getInstNumber());
            }
            
            retString.append(": " + key.getType() + " -> ");
            List<Instruction> insts = chains.get(inst_number);
            for (Instruction inst : insts) {
                retString.append(inst.toStr());
                retString.append(", ");
            }
            System.out.println(retString.toString());
        }
    }
}