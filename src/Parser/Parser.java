/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Parser;

/**
 *
 * @author Weiyu, Amir
 */

// importing the scanner class
import Backend.RegisterAllocator;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import Lex.Token;
import Lex.Scanner;
import Lex.Result;
import Lex.Result.ResultType;
import SSA.Instruction;
import SSA.Opcode;
import SSA.SymbolTable;
import SSA.Symbol;
import SSA.Symbol.SymbolType;
import SSA.DefUseChain;
import SSA.MemoryAllocator;
import CFG.BasicBlock;
import CFG.ControlFlowGraph;

public class Parser {
    private Scanner scanner_obj; // Object for the scanner class.
    private static Parser parser_obj = new Parser(); // Object for the parser class.
    private SymbolTable symbolTable;
    private SymbolTable globalTable;
    private ControlFlowGraph cfg;
        
    private Token currentToken;
    private int last_number;
    private int last_ident;
    private static int currInstCounter = 1;
    private Instruction last_instruction;
    private String fileName;

    void Error(String errorMsg) {
        scanner_obj.Error(errorMsg);
    }

    private void Next() {
        currentToken = scanner_obj.GetToken();
    }
    
    public Parser() {
        // symbolTable = ControlFlowGraph.getCurrent().getSymbolTable();
    }

    // get the object from the parser class
    public static Parser getInstance() {
        return parser_obj;
    }
    
    // Function to parse the input file
    public void parsing(String file) {
        scanner_obj = new Scanner(file);
        fileName = file;
        
        MemoryAllocator.getInstance().resetCounter();
        ControlFlowGraph.initialize();
        ControlFlowGraph.setCurrentCFG( ControlFlowGraph.getMain() );
        
        symbolTable = ControlFlowGraph.getCurrent().getSymbolTable();
        globalTable = ControlFlowGraph.getMain().getSymbolTable();
        
        Next(); // get the first token
        
        if (currentToken != Token.eofToken) {
            computation(); //start to parser
        }

        // rename operands of all instructions based on phi instructions
        for (ControlFlowGraph cfg: ControlFlowGraph.getCFGs().values()) {
            RenamePass renamePass = new RenamePass();
            renamePass.renameOperands(cfg);
        
            //System.out.println(cfg.getName());
            //renamePass.print();
        }
    }

    // check the current token and get the next token
    private boolean IsToken(Token... tokens) {
        String ExpectedTokens = "";
        last_number = scanner_obj.getLastNumber();
        last_ident = scanner_obj.getLastIdentifier();
        for (Token token: tokens) {
            if (currentToken == token) {
                Next();
                return true;
            }
            ExpectedTokens = ExpectedTokens + token.toString() + ", ";
        }

        Error("Expected to recieve one of these tokens: " + ExpectedTokens + " - but received: " + currentToken + ".");
        Next();
        return false;
    }
	
    // "main" {verDecl} {funcDecl} "{" statSequence "}" "."
    private void computation() {
        IsToken(Token.mainToken); 
        // check the current token from scanner class to var or array
        while (currentToken == Token.varToken ||
               currentToken == Token.arrToken) {   
            varDecl();
        }

        // check the current token from scanner class to function or procedure
        while (currentToken == Token.procToken ||
               currentToken == Token.funcToken) {
            funcDecl(); 
        }

        // Switching back to main CFG
        ControlFlowGraph.setCurrentCFG( ControlFlowGraph.getMain() );
        symbolTable = ControlFlowGraph.getMain().getSymbolTable();
        
        //symbolTable.resetValueList();
        BasicBlock mainBBl = BasicBlock.newBBl();
        BasicBlock.setCurrent( mainBBl );
        
        IsToken(Token.beginToken);
        statSequence();
        
        IsToken(Token.endToken);
        IsToken(Token.periodToken); 
        
        //*** Insert end instruction

        //*** wait for the end of file?
    }
	
