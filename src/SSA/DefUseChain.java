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
    private HashMap<Result, List<Instruction> > chains;
    private static DefUseChain instance = new DefUseChain();
    
    private DefUseChain() {
        chains = new HashMap<>();
    }
    
    public static DefUseChain getInstance() {
        return instance;   
    }
    
    public List<Instruction> getChain(Result key) {
        return chains.get(key);
    }
    
    // append an instruction number (a use) to one chain
    private void append(Result key, Instruction inst) {
        List<Instruction> chain;
        if (!chains.containsKey(key)) {
            chain = new ArrayList<>();
            chain.add(inst);
            
            chains.put(key, chain);
        } else {
            chain = chains.get(key);
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
    
    public void clear() {
        chains.clear();;
    }
    
    public void print() {
        for (Result key: chains.keySet()) {
            StringBuilder retString = new StringBuilder();
            if (key.getType() == ResultType.VARIABLE) {
                retString.append(key.getName() + key.getInstNumber());
            } else if (key.getType() == ResultType.INSTRUCTION) {
                retString.append(key.getInstNumber());
            }
            
            retString.append(": " + key.getType() + " -> ");
            retString.append(chains.get(key));
            System.out.println(retString.toString());
        }
    }
}