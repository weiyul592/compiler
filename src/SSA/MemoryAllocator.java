/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SSA;

import java.util.List;
import SSA.Symbol.SymbolType;
/**
 *
 * @author Weiyu, Amir
 */
public class MemoryAllocator {
    private static final int WORD_SIZE = 4;
    private static MemoryAllocator instance = new MemoryAllocator();
    private int globalCounter = 0;
    
    public static MemoryAllocator getInstance() {
        return instance;
    }
    
    public void Allocate(Symbol sym) {
        if (sym.getType() == SymbolType.VARIABLE) {
            sym.setBaseAddr( globalCounter * WORD_SIZE );
            globalCounter++;
        } else if (sym.getType() == SymbolType.ARRAY) {
            sym.setBaseAddr( globalCounter * WORD_SIZE);
            List<Integer> dimensions = sym.getDim();
            
            int dim = 1;
            for (Integer i : dimensions) {
                dim *= i;
            }
            globalCounter += dim;
        } else if (sym.getType() == SymbolType.PROCEDURE) {
            // *** do something?
            sym.setBaseAddr( globalCounter * WORD_SIZE );
            globalCounter++;
        }
        
        //System.out.println( sym.getName() + ": " + sym.getBaseAddr());
    }
    
    public void resetCounter() {
        globalCounter = 0;
    }
}