    // typeDecl ident { "," ident } ";".
    private void varDecl() {
        String scope;
        if (ControlFlowGraph.getCurrent() == ControlFlowGraph.getMain()) {
            scope = "global";
        } else {
            scope = "local";
        }
        
        List<Integer> ArrayDimensions = typeDecl(); // Array can be any dimension. Store all the dimensions in the ArrayDimensions list 
        IsToken(Token.ident);
        
        String var = scanner_obj.getStringFromId(last_ident);
        Symbol symbol;
        
        // Add the variable or the array with its dimentions to the symbol table
        if (ArrayDimensions.isEmpty() ) {
            symbolTable.pushSymbol(var, scope);
        } else {
            symbolTable.pushArray(var, ArrayDimensions);
        }
        
        // Arrays do not support comma separated declaration
        // Add remaining variables to the symbol table
        while (currentToken == Token.commaToken) {
            IsToken(Token.commaToken);
            IsToken(Token.ident);
        
            var = scanner_obj.getStringFromId(last_ident);
            symbolTable.pushSymbol(var, scope);
        }
        IsToken(Token.semiToken);
    }
	
    // "var" | "array" "[" number "]" {"[" number "]"}.
    private List<Integer> typeDecl() {
        List<Integer> ArrayDimensions = new ArrayList<>();

        if (currentToken == Token.varToken)
            IsToken(Token.varToken);
        else if (currentToken == Token.arrToken) {
            IsToken(Token.arrToken);
            do {
                IsToken(Token.openbracketToken);
                IsToken(Token.number);
                ArrayDimensions.add(last_number); // add the last seen number to the array as a dimension
                IsToken(Token.closebracketToken);
            } while (currentToken == Token.openbracketToken);
        }

        return ArrayDimensions;		
    }

    // ("function" | "procedure") ident [formalParam] ";" funcBody ";".
    private void funcDecl() {
        // Check if the current token is funcToken or procToken
        boolean isfun;
        
        if (currentToken == Token.funcToken) isfun = true;
        else isfun = false;
        
        IsToken(Token.funcToken, Token.procToken);
        IsToken(Token.ident);
        Integer ID = last_ident;// Save the ID for the name of the function in Integer ID
        
        String funcName = scanner_obj.getStringFromId(ID);
        
        ControlFlowGraph cfg = ControlFlowGraph.create( funcName );
        ControlFlowGraph.setCurrentCFG( cfg );
        symbolTable = cfg.getSymbolTable();
        
        List<Symbol> Parameters = new ArrayList<>();
        
        // to see if our function has parameters
        if (currentToken == Token.openparenToken)  {
            Parameters = formalParam();
        }
        
        if ( isfun ) {
            globalTable.pushFunction(funcName, Parameters);
        } else {
            globalTable.pushProcedure(funcName, Parameters);
        }
        
        BasicBlock funcBBl = BasicBlock.newBBl();
        BasicBlock.setCurrent(funcBBl);
        // symbolTable.resetValueList();
        
        IsToken(Token.semiToken);
        funcBody();
        IsToken(Token.semiToken);
    }
    
    // "(" [ident {"," ident} ";".
    private List<Symbol> formalParam() {
        List<Symbol> Parameters = new ArrayList<>();// Function or Procedure parameters' list
        
        IsToken(Token.openparenToken);
        // check if the function has parameters or not
        if (currentToken == Token.ident) {
            IsToken(Token.ident);
            String param = scanner_obj.getStringFromId(last_ident);
            
            // Add the lastseenidentifer as a symbol to the symbol table and store it to the ParamSym
            Symbol paramSym = Symbol.variable(param);
            paramSym.SetFuncParam();
//            symbolTable.pushSymbol(param, "local");

            symbolTable.pushSymbol(paramSym);
            Parameters.add(paramSym);
            
            while(currentToken == Token.commaToken) {
                IsToken(Token.commaToken);
                IsToken(Token.ident);
                param = scanner_obj.getStringFromId(last_ident);
                
                // Add the lastseenidentifer as a symbol to the symbol table and store it to the ParamSym
                // symbolTable.pushSymbol(param, "local");
                paramSym = Symbol.variable(param);
                paramSym.SetFuncParam();
                symbolTable.pushSymbol(paramSym);
                
                Parameters.add(paramSym);
            }
        }
        IsToken(Token.closeparenToken);
        return Parameters;
    }
        
