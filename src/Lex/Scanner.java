/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Lex;
import java.util.HashMap;

/**
 *
 * @author Weiyu, Amir
 */   

public class Scanner {
    public FileReader fileReader;

    /* moved up from FileReader */
    private char inputSym;  // the current character on the input
    private void Next() {   // advance to the next cahracter
        inputSym = fileReader.GetSym();
        col_number += 1;
    }

    public int last_number; // the last number seen
    public int last_ident;  // the last identifier seen

    private Token token;
    private HashMap<String, Integer> String2Id;
    private HashMap<Integer, String> Id2String;
    private int ident_counter;

    private int line_number = 1;
    private int col_number = 0;

    private String[] reserved;

    public String getStringFromId(int id) {
        return Id2String.get(id);
    }

    public int getIdFromString(String name) {
        return String2Id.get(name);
    }
    
    public void Error(String errorMsg) {
        fileReader.Error(errorMsg);
    }

    public Scanner(String fileName){
        fileReader = new FileReader(fileName);
        Next();

        String2Id = new HashMap<>();
        Id2String = new HashMap<>();
        ident_counter = 0;

    	prePopulate();
    }

    /* prepopulate the table with Id2String and String2Id with reserved words 
     * and predefined functions 
     */
    private void prePopulate() {
        reserved = new String[] {"then", "do", "od", "fi", "else", "let", "call", "if", "while", "return", 
                                            "var", "array", "function", "procedure", "main", "enf of file"};
        for (int i = 0; i < reserved.length; i++) {
            String2Id.put(reserved[i], ident_counter);
            Id2String.put(ident_counter, reserved[i]);
            ident_counter += 1;
        }
        
        String[] predefinedFunc = new String[] { "InputNum", "OutputNum", "OutputNewLine" };
        for (int i = 0; i < predefinedFunc.length; i++) {
            String2Id.put(predefinedFunc[i], ident_counter);
            Id2String.put(ident_counter, predefinedFunc[i]);
            ident_counter += 1;
        }
    }

    public Token GetToken() {

        // skip white spaces, '\t' and '\n' before processing tokens
        while ( Character.isWhitespace(inputSym) ) {
            if (inputSym == '\n') {
                Next();
                line_number += 1;
                col_number = 1;    
            } else Next();
        }

        if (inputSym != FileReader.EOF) {
            if ( Character.isDigit(inputSym) ) {
                getNumber();
                // System.out.println("number found: " + last_number);
            } else if ( Character.isLetter(inputSym) ) {
                getIdentifier();
                // System.out.println("letter found: " + Id2String.get(last_ident));
            } else if (inputSym == '<' || inputSym == '>' || inputSym == '!' || inputSym == '=') {
                getRelOrBecomes();
            } else {
                getSymbol();    // process other symbols
            }
        } else {
            token = Token.eofToken;
        }

        return token;
    }

    private void getNumber() {
        String number = Character.toString(inputSym);

        Next();
        while ( Character.isDigit(inputSym) ) {
            number += Character.toString(inputSym);
            Next();
        }

        token = Token.number;
        last_number = Integer.parseInt(number);
    }

    private void getIdentifier() {
        String ident = Character.toString(inputSym);

        Next();
        while ( Character.isDigit(inputSym) || Character.isLetter(inputSym) ) {
            ident += Character.toString(inputSym);
            Next();
        }

        // if the identifier is not in the symbol table, then add it
        if ( !String2Id.containsKey(ident) ){
            Id2String.put(ident_counter, ident);
            String2Id.put(ident, ident_counter);
            ident_counter += 1;
        }

        if ( !checkReserved(ident) ) {  // if not a reserved word
            token = Token.ident;
            last_ident = String2Id.get(ident);
        }    
    }

    private boolean checkReserved(String ident) {
        boolean is_reserved = false;

        for (int i = 0; i < reserved.length; i++) {
            if ( ident.equals(reserved[i]) ) {
                is_reserved = true;
                break;
            }
        }

        if (is_reserved) {	// if it is a reserved word, get the right token
            for (Token tok : Token.values() ) {
                if ( ident.equals(tok.sym() ) ) {
                    token = tok;
                    break;
                }
            }
        }

        return is_reserved;
    }

    private void getRelOrBecomes() {
        // get relational operators or "becomes" tokens
        switch(inputSym) {
            case '=':
                Next();
                if (inputSym == '=') {
                    token = Token.eqlToken;
                    Next(); 
                } else {
                    token = Token.errorToken;   // did not find expected character
                    int cur_col = col_number - 1;
                    Error("Error token at line " + line_number + " column " + cur_col);
                } break;
            case '!':
                Next();
                if (inputSym == '=') {
                    token = Token.neqToken;
                    Next(); 
                } else {
                    token = Token.errorToken;   // did not find expected character
                    int cur_col = col_number - 1;
                    Error("Error token at line " + line_number + " column " + cur_col);
                } break;
            case '<':
                Next();
                if (inputSym == '=') {
                    token = Token.leqToken;
                    Next();
                } else if (inputSym == '-') {
                    token = Token.becomesToken;
                    Next();
                } else {
                    token = Token.lssToken;
                } break;
            case '>':
                Next();
                if (inputSym == '=') {
                    token = Token.geqToken;
                    Next();
                } else {
                    token = Token.gtrToken;
                } break;
            default: break;
        }
    }

    private void getSymbol() {
        // deal with other symbols
    	boolean comment_found = false;

        switch(inputSym) {
            case '*' :  token = Token.timesToken;       Next(); break;
            case '+' :  token = Token.plusToken;        Next(); break;
            case '-' :  token = Token.minusToken;       Next(); break;
            case '.' :  token = Token.periodToken;      Next(); break;
            case ',' :  token = Token.commaToken;       Next(); break;
            case '[' :  token = Token.openbracketToken; Next(); break;
            case ']' :  token = Token.closebracketToken;Next(); break;
            case ')' :  token = Token.closeparenToken;  Next(); break;
            case '(' :  token = Token.openparenToken;   Next(); break;
            case ';' :  token = Token.semiToken;        Next(); break;
            case '}' :  token = Token.endToken;         Next(); break;
            case '{' :  token = Token.beginToken;       Next(); break;
            case '#' :  // ignore comments that start with #
            	while (inputSym != '\n') Next(); 
            	comment_found = true;
            	break;
            case '/' :
                Next(); 
                if (inputSym == '/') {	// if it is a comment
                	while (inputSym != '\n') Next(); 
                	comment_found = true;
                } else {
                	token = Token.divToken;
                } break;

            default: 
                token = Token.errorToken;   // unrecognized symbols
                Error("Error token at line " + line_number + " column " + col_number);
                Next(); break;
        }
        if (comment_found) { 
        	GetToken();
        }
    }

    public int getLastNumber() {
	return last_number;
    }

    public int getLastIdentifier() {
        return last_ident;
    }

    public Token getCurrentToken() {
        return token;
    }
    
/* checking output, for debuging purposes */
    public void print() {
        System.out.println(String2Id);
        System.out.println(Id2String);
    }

    public void printToken() {
        System.out.print(token + ", ");
        if (token == Token.number) {
            System.out.printf("name: %d\n", last_number);
        } else if (token == Token.ident) {
            System.out.printf("name: %s, id: %d\n", Id2String.get(last_ident), last_ident );
        } else {
            System.out.printf("name: %s\n", token.sym());
        }
    }
/* checking output, for debuging purposes */
}