/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CFG;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import SSA.Instruction;
import SSA.Symbol;
import SSA.SymbolTable;
import Parser.Parser;
/**
 *
 * @author Weiyu, Amir
 */
public class ControlFlowGraph {
    // Static
    public static final String MAIN = "main";
    private static Map<String, ControlFlowGraph> controlFlowGraphs;
    private static ControlFlowGraph current;
    private HashMap<Integer, Instruction> instructions;
    private SymbolTable symbolTable;
    private HashMap<Integer, Integer> registers;

    // Instance
    private List<BasicBlock> basicBlocks;
    private String name;
    private Integer basicBlockCounter;
    private Integer InstCounter;

    private ControlFlowGraph(String name) {
        this.name = name;
        basicBlocks = new ArrayList<>();
        basicBlockCounter = 0;
        instructions = new HashMap<>();
        InstCounter = 1;
        symbolTable = new SymbolTable();
    }

    public static void initialize() {
        controlFlowGraphs = new HashMap<>();
        controlFlowGraphs.put(MAIN, new ControlFlowGraph(MAIN));
    }

    public static void setCurrentCFG(ControlFlowGraph controlFlowGraph) {
        current = controlFlowGraph;
    }

    public static ControlFlowGraph getCurrent() {
        return current;
    }

    public static Map<String, ControlFlowGraph> getCFGs () {
        return controlFlowGraphs;
    }
    
    public static ControlFlowGraph create(String name) {
        if (controlFlowGraphs == null) {
            throw new RuntimeException("ControlFlowGraph should be first initialized!");
        }

        if (controlFlowGraphs.containsKey(name)) {
            Symbol funcSymbol = ControlFlowGraph.getMain().getSymbolTable().getSymbol(name);
            
            throw new RuntimeException("A " + funcSymbol.getType()
                    + " with name " + name + " already exists!");
        }

        ControlFlowGraph controlFlowGraph = new ControlFlowGraph(name);
        controlFlowGraphs.put(name, controlFlowGraph);
        
        return controlFlowGraph;
    }

    public static ControlFlowGraph get(String name) {
        return controlFlowGraphs.get(name);
    }

    public static ControlFlowGraph getMain() {
        return controlFlowGraphs.get(MAIN);
    }

    public Instruction getInstruction(Integer index) {
        return instructions.get(index);
    }

    public int getInstCounter() {
        return InstCounter;
    }
    
    public void incrInstCounter() {
        InstCounter++;
    }
    
    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        basicBlock.setNum(basicBlockCounter++);
        basicBlocks.add(basicBlock);
    }

    public String getName() {
        return name;
    }

    public void addInstruction(Instruction instruction) {
        this.instructions.put(instruction.getInstNumber(), instruction);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
    
    public BasicBlock getEntryBlock() {
        if (basicBlocks.isEmpty())
            return null;
        else 
            return basicBlocks.get(0);
    }
    
    public void attachRegisters(HashMap<Integer, Integer> coloring) {
        this.registers = coloring;
    }
    
    public HashMap<Integer, Integer> getRegisters () {
        return registers;
    }
    
    // *** print
    public void print() {
        for ( Integer index : current.instructions.keySet() ) {
            current.instructions.get(index).print();
        }
    }
    
    public static void generateGraphFiles() {
        Path graphDirectory = Paths.get("graphs", Parser.getInstance().getFileName());
        if (!(Files.isDirectory(graphDirectory))) {
            graphDirectory.toFile().mkdirs();
        }

        for (ControlFlowGraph controlFlowGraph: controlFlowGraphs.values()) {
            controlFlowGraph.generateControlFlowGraphFile();
            // controlFlowGraph.generateDominationTreeFile();
            // controlFlowGraph.generateChildParentTreeFile();
        }
    }

    public void generateChildParentTreeFile() {
        StringBuilder retString = new StringBuilder();

        retString.append("digraph " + getName() + " {\n\n");

        for (BasicBlock basicBlock: basicBlocks) {
            retString.append("\"" + basicBlock.toStr() + "\" [shape=box];\n");
        }

        retString.append("\n");

        for (BasicBlock basicBlock: basicBlocks) {
            for (BasicBlock childBlock: basicBlock.getChildrenBlock()) {
                retString.append("\"" + basicBlock.toStr() + "\" -> ");
                retString.append("\"" + childBlock.toStr() + "\";\n");
            }
        }
        retString.append("\n}");

        createGraphFile(retString.toString(), "_child");
    }
    
    public void generateDominationTreeFile() {
        StringBuilder retString = new StringBuilder();

        retString.append("digraph " + getName() + " {\n\n");

        for (BasicBlock basicBlock: basicBlocks) {
            retString.append("\"" + basicBlock.toStr() + "\" [shape=box];\n");
        }

        retString.append("\n");

        for (BasicBlock basicBlock: basicBlocks) {
            for (BasicBlock immDom: basicBlock.getImmediateDominations() ) {
                retString.append("\"" + basicBlock.toStr() + "\" -> ");
                retString.append("\"" + immDom.toStr() + "\";\n");
            }
        }
        retString.append("\n}");

        createGraphFile(retString.toString(), "_dom");
    }
    
    public void generateControlFlowGraphFile() {
        StringBuilder retString = new StringBuilder();

        retString.append("digraph " + getName() + " {\n\n");

        for (BasicBlock basicBlock: basicBlocks) {
            StringBuilder instString = new StringBuilder();

            Instruction currentInst = basicBlock.getFirstInst();
            
            if (currentInst != null) {
                do {
                    instString.append(currentInst.toStr()).append("\n");
                    currentInst = currentInst.getNext();
                } while (currentInst != basicBlock.getLastInst().getNext());
            }

            retString.append("\"" + basicBlock.toStr() + "\" [shape=box, label=\"" + basicBlock.toStr()
                    + "\n=================\n" + instString.toString() + "\"];\n");
        }

        retString.append("\n");

        for (BasicBlock basicBlock: basicBlocks) {
            if (basicBlock.getFallThroughBl() != null) {
                retString.append("\"" + basicBlock.toStr() + "\" -> ");
                retString.append("\"" + basicBlock.getFallThroughBl().toStr() + "\";\n");
            }

            if (basicBlock.getBranchBl() != null) {
                retString.append("\"" + basicBlock.toStr() + "\" -> ");
                retString.append("\"" + basicBlock.getBranchBl().toStr() + "\";\n");
            }
        }

        retString.append("\n}");
        
        createGraphFile(retString.toString());
    }

    private void createGraphFile(String graphDescription) {
        createGraphFile(graphDescription, "");
    }

    private void createGraphFile(String graphDescription, String fileNamePostfix) {
        try {
            // only create .gv
            Path graphFile = Paths.get("graphs", Parser.getInstance().getFileName(),
                    getName() + fileNamePostfix + ".gv");
            Files.write(graphFile, graphDescription.getBytes());
            
            /*
            Path psFile = Paths.get("graphs", Parser.getInstance().getFileName(),
                    getName() + fileNamePostfix + ".ps");

            Runtime.getRuntime().exec("dot -Tps " + graphFile + " -o " + psFile);
            */
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public String toString() {
        StringBuilder retString = new StringBuilder();
        Instruction currentInstruction = basicBlocks.get(0).getFirstInst();
        while (currentInstruction != null) {
            retString.append(currentInstruction + "\n");
            currentInstruction = currentInstruction.getNext();
        }
        return retString.toString();
    }
}