    private void funcBody() {
        while (currentToken == Token.varToken ||
               currentToken == Token.arrToken) {
            varDecl();
        }
    
        IsToken(Token.beginToken);
        if (currentToken == Token.letToken ||
                currentToken == Token.callToken || 
                currentToken == Token.ifToken ||
                currentToken == Token.whileToken ||
                currentToken == Token.returnToken) {
            statSequence();
        }
        IsToken(Token.endToken);
    }
        
    // statement { ";" statement} 
    private Instruction statSequence() {
        // get the last instruction from the current basic block and store in LastInstruction
        last_instruction = statement();

        while (currentToken == Token.semiToken) {
            IsToken(Token.semiToken);
            last_instruction = statement();
        }
        
        return last_instruction;
    }
   
    // assignment|funcCall|ifStatement|whileStatement|returnStatement
    private Instruction statement() {
        if (currentToken == Token.letToken) {
            return assignment();
        } else if (currentToken == Token.callToken) {
            Result callResult = funcCall();
            return ControlFlowGraph.getCurrent().getInstruction( callResult.getInstNumber());
        } else if (currentToken == Token.ifToken) {
            return ifStatement();
        } else if (currentToken == Token.whileToken) {
            return whileStatement();
        } else if (currentToken == Token.returnToken) {
            return returnStatement();
        } else {
            return null;
        }
    }

    // "let" designator "<-" expression
    private Instruction assignment() {
        IsToken(Token.letToken);
        Result desigResult = designator();
        IsToken(Token.becomesToken);
        Result exp = expression();
        
        if (exp == null) {
            Error("Can not assign a procedure to a variable");
        }
        
        Instruction retInst = null;
        if (desigResult.getType() == ResultType.VARIABLE) {
            Symbol desig = symbolTable.getSymbol(desigResult.getName());
            if (desig == null) {
                desig = globalTable.getSymbol(desigResult.getName());
            }
            
            if (desig.getScope() == "global") {
                // emit load and store for global variables
                Result DF = Result.AddrResult( Result.AddrType.DF );
                Instruction inst = Instruction.add( DF, Result.ConstResult( desig.getBaseAddr() ) );
                retInst = Instruction.store(exp, Result.InstResult( inst.getInstNumber() ) );
                
                symbolTable.updateTable(desigResult, retInst.getInstNumber());
            } else {
                // System.out.println(desig.getName() + ": " + desig.getScope());
                // emit move for local variables
                // Result valueB4Move = symbolTable.getLastValue(desigResult.getName());
                Integer ListSizeB4Move = symbolTable.getValueSize( desigResult.getName() );
                
                desigResult = symbolTable.updateTable(desigResult, ControlFlowGraph.getCurrent().getInstCounter());
                retInst = Instruction.move(exp, desigResult);
                if (BasicBlock.getCurrent().getJoinBl() != null) {
                    addPhiInstruction(desigResult, ListSizeB4Move /*, valueB4Move, desigResult*/);
                }
            }
        } else if (desigResult.getType() == ResultType.SELECTOR){
            Result flatIndex = symbolTable.lookUpArrayIndex(desigResult);
            Result offset = Result.Compute(flatIndex, Result.ConstResult(-4), Opcode.MUL);
            Result DF = Result.AddrResult( Result.AddrType.DF );
            Instruction instBaseAddr = Instruction.add( DF, Result.ConstResult(desigResult.baseAddr) );
            Instruction instAbsoAddr = Instruction.adda(offset, Result.InstResult( instBaseAddr.getInstNumber() ));
            retInst = Instruction.store(exp, Result.InstResult( instAbsoAddr.getInstNumber() ) );
        } else {
            Error("Error designator type");
        }
        
        return retInst;
    }

