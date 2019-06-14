/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SSA;
import CFG.ControlFlowGraph;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import Lex.Result;
import Lex.Result.ResultType;
import SSA.Symbol.SymbolType;
/**
 *
 * @author Weiyu, Amir
 */
public class SymbolTable {
    private HashMap<String, Symbol> SymTable;
    // private HashMap<String, Symbol> VarToSymbol = new HashMap();;
    // private static SymbolTable instance = new SymbolTable();
    
    private void Error (String errorMsg) {
        System.out.println(errorMsg);
    }
    
    public SymbolTable() {
        SymTable = new HashMap();
        prePopulate();
    }
    
    // add predefined functions to symbol table
    private void prePopulate() {
        List<Symbol> oneParameter = new ArrayList<>();
        oneParameter.add( Symbol.variable("oneparam") );
        
        pushProcedure("InputNum", oneParameter);
        pushProcedure("OutputNum", oneParameter);
        pushProcedure("OutputNewLine", new ArrayList<>());
    }

    public void pushSymbol(Symbol sym) {
        SymTable.put(sym.getName(), sym);
    }
    
    public void pushSymbol(String var, String scope) {
        Symbol sym = Symbol.variable(var);
        sym.setScope(scope);
//        System.out.printf("%s has base address %d\n", var, sym.getBaseAddr());
        
        SymTable.put(var, sym);
    }
    
    public void pushArray(String var, List<Integer> dimensions) {
        // push symbol var with its dimensions into Symbol Table
        Symbol sym = Symbol.array(var, dimensions);
//        System.out.printf("%s has base address %d\n", var, sym.getBaseAddr());
        
        //List<String> list = new ArrayList<String>();
        SymTable.put(var, sym);
    }

    public void pushProcedure(String funcName, List<Symbol> parameters) {
        Symbol sym = Symbol.procedure(funcName, parameters);
//        System.out.printf("%s has base address %d\n", funcName, sym.getBaseAddr());
        
        //List<String> list = new ArrayList<>();
        SymTable.put(funcName, sym);
        //SymTable.put(sym, list);
        //VarToSymbol.put(funcName, sym);
    }

    public void pushFunction(String funcName, List<Symbol> parameters) {
        Symbol sym = Symbol.function(funcName, parameters);
//        System.out.printf("%s has base address %d\n", funcName, sym.getBaseAddr());
        
        //List<String> list = new ArrayList<>();
        SymTable.put(funcName, sym);
    }
    
    public boolean findSymbol(String var) {
        if (SymTable.containsKey(var))
            return true;
        
        return false;
    }
    
    // check if a variable has been declared
    public void lookUpSymbol(Result result) {
        ControlFlowGraph curr_cfg = ControlFlowGraph.getCurrent();
        ControlFlowGraph main_cfg = ControlFlowGraph.getMain();
        Symbol sym;
        
        sym = getSymbol( result.getName() );
     
        if (sym == null && curr_cfg != main_cfg) {
            // if symbol does not found locally, look it up in the global table
            SymbolTable globalTable = main_cfg.getSymbolTable();
            sym = globalTable.getSymbol( result.getName() );
        }
        
        if (sym != null) {
            result.setBaseAddr( sym.getBaseAddr() );
        }
    }
    
    
    // get a symbol based on a variable name.  
    public Symbol getSymbol(String symbolName) {
        if ( !findSymbol(symbolName) ) {
            // Error(symbolName + " undeclared");
            return null; // *** maybe raise exception
        }

        Symbol sym = SymTable.get(symbolName);
        return sym;
    }
    
    // get dimension
    public Result lookUpArrayIndex(Result result) {
        ControlFlowGraph curr_cfg = ControlFlowGraph.getCurrent();
        ControlFlowGraph main_cfg = ControlFlowGraph.getMain();
        Symbol sym;
        
        sym = getSymbol(result.getName());
        if (sym == null && curr_cfg != main_cfg) {
            SymbolTable globalTable = main_cfg.getSymbolTable();
            sym = globalTable.getSymbol(result.getName());
        }
        
        List<Integer> dimensions = sym.getDim();
        List<Result> ArrayIndexes = result.ArrayIndexes;
        
        if ( ArrayIndexes.size() != dimensions.size() ){
            Error( "Index sizes mismatch" );
            return null;    // exception?
        }
        
        List<Integer> baseIndexes = new ArrayList<>();
        
        for (int i = 0; i < dimensions.size(); i++) {
            int arrayIndex = 1;
            
            int j = i + 1;
            while (j < dimensions.size() ) {
                arrayIndex *= dimensions.get(j);
                j++;
            }
            baseIndexes.add(arrayIndex);
        }

        Result index = ArrayIndexes.get(0);
        int baseIndex = baseIndexes.get(0);
            
        Result flatIndex = Result.Compute(index, Result.ConstResult(baseIndex), Opcode.MUL);
        
        for (int i = 1; i < dimensions.size(); i++ ) {
            index = ArrayIndexes.get(i);
            baseIndex = baseIndexes.get(i);
            
            Result tmp = Result.Compute(index, Result.ConstResult(baseIndex), Opcode.MUL);
            flatIndex = Result.Compute(flatIndex, tmp, Opcode.ADD);
        }
        
        return flatIndex;
    }
    
    public Result updateTable(Result result, int instNumber) {
        ControlFlowGraph curr_cfg = ControlFlowGraph.getCurrent();
        ControlFlowGraph main_cfg = ControlFlowGraph.getMain();
        String var = result.getName();
        
        if ( findSymbol(var) ) {
            Symbol sym = SymTable.get(var);
            sym.addValue(result);
            result.setBaseAddr( sym.getBaseAddr() );
        } else if (curr_cfg != main_cfg) {
            SymbolTable globalTable = main_cfg.getSymbolTable();
            Symbol sym = globalTable.getSymbol(var);   
            sym.addValue(result);
            
            result.setBaseAddr( sym.getBaseAddr() );
        }
        
        result.setInstNumber(instNumber);
        
        return result;
    }

    // get last value of an identifier
    public Result getLastValue(Result result) {
        return getLastValue( result.getName() );
    }
    
    // overloaded version for Strings
    public Result getLastValue(String varName) {
        ControlFlowGraph curr_cfg = ControlFlowGraph.getCurrent();
        ControlFlowGraph main_cfg = ControlFlowGraph.getMain();
        Result ret = null;
        
        Symbol sym = getSymbol( varName );
        
        if (sym == null && curr_cfg != main_cfg){
            // current cfg is the cfg of a function. 
            // Since the symbol is not found, look it up in the global symbol table
            SymbolTable globalTable = main_cfg.getSymbolTable();
            sym = globalTable.getSymbol(varName);    
        }
        
        if (sym != null) {
            ret = sym.getLastValue();
        } 
        
        return ret;
    }  

    public Integer getValueSize(String varName) {
        Symbol symbol = getSymbol(varName);
        if (symbol == null) {
            return 0;
        } else {
            return symbol.getValueSize();
        }
    }
    
    public void resetValueList() {
        for ( Symbol sym: SymTable.values() ) {
            sym.resetValueList();
        }
    }
    
    public void printTable() {
        for (Symbol sym: SymTable.values() ) {
//            if ( sym.getType() == SymbolType.FUNCTION || sym.getType() == SymbolType.PROCEDURE)
//                continue;
            System.out.print(sym.getName() + ": ");
            for (Result r: sym.getValueList()) {
                System.out.print(r.getName() + r.getInstNumber() + ", ");
            }
            System.out.println("");
            
        }
    }
}