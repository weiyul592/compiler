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

import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

public class FileReader{

    public static final char ERROR = (char) 0;
    public static final char EOF = (char) 255;

    private InputStream inputstream;
    private char sym;
    private boolean filestate = true;

    public void Error(String errorMsg) {
        System.out.println(errorMsg);
        sym = ERROR;
    }

    public FileReader(String fileName) {
        try {
            File f = new File(fileName);
            inputstream = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            Error("File was not found\n" + e.toString() );
        }
    }

    public char GetSym() {
        if (filestate) {
            try {
                int data = inputstream.read();
                sym = (char) data;

                // check for end of file
                if (data == -1) {
                    sym = EOF;
                    filestate = false;
                    inputstream.close();
                }
            } catch (IOException e) {
                Error( e.toString() );
            }
            return sym;
        } 

        return sym;
    } 

} 