    private Instruction addPhiInstruction(Result designatorResult, Integer ListSizeBMove /*, Result befValue, Result newValue*/) {
        BasicBlock currentBBl = BasicBlock.getCurrent();
        BasicBlock joinBl = currentBBl.getJoinBl();
        
        if (joinBl != null) { //need to create phi inst
            Instruction existPhi = joinBl.PHIInsts.get(designatorResult.getName());
            if (existPhi == null) {
                // not any phi instruction for this variable in join block
                Instruction PhiInstruction = Instruction.PHI(joinBl, designatorResult, null, null);;

                //reseting value list
                PhiInstruction.PHIBefValueListSize = ListSizeBMove;
                
                return PhiInstruction;
            } else {
                return existPhi;
            }
        }
        return null;
    }

    // "call" ident ["("[expression{","expression}]")"]
    private Result funcCall() {
        boolean hasReturn = false;
        
        List<Result> args = new ArrayList<>();
        IsToken(Token.callToken);
        IsToken(Token.ident);
        
        String funcName = scanner_obj.getStringFromId(last_ident);
                
        // check if the function has been declared
        Symbol sym = symbolTable.getSymbol(funcName);
        if (sym == null) {
            return null;
        } else if (sym.getType() == SymbolType.PROCEDURE) {
            hasReturn = false;
        } else if (sym.getType() == SymbolType.FUNCTION) {
            hasReturn = true;
        } else {
            Error( funcName + " is not a function");
            return null;
        }
        
        List<Symbol> parameters = sym.getParam();
        
        // collect arguments
        IsToken(Token.openparenToken);
        if (currentToken != Token.closeparenToken) {
            do {
                args.add( expression() );
            } while (currentToken == Token.commaToken && IsToken(Token.commaToken) );
        }
        IsToken(Token.closeparenToken);
        
        // check if the the number of parameters is as expected
        if (parameters.size() != args.size()) {
            Error( "The function takes in " + parameters.size() 
                    + " arguments, but " + args.size() + " provided");
        }
        
        Result callResult = null;
        // check if the function is predefined
        if (sym.getName().equals("InputNum")) {
            callResult = Result.newResult(ResultType.PROCEDURE, Instruction.read().getInstNumber());
        } else if (sym.getName().equals("OutputNum")) {
            callResult = Result.newResult(ResultType.PROCEDURE, Instruction.write(args.get(0)).getInstNumber());
        } else if (sym.getName().equals("OutputNewLine")) {
            callResult = Result.newResult(ResultType.PROCEDURE, Instruction.writeLine().getInstNumber());
        } else {
            callResult = Result.newResult(ResultType.PROCEDURE, 
                                       Instruction.call( Result.ProcResult(funcName), args ).getInstNumber() );
        }
        
        return callResult;
    }
    
    private void ResetBasedOnPHI(List<Instruction> PHIInsts) {
        for (Instruction PHIInst: PHIInsts) {
            String varName = PHIInst.affectedVariable;
            symbolTable.getSymbol(varName).resetValueListTo(PHIInst.PHIBefValueListSize);
        }
    }

