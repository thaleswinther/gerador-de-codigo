package br.ufscar.dc.compiladores.AnalisadorSintatico.lexico;



import br.ufscar.dc.compiladores.AnalisadorSintatico.sintatico.Parser;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java Main <input file> <output file>");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];

        Lexer lexer = new Lexer(inputFile);
        List<Token> tokens = lexer.tokenize();

        try (PrintWriter writer = new PrintWriter(outputFile)) {
            if (lexer.getErrors().isEmpty()) {
                Parser parser = new Parser(tokens, writer);
                parser.parse(); // Realiza a análise sintática
            } else {
                for (String error : lexer.getErrors()) {
                    writer.println(error);
                }
            }
        }
    }
}

