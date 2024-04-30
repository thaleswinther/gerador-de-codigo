package br.ufscar.dc.compiladores.AnalisadorSintatico.sintatico;


import br.ufscar.dc.compiladores.AnalisadorSintatico.lexico.Token;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int currentTokenIndex = 0;
    private Token currentToken;
    private final PrintWriter writer; // Para escrever no arquivo de saída

    public Parser(List<Token> tokens, PrintWriter writer) {
        this.tokens = tokens;
        this.writer = writer;
        advance(); // Inicializa o primeiro token
    }

    // Método para avançar para o próximo token
    private void advance() {
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex++);
        } else {
            currentToken = null; // EOF
        }
    }

    // Inicia a análise sintática
    public void parse() {
        program();
        if (currentToken != null) {
            writer.println("Erro sintático: token não esperado '" + currentToken.getValue() + "' na linha " + currentToken.getLine());
        } else {
            // Se não houver erros sintáticos, escreva todos os tokens no arquivo de saída
            printTokens();
        }
    }

    private void printTokens() {
        for (Token token : tokens) {
            writer.println(token.toString());
        }
    }



    // Método exemplo para análise de um elemento da gramática
    private void program() {
        if (currentToken != null && currentToken.getType().equals("algoritmo")) {
            advance(); // Consume 'algoritmo'
            declarations(); // Processa as declarações
            commands(); // Processa os comandos
            endAlgorithm(); // Verifica se 'fim_algoritmo' está presente
        } else {
            error("algoritmo"); // Esperado 'algoritmo' no início
        }
    }

    private void commands() {
        while (currentToken != null && !currentToken.getType().equals("fim_algoritmo")) {
            if (currentToken.getType().equals("leia")) {
                processRead();
            } else if (currentToken.getType().equals("escreva")) {
                processWrite();
            } else if (currentToken.getType().equals("IDENT") && lookAhead().getType().equals("<-")) {
                processAssignment();
            } else {
                error("comando"); // Comando desconhecido ou inesperado
            }
        }
    }

    private void processAssignment() {
        String variable = currentToken.getValue();
        advance(); // Consume identifier
        if (currentToken != null && currentToken.getType().equals("<-")) {
            advance(); // Consume '<-'
            expression(); // Process the expression
        } else {
            error("<-"); // Esperado operador de atribuição
        }
    }

    private Token lookAhead() {
        if (currentTokenIndex < tokens.size()) {
            return tokens.get(currentTokenIndex);
        } else {
            return null; // No more tokens
        }
    }

    private void processRead() {
        advance(); // Consume 'leia'
        if (currentToken != null && currentToken.getType().equals("(")) {
            advance(); // Consume '('
            readArguments();
            if (currentToken != null && currentToken.getType().equals(")")) {
                advance(); // Consume ')'
            } else {
                error(")"); // Esperado ')'
            }
        } else {
            error("("); // Esperado '('
        }
    }

    private void readArguments() {
        do {
            if (currentToken != null && currentToken.getType().equals("IDENT")) {
                advance(); // Consume identifier
            } else {
                error("identificador"); // Esperado um identificador
                return;
            }
            if (currentToken != null && currentToken.getType().equals(",")) {
                advance(); // Consume the comma
            } else {
                break;
            }
        } while (true);
    }


    private void processWrite() {
        advance(); // Consume 'escreva'
        if (currentToken != null && currentToken.getType().equals("(")) {
            advance(); // Consume '('
            writeArguments(); // Processa os argumentos dentro do comando escreva
            if (currentToken != null && currentToken.getType().equals(")")) {
                advance(); // Consume ')'
            } else {
                error(")"); // Esperado ')'
            }
        } else {
            error("("); // Esperado '('
        }
    }

    private void writeArguments() {
        if (currentToken == null) return;

        // Processa o primeiro argumento
        if (currentToken.getType().equals("CADEIA")) {
            advance();  // Consumir a cadeia diretamente se o primeiro token for uma cadeia
        } else {
            expression();  // Caso contrário, processa uma expressão
        }

        // Enquanto houver mais argumentos
        while (currentToken != null && currentToken.getType().equals(",")) {
            advance();  // Consumir a vírgula
            if (currentToken != null && currentToken.getType().equals("CADEIA")) {
                advance();  // Consumir a cadeia diretamente se o próximo token for uma cadeia
            } else {
                expression();  // Caso contrário, processa outra expressão
            }
        }
    }


    private void declarations() {
        while (currentToken != null && currentToken.getType().equals("declare")) {
            advance(); // Consume 'declare'
            declaration(); // Processa uma declaração individual
        }
    }

    private void declaration() {
        boolean first = true;
        do {
            if (!first && currentToken != null && currentToken.getType().equals(",")) {
                advance(); // Consumes the comma
            } else if (!first) {
                error(","); // Esperado uma vírgula
                return;
            }
            if (currentToken != null && currentToken.getType().equals("IDENT")) {
                advance(); // Consumes the identifier
                first = false;
            } else {
                error("identifier"); // Esperado um identificador
                return;
            }
        } while (currentToken != null && !currentToken.getType().equals(":"));

        if (currentToken != null && currentToken.getType().equals(":")) {
            advance(); // Consumes ':'
            type(); // Processa o tipo da variável
        } else {
            error(":"); // Esperado ':'
        }
    }


    private void type() {
        if (currentToken != null && (currentToken.getType().equals("inteiro") ||
                currentToken.getType().equals("real") ||
                currentToken.getType().equals("literal") ||
                currentToken.getType().equals("logico"))) {
            advance(); // Consumes the type
        } else {
            error("type"); // Esperado um tipo de dado
        }
    }

    private void endAlgorithm() {
        if (currentToken != null && currentToken.getType().equals("fim_algoritmo")) {
            advance(); // Consume 'fim_algoritmo'
        } else {
            error("fim_algoritmo"); // Esperado 'fim_algoritmo'
        }
    }


    // Método para tratar erros
    private void error(String expected) {
        if (currentToken != null) {
            writer.println("Linha " + currentToken.getLine() + ": erro sintático próximo a '" + currentToken.getValue() + "', esperado '" + expected + "'");
        } else {
            writer.println("Erro sintático: esperado '" + expected + "' mas nenhum token foi encontrado");
        }
        advance(); // Opcional: pular o token problemático
    }

    private void term() {
        factor();
        while (currentToken != null && (currentToken.getType().equals("+") || currentToken.getType().equals("-"))) {
            String opType = currentToken.getType();
            advance();  // Consumir '+' ou '-'
            factor();
        }
    }

    private void factor() {
        primary();
        while (currentToken != null && (currentToken.getType().equals("*") || currentToken.getType().equals("/"))) {
            String opType = currentToken.getType();
            advance();  // Consumir '*' ou '/'
            primary();
        }
    }

    private void primary() {
        if (currentToken.getType().equals("NUM_INT") || currentToken.getType().equals("NUM_REAL") || currentToken.getType().equals("IDENT")) {
            advance();  // Consumir o número ou identificador
        } else if (currentToken.getType().equals("(")) {
            advance();  // Consumir '('
            expression();  // Processar a expressão interna
            expect(")");  // Esperar por ')'
        } else {
            error("número, identificador ou '(' esperado");
        }
    }

    private void expression() {
        logicalOrExpression();
    }

    private void logicalOrExpression() {
        logicalAndExpression();
        while (currentToken != null && currentToken.getType().equals("ou")) {
            advance();  // Consumir 'ou'
            logicalAndExpression();
        }
    }

    private void logicalAndExpression() {
        equalityExpression();
        while (currentToken != null && currentToken.getType().equals("e")) {
            advance();  // Consumir 'e'
            equalityExpression();
        }
    }

    private void equalityExpression() {
        relationalExpression();
        while (currentToken != null && (currentToken.getType().equals("=") || currentToken.getType().equals("!="))) {
            advance();  // Consumir '=' ou '!='
            relationalExpression();
        }
    }

    private void relationalExpression() {
        additiveExpression();
        while (currentToken != null && (currentToken.getType().equals("<") || currentToken.getType().equals(">"))) {
            advance();  // Consumir '<' ou '>'
            additiveExpression();
        }
    }

    private void additiveExpression() {
        multiplicativeExpression();
        while (currentToken != null && (currentToken.getType().equals("+") || currentToken.getType().equals("-"))) {
            advance();  // Consumir '+' ou '-'
            multiplicativeExpression();
        }
    }

    private void multiplicativeExpression() {
        unaryExpression();
        while (currentToken != null && (currentToken.getType().equals("*") || currentToken.getType().equals("/"))) {
            advance();  // Consumir '*' ou '/'
            unaryExpression();
        }
    }

    private void unaryExpression() {
        if (currentToken != null && currentToken.getType().equals("nao")) {
            advance(); // Consumir 'nao'
            unaryExpression(); // Aplicar 'nao' ao próximo termo
        } else {
            primaryExpression();
        }
    }

    private void primaryExpression() {
        if (currentToken.getType().equals("NUM_INT") || currentToken.getType().equals("NUM_REAL") || currentToken.getType().equals("IDENT")) {
            advance();  // Consumir o número ou identificador
        } else if (currentToken.getType().equals("(")) {
            advance();  // Consumir '('
            expression();  // Processar a expressão interna
            expect(")");  // Esperar por ')'
        } else {
            error("número, identificador ou '(' esperado");
        }
    }

    private void expect(String expected) {
        if (currentToken != null && currentToken.getType().equals(expected)) {
            advance();
        } else {
            error(expected + " esperado");
        }
    }
}