    private Instruction ifStatement() {
        BasicBlock BasicBlock_Initial = BasicBlock.getCurrent(); // Get the current basic block
        BasicBlock fallThroughBl = BasicBlock.newBBl();
        BasicBlock joinBlock = BasicBlock.newBBl(); // create a block for join block
        BasicBlock BranchBl = null;
        
        // Connect blocks
        joinBlock.setJoinBlock(BasicBlock_Initial.getJoinBl());
        fallThroughBl.setJoinBlock(joinBlock);
        BasicBlock_Initial.setFallThroughBl(fallThroughBl);
        BasicBlock_Initial.setJoinBlock(joinBlock);
        
        // add domination information
        BasicBlock_Initial.addImmediateDomination(fallThroughBl);
        BasicBlock_Initial.addImmediateDomination(joinBlock);
        fallThroughBl.setDominator(BasicBlock_Initial);
        joinBlock.setDominator(BasicBlock_Initial);
        
        // add child parent information
        BasicBlock_Initial.addChildBlock(fallThroughBl);
        fallThroughBl.addParentBlock(BasicBlock_Initial);
        
        joinBlock.addEmptyInst();
        
        IsToken(Token.ifToken);
        Result RelationRslt = relation();
        IsToken(Token.thenToken);
        Instruction FixUPBraInst = ControlFlowGraph.getCurrent().getInstruction(RelationRslt.getInstNumber());
        
        BasicBlock.setCurrent(fallThroughBl);
        
        Instruction ThenPart_LastInst = statSequence();
        BasicBlock_Initial.getLastInst().connectTo(fallThroughBl.getFirstInst());        
        
        if (currentToken == Token.elseToken) {
            IsToken(Token.elseToken);
            BranchBl = BasicBlock.newBBl();
            
            // connect entry block to branch block
            BasicBlock_Initial.setBranchBlock(BranchBl);
            // set up domination information
            BasicBlock_Initial.addImmediateDomination(BranchBl);
            BranchBl.setDominator(BasicBlock_Initial);
            
            // add child-parent information
            BasicBlock_Initial.addChildBlock(BranchBl);
            BranchBl.addParentBlock(BasicBlock_Initial);
            
            BranchBl.setJoinBlock(joinBlock);
            
            BasicBlock.setCurrent(BranchBl);
            // resetting value list for variables with phi instructions in join block

            ResetBasedOnPHI(joinBlock.getPhiInstructions());

            Instruction ElsePart_LastInst = statSequence();

            Result branchToResult = Result.InstResult( BranchBl.getFirstInst().getInstNumber() );
            FixUPBraInst.setOperand2(branchToResult);
            DefUseChain.getInstance().addUse(branchToResult, FixUPBraInst); // update def-use chain
            
            fallThroughBl = ThenPart_LastInst.getBBl();
            fallThroughBl.setBranchBlock(joinBlock);
            
            // adds corresponding branch instruction to the new fall through block
            BasicBlock.setCurrent(fallThroughBl);
            Instruction.branch(Result.InstResult( joinBlock.getFirstInst().getInstNumber() ) );

            // connests branch block to join block
            BranchBl = ElsePart_LastInst.getBBl();
            BranchBl.setFallThroughBl(joinBlock);
            
            // add child-parent information
            fallThroughBl.addChildBlock(joinBlock);
            BranchBl.addChildBlock(joinBlock);
            joinBlock.addParentBlock(fallThroughBl);
            joinBlock.addParentBlock(BranchBl);
            
            ElsePart_LastInst.connectTo(joinBlock.getFirstInst());
            FixUPBraInst.connectTo(BranchBl.getFirstInst());
        } else {
            BasicBlock_Initial.setBranchBlock(joinBlock);

            Result branchToResult = Result.InstResult(joinBlock.getFirstInst().getInstNumber());
            FixUPBraInst.setOperand2(branchToResult);
            DefUseChain.getInstance().addUse(branchToResult, FixUPBraInst);
            
            // connects fall through block to join block
            fallThroughBl = ThenPart_LastInst.getBBl();
            fallThroughBl.setFallThroughBl(joinBlock);
            
            // add child-parent information
            fallThroughBl.addChildBlock(joinBlock);
            BasicBlock_Initial.addChildBlock(joinBlock);
            joinBlock.addParentBlock(fallThroughBl);
            joinBlock.addParentBlock(BasicBlock_Initial);
            
            ThenPart_LastInst.connectTo(joinBlock.getFirstInst());
        }
        
        IsToken(Token.fiToken);
        ResetBasedOnPHI(joinBlock.getPhiInstructions());

        BasicBlock.setCurrent(joinBlock);
        
        // propagating phi inst to the outer join block
        // updating value list for variables with phi insts
        for (Instruction PhiInst: joinBlock.PHIInsts.values()) {
            String varName = PhiInst.affectedVariable;

            //Result valueB4Move = symbolTable.getLastValue(varName);
            Integer ListSizeB4Move = symbolTable.getValueSize( varName );
            
            symbolTable.updateTable( Result.VarResult(varName), PhiInst.getInstNumber());
            //Result newValue = symbolTable.getLastValue(varName);
            
            addPhiInstruction(Result.VarResult(varName), ListSizeB4Move /*, valueB4Move, newValue*/);
        }
        
        // update value list for phi in inner blocks
        if (joinBlock.getJoinBl() != null) {
            for (Instruction PhiInst: joinBlock.getJoinBl().PHIInsts.values()) {
                Result varResult = Result.VarResult(PhiInst.affectedVariable);
                symbolTable.updateTable(varResult, PhiInst.getInstNumber());
            }
        }
        
        return joinBlock.getLastInst();
    }

