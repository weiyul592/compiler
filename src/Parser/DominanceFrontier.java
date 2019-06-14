/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Parser;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import CFG.BasicBlock;
import CFG.ControlFlowGraph;

/**
 *
 * @author Weiyu, Amir
 */
public class DominanceFrontier {
    private HashMap<Integer, Set<BasicBlock> > DFs;
    
    public DominanceFrontier() {
        DFs = new HashMap<>();
    }
    
    public void computeDF(BasicBlock node) {
        //List<BasicBlock> S = new ArrayList<>();
        Set<BasicBlock> S = new HashSet<>();
        
        for (BasicBlock child : node.getChildrenBlock()) {
            if (child.getImmeDominator() != node) {
                S.add(child);
            }
        }
        
        for (BasicBlock domination_child : node.getImmediateDominations() ) {
            computeDF(domination_child);
            
            for (BasicBlock w : getDF(domination_child.BBNum) ) {
                if (node.getImmediateDominations().contains(w) || node == w) {
                    S.add(w);
                }
            }
        }
        
        DFs.put(node.BBNum, S);
    }
    
    public Set<BasicBlock> getDF(Integer node) {
        return DFs.get(node);
    }
    
    public void print() {
        for (Integer node: DFs.keySet()) {
            Set<BasicBlock> dominanceFrontier = DFs.get(node);
            
            System.out.print(node + " has DF: ");
            for (BasicBlock block : dominanceFrontier) {
                System.out.print(block.BBNum + " ");
            }
            System.out.print("\n");
        }
    }
}
