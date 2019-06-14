/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Lex;

/**
 *
 * @author Weiyu, Amir
 */

public enum Token {
    errorToken ("ERROR", 0),

    timesToken ("*", 1),
    divToken ("/", 2),

    plusToken ("+", 11),
    minusToken ("-", 12),

    eqlToken ("==", 20),
    neqToken ("!=", 21),
    lssToken ("<", 22),
    geqToken (">=", 23),
    leqToken ("<=", 24),
    gtrToken (">", 25),

    periodToken (".", 30),
    commaToken (",", 31),
    openbracketToken ("[", 32),
    closebracketToken ("]", 34),
    closeparenToken (")", 35),

    becomesToken ("<-", 40),
    thenToken ("then", 41),
    doToken ("do", 42),

    openparenToken ("(", 50),
    
    number ("number", 60),
    ident ("identifier", 61),

    semiToken (";", 70),

    endToken ("}", 80),
    odToken ("od", 81),
    fiToken ("fi", 82),

    elseToken ("else", 90),

    letToken ("let", 100),
    callToken ("call", 101),
    ifToken ("if", 102),
    whileToken ("while", 103),
    returnToken ("return", 104),

    varToken ("var", 110),
    arrToken ("array", 111),
    funcToken ("function", 112),
    procToken ("procedure", 113),

    beginToken ("{", 150),
    mainToken ("main", 200),
    eofToken ("end of file", 250);

    private final String sym;
    private final int value;
    public String sym() { return sym; }
    public int value() { return value; }

    Token(String sym, int value) { 
        this.sym = sym;
        this.value = value; 
    }
    
    public void print() {
        System.out.println(this);
    }
}