    // "while" relation "do" statSequence "od"
    private Instruction whileStatement() {
        BasicBlock BasicBlock_Initial = BasicBlock.getCurrent();
        BasicBlock LoopBlock = BasicBlock.newBBl();
        BasicBlock FollowBlock = BasicBlock.newBBl();
        FollowBlock.addEmptyInst();
        BasicBlock joinBlock;
        
        // entry block for whie statements joins from left
        
        if (BasicBlock_Initial.getFirstInst() == null) {
            joinBlock = BasicBlock_Initial;
        } else {
            joinBlock = BasicBlock.newBBl();
            joinBlock.addEmptyInst();
            BasicBlock_Initial.setFallThroughBl(joinBlock);
            BasicBlock_Initial.getLastInst().connectTo(joinBlock.getFirstInst());
            BasicBlock_Initial.addImmediateDomination(joinBlock);
            joinBlock.setDominator(BasicBlock_Initial);
            
            // add child-parent information
            BasicBlock_Initial.addChildBlock(joinBlock);
            joinBlock.addParentBlock(BasicBlock_Initial);
        }
        
        joinBlock.setFallThroughBl(LoopBlock);
        joinBlock.setBranchBlock(FollowBlock);

        //LoopBlock.setBranchBlock(joinBlock);
        LoopBlock.setJoinBlock(joinBlock);
        
        joinBlock.addImmediateDomination(LoopBlock);
        joinBlock.addImmediateDomination(FollowBlock);
        LoopBlock.setDominator(joinBlock);
        FollowBlock.setDominator(joinBlock);
        
        // add child-parent information
        joinBlock.addChildBlock(LoopBlock);
        joinBlock.addChildBlock(FollowBlock);
        LoopBlock.addParentBlock(joinBlock);
        FollowBlock.addParentBlock(joinBlock);
        
        IsToken(Token.whileToken);

        BasicBlock.setCurrent(joinBlock);
        Result RelationResult = relation();
        Instruction FixUPBraInst = ControlFlowGraph.getCurrent().getInstruction(RelationResult.getInstNumber());
        FixUPBraInst.setOperand2(Result.InstResult(FollowBlock.getFirstInst().getInstNumber()));
        
        IsToken(Token.doToken);
        
        BasicBlock.setCurrent(LoopBlock);
        
        Instruction LastInstruction = statSequence();
        
        for (Instruction PhiInst: joinBlock.PHIInsts.values()) {
            Result varResult = Result.VarResult(PhiInst.affectedVariable);
            symbolTable.updateTable(varResult, PhiInst.getInstNumber());
        }
        
        Instruction LoopBodyBrInst = Instruction.branch(Result.InstResult(joinBlock.getFirstInst().getInstNumber()));
        
        LoopBodyBrInst.connectTo(FollowBlock.getFirstInst());
        joinBlock.getLastInst().connectTo(LoopBlock.getFirstInst());
        
        LoopBlock = LastInstruction.getBBl();
        LoopBlock.setBranchBlock(joinBlock);
        LoopBlock.setJoinBlock(joinBlock);
        
        LoopBlock.addChildBlock(joinBlock);
        joinBlock.addParentBlock(LoopBlock);
        
        FollowBlock.setJoinBlock(BasicBlock_Initial.getJoinBl());
        
        IsToken(Token.odToken);
        
        // *** BasicBlock.setCurrent(joinBlock);
        
        // resetting value list for variables with phi instructions in join block
        ResetBasedOnPHI(joinBlock.getPhiInstructions());
        
        // propogate phi to outer while loop
        BasicBlock.setCurrent(FollowBlock);
        for (Instruction PhiInst: joinBlock.PHIInsts.values()) {            
            String varName = PhiInst.affectedVariable;
            
            //Result valueB4Move = symbolTable.getLastValue(varName);
            Integer ListSizeB4Move = symbolTable.getValueSize( varName );
            
            symbolTable.updateTable( Result.VarResult(varName), PhiInst.getInstNumber());
            //Result newValue = symbolTable.getLastValue(varName);
            
            addPhiInstruction(Result.VarResult(varName), ListSizeB4Move /*, valueB4Move, newValue*/);
        }        
        
        // update symbol table in the outer block
        if (FollowBlock.getJoinBl() != null) {
            for (Instruction PhiInst: FollowBlock.getJoinBl().PHIInsts.values()) {
                Result varResult = Result.VarResult(PhiInst.affectedVariable);
                symbolTable.updateTable(varResult, PhiInst.getInstNumber());
            }
        }
        
        // updating value list for variables with phi insts        
        for (Instruction PhiInst: joinBlock.PHIInsts.values()) {
            Result varResult = Result.VarResult(PhiInst.affectedVariable);
            symbolTable.updateTable(varResult, PhiInst.getInstNumber());
        }
        
        BasicBlock.setCurrent(FollowBlock);
        
        return FollowBlock.getLastInst();
    }

    // "return" [expression]
    private Instruction returnStatement() {
        IsToken(Token.returnToken);
        if (currentToken == Token.number ||
            currentToken == Token.ident ||
            currentToken == Token.openparenToken ||
            currentToken == Token.callToken) {
            Result expression_Result = expression();
            Instruction.ret(expression_Result);
        }
        
        return null;
    }

    // ident{"[" expression "]"}
    private Result designator() {
        IsToken(Token.ident);
        Result var = new Result();
        var.setName( scanner_obj.getStringFromId(last_ident) );
        
        // check if the identifier has been declared
        symbolTable.lookUpSymbol(var);
        
        if (currentToken == Token.becomesToken) {
            var.setType( ResultType.VARIABLE );
        } else if (currentToken == Token.openbracketToken) {
            // if it is an element from an array
            List<Result> ArrayIndexes = new ArrayList<>(); // a list of indexes
            do {
                IsToken(Token.openbracketToken);
                ArrayIndexes.add( expression() ); // dimensions can be expressions
                IsToken(Token.closebracketToken);
            } while (currentToken == Token.openbracketToken);
            var.setType(ResultType.SELECTOR);
            var.ArrayIndexes = ArrayIndexes;
        }

        return var;
    }
    
    private Result expression() {
        Result x, y;
        Opcode op;
        
        x = term();
        while (currentToken == Token.plusToken ||
                currentToken == Token.minusToken ) {
            if (currentToken == Token.plusToken) op = Opcode.ADD; 
            else op = Opcode.SUB;
            
            Next();
            y = term();
            x = Result.Compute(x, y, op);
        }
        return x;
    }    
    
    private Result term() {
        Result x, y;
        Opcode op;
        
        x = factor();
        while (currentToken == Token.timesToken ||
                currentToken == Token.divToken) {
            if (currentToken == Token.timesToken) op = Opcode.MUL; 
            else op = Opcode.DIV;
            
            Next();
            y = factor();
            x = Result.Compute(x, y, op);
        }
        return x;
    }
    
