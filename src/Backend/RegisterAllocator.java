/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import SSA.Instruction;
import SSA.Opcode;
import CFG.ControlFlowGraph;
import CFG.BasicBlock;
import Lex.Result;
import Lex.Result.ResultType;
/**
 *
 * @author Weiyu, Amir
 */
public class RegisterAllocator {
    private HashMap< Integer, Set<Integer> > liveRanges;
    private InterferenceGraph interGraph;
    
    // private Integer[][] LifeRanges = new Integer[10000][10000];

    public RegisterAllocator() {
        interGraph = new InterferenceGraph();
    }

    public void execute(ControlFlowGraph cfg) {
        Liveness analysis = new Liveness();
        liveRanges = analysis.computeLiveRanges(cfg);
        
        analysis.printLiveRanges();
        
        buildGraph();
        interGraph.print();
    }

    
    // build interference graph
    private void buildGraph() {
        for (Set<Integer> liveSet : liveRanges.values()) {
            List<Integer> liveList = new ArrayList<>(liveSet);
            
            for (int i = 0; i < liveList.size(); i++) {
                for (int j = i + 1; j < liveList.size(); j++) {
                    Integer node1 = liveList.get(i);
                    Integer node2 = liveList.get(j);
                    interGraph.addEdge(node1, node2);
                }
            }
        }
    }
}