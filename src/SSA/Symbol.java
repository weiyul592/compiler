/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SSA;

import java.util.ArrayList;
import java.util.List;
import Lex.Result;
/**
 *
 * @author Weiyu, Amir
 */
public class Symbol{
    private SymbolType type;
    private String name;
    private String scope;
    private int baseAddr;
    private boolean isFuncParam;
    
    private List<Integer> dimensions;
    private List<Symbol> parameters;
    private List<Result> values;

    public Symbol() {
        values = new ArrayList<>();
        isFuncParam = false;
    }
    
    public enum SymbolType {
        VARIABLE,
        ARRAY,
        PROCEDURE,
        FUNCTION
    }
    
    public static Symbol variable(String name /*, List<Integer> dimensions*/) {
        Symbol sym = new Symbol();
        sym.setType(SymbolType.VARIABLE).setName(name);
        // sym.addValue(Result.VarResult(name));
        
        // allocate memory address for this variable
        MemoryAllocator.getInstance().Allocate(sym);
        // System.out.println(name + ": " + sym.getBaseAddr());
        
        return sym;
    }
    
    public static Symbol array(String name, List<Integer> dimensions) {
        Symbol sym = new Symbol();
        sym.setType(SymbolType.ARRAY).setName(name).setDim(dimensions);
        
        // allocate memory address for this array
        MemoryAllocator.getInstance().Allocate(sym);        
        // System.out.println(name + ": " + sym.getBaseAddr());
        
        return sym;
    }

    public static Symbol procedure(String name, List<Symbol> parameters) {
        Symbol sym = new Symbol();
        sym.setType(SymbolType.PROCEDURE).setName(name).setParam(parameters);
        
        // allocate memory address for this variable
        MemoryAllocator.getInstance().Allocate(sym);
        
        return sym;
    }

    public static Symbol function(String name, List<Symbol> parameters) {
        Symbol sym = new Symbol();
        sym.setType(SymbolType.FUNCTION).setName(name).setParam(parameters);
        
        // allocate memory address for this variable
        MemoryAllocator.getInstance().Allocate(sym);
        
        return sym;
    }
    
    public void addValue(Result result) {
        values.add(result);
    }
    
    public List<Result> getValueList() {
        return values;
    }
    
    public Result getLastValue() {
        if (values.isEmpty()) {
            // System.out.println( this.name + " has not been initialized");
            return null;
        }
        
        return values.get( values.size() - 1 );
    }
    
    public Integer getValueSize() {
        return values.size();
    }
    
    public boolean isValueEmpty() {
        return values.isEmpty();
    }
    
    public boolean isFuncParameter() {
        return isFuncParam;
    }
    
    public void resetValueList() {
        values.clear();
    }
    
    public Symbol setType(SymbolType type) {
        this.type = type;
        return this;
    }

    public Symbol setName(String name) {
        this.name = name;
        return this;
    }
    
    public Symbol setScope(String scope) {
        this.scope = scope;
        return this;
    }
    
    public Symbol setBaseAddr (int baseAddr) {
        this.baseAddr = baseAddr;
        return this;
    }
    
    public Symbol setDim(List<Integer> dimensions) {
        this.dimensions = dimensions;
        return this;
    }

    public Symbol setParam(List<Symbol> parameters) {
        this.parameters = parameters;
        return this;
    }
    
    public Symbol SetFuncParam() {
        this.isFuncParam = true;
        return this;
    }
    
    public void resetValueListTo(Integer listSize) {
        Integer currSize = this.getValueSize();
        if (currSize <= listSize)
            return;
        else {
            List<Result> sublist = new ArrayList<Result>(values.subList(0, listSize));
            this.values = sublist;
        }
    }
    
    public String getName() { return this.name; }
    public String getScope() { return this.scope; }
    public SymbolType getType() { return this.type; }
    public int getBaseAddr() { return this.baseAddr; }
    public List<Integer> getDim() { return this.dimensions; }
    public List<Symbol> getParam() { return this.parameters; }
}