    private Result factor() {
        Result x = new Result();
        if (currentToken == Token.ident) {
            IsToken(Token.ident);
            x.setName( scanner_obj.getStringFromId(last_ident) );
            symbolTable.lookUpSymbol(x);
            
            Symbol sym = symbolTable.getSymbol(x.getName());
            if (sym == null) {
                sym = globalTable.getSymbol(x.getName());
            }
            
            if (currentToken == Token.openbracketToken) {
                // if an array element
                List<Result> ArrayIndexes = new ArrayList<>(); // a list of indexes
                do {
                    IsToken(Token.openbracketToken);
                    ArrayIndexes.add( expression() ); // dimensions can be expressions
                    IsToken(Token.closebracketToken);
                } while (currentToken == Token.openbracketToken);
                
                x.setType(ResultType.SELECTOR);
                x.ArrayIndexes = ArrayIndexes;

                Result flatIndex = symbolTable.lookUpArrayIndex(x);                
                Result offset = Result.Compute(flatIndex, Result.ConstResult(-4), Opcode.MUL);
                Result DF = Result.AddrResult( Result.AddrType.DF );

                Instruction instBaseAddr = Instruction.add( DF, Result.ConstResult(x.baseAddr) );
                Instruction instAbsoAddr = Instruction.adda(offset, Result.InstResult( instBaseAddr.getInstNumber() ));
                Instruction ret = Instruction.load( Result.InstResult(instAbsoAddr.getInstNumber()) );
                
                x = Result.InstResult(ret.getInstNumber());
            } else {
                // if a variable
                if (sym.getScope() == "global" || sym.isFuncParameter()) {
                    Result DF = Result.AddrResult( Result.AddrType.DF );
                    Instruction instBaseAddr = Instruction.add( DF, Result.ConstResult( x.baseAddr ) );
                    Instruction instAbsoAddr = Instruction.load(Result.InstResult( instBaseAddr.getInstNumber() ) );
                    x = symbolTable.updateTable(x, instAbsoAddr.getInstNumber());
                } else {
                        x = symbolTable.getLastValue(x);
                }

                x.setType(ResultType.VARIABLE);
            }
        } else if (currentToken == Token.number) {
            IsToken(Token.number);
            x = Result.ConstResult( last_number );
        } else if (currentToken == Token.openparenToken) {
            // if it is ( expression )
            IsToken(Token.openparenToken);
            x = expression();
            if (currentToken == Token.closeparenToken) 
                IsToken(Token.closeparenToken);
            else 
                Error("Missing a close parenthesis");
        } else if (currentToken == Token.callToken) {
            x = funcCall();
        } else {
            Error("Syntax Error");
            System.out.println( currentToken );
        }
        
        return x;
    }
    
    private Result relation() {
        Result expr1 = expression();
        Token relationToken = relationOperation();
        Result expr2 = expression();
        
        Opcode op;
        switch (relationToken) {
            case eqlToken: op = Opcode.BNE; break;
            case neqToken: op = Opcode.BEQ; break;
            case lssToken: op = Opcode.BGE; break;
            case leqToken: op = Opcode.BGT; break;
            case gtrToken: op = Opcode.BLE; break;
            case geqToken: op = Opcode.BLT; break;
            default: op = null; break;
        }
        
        Result relationResult = Compare(expr1, expr2, op);
        return relationResult;
    }

    private Token relationOperation() {
        // Assign Relation to eqlToken if current token is not a relational token?
        Token Relation = currentToken;
        if (currentToken != Token.eqlToken &&
                currentToken != Token.neqToken &&
                currentToken != Token.lssToken &&
                currentToken != Token.leqToken &&
                currentToken != Token.gtrToken &&
                currentToken != Token.geqToken) {
            Error("Wrong relation operator");
        } else {
            IsToken(Token.eqlToken, Token.neqToken, Token.lssToken, 
                    Token.leqToken, Token.gtrToken, Token.geqToken);
        }
        
        return Relation;
    }
    
    public Result Compare(Result x, Result y, Opcode op) {
        Result cmpResult = null;
        if (x.getType() == ResultType.CONSTANT && y.getType() == ResultType.CONSTANT) {
            cmpResult = Result.ConstResult(x.constValue - y.constValue);
        } else {
            cmpResult = Result.InstResult( Instruction.cmp(x, y).getInstNumber() );
        }
        
        return Result.InstResult(Instruction.conditionalBranch(op, cmpResult).getInstNumber());
    }
    
    public String getFileName() {
        return fileName;
    }
}