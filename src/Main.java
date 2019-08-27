/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import Backend.Liveness;
import Backend.RegisterAllocator;
import CFG.ControlFlowGraph;
import Parser.CSEPass;
import Parser.CopyPropagation;
import Parser.Parser;

import java.util.HashMap;
/**
 *
 * @author weiyu
 */

public class Main {
    public static void main(String[] args){

//        Parser.getInstance().parsing("test/test001.txt");
//        Parser.getInstance().parsing("test/test002.txt");
//        Parser.getInstance().parsing("test/test003.txt");
//        Parser.getInstance().parsing("test/test004.txt");
//        Parser.getInstance().parsing("test/test005.txt");
//        Parser.getInstance().parsing("test/test006.txt");

        Parser.getInstance().parsing("test/test007.txt");
        for (ControlFlowGraph cfg : ControlFlowGraph.getCFGs().values()) {
            CopyPropagation CPpass = new CopyPropagation();
            CSEPass csePass = new CSEPass();
        
            //CPpass.execute(cfg);
            //csePass.execute(cfg);
        }
        
        for (ControlFlowGraph cfg : ControlFlowGraph.getCFGs().values()) {
            RegisterAllocator registerAllocator = new RegisterAllocator();
            System.out.println(cfg.getName());
            registerAllocator.execute(cfg);
        }
        
        ControlFlowGraph.generateGraphFiles();
        
//        Parser.getInstance().parsing("test/test008.txt");
//        Parser.getInstance().parsing("test/test009.txt");

//        Parser.getInstance().parsing("test/test010.txt");

//        Parser.getInstance().parsing("test/test011.txt");
//        Parser.getInstance().parsing("test/test012.txt");
//        Parser.getInstance().parsing("test/test013.txt");

//        Parser.getInstance().parsing("test/test014.txt");
//        Parser.getInstance().parsing("test/test015.txt");
//        Parser.getInstance().parsing("test/test016.txt");
//        Parser.getInstance().parsing("test/test017.txt");
//        Parser.getInstance().parsing("test/test018.txt");
//        Parser.getInstance().parsing("test/test019.txt");
//        Parser.getInstance().parsing("test/test020.txt");

        
    }
}