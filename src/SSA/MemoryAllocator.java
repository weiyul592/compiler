/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SSA;

import java.util.List;
import SSA.Symbol.SymbolType;
import java.util.HashMap;
/**
 *
 * @author Weiyu, Amir
 */
public class MemoryAllocator {
    private static final int WORD_SIZE = 4;
    private static MemoryAllocator instance = new MemoryAllocator();
    private HashMap<String, Integer> variables;
    private int globalCounter = 0;
    
    public static MemoryAllocator getInstance() {
        return instance;
    }
    
    public MemoryAllocator() {
        // does not seem to be useful so far
        variables = new HashMap<>();
    }
    
    public void Allocate(Symbol sym) {
        if (sym.getType() == SymbolType.VARIABLE) {
            sym.setBaseAddr( -globalCounter * WORD_SIZE );
            variables.put(sym.getName(), globalCounter);
            globalCounter++;
        } else if (sym.getType() == SymbolType.ARRAY) {
            sym.setBaseAddr( -globalCounter * WORD_SIZE);
            variables.put(sym.getName(), globalCounter);
            List<Integer> dimensions = sym.getDim();
            
            int dim = 1;
            for (Integer i : dimensions) {
                dim *= i;
            }
            globalCounter += dim;
        } else if (sym.getType() == SymbolType.PROCEDURE) {
            // *** do something?
            sym.setBaseAddr( -globalCounter * WORD_SIZE );
            variables.put(sym.getName(), globalCounter);
            globalCounter++;
        }
        
        //System.out.println( sym.getName() + ": " + sym.getBaseAddr());
    }
    
    public void resetCounter() {
        globalCounter = 0;
    }
    
    public Integer getGlobalCounter() {
        return globalCounter;
    }
    
    public HashMap<String, Integer> getVariables() {
        return variables;
    }
}
