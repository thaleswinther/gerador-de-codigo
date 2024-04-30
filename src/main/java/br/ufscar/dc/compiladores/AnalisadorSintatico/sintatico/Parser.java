package br.ufscar.dc.compiladores.AnalisadorSintatico.sintatico;
import br.ufscar.dc.compiladores.AnalisadorSintatico.lexico.Token;

import java.io.PrintWriter;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int currentTokenIndex = 0;
    private Token currentToken;
    private final PrintWriter writer;

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
    private void program() {
        if (currentToken != null && currentToken.getType().equals("algoritmo")) {
            advance(); // Consome 'algoritmo'
            declarations(); // Processa as declarações
            commands(); // Processa os comandos
            endAlgorithm(); // Verifica se 'fim_algoritmo' está presente
        } else {
            error("algoritmo");
        }
    }
    private void commands() {
        while (currentToken != null && !isEndOfBlock()) {
            switch (currentToken.getType()) {
                case "leia":
                    processRead();
                    break;
                case "escreva":
                    processWrite();
                    break;
                case "se":
                    processConditional();
                    break;
                case "caso":
                    processSwitchCase();
                    break;
                case "para":
                    processForLoop();
                    break;
                case "enquanto":
                    processWhileLoop();
                    break;
                case "faca":
                    processDoWhileLoop();
                    break;
                case "IDENT":
                    if (lookAhead() != null && lookAhead().getType().equals("<-")) {
                        processAssignment();
                    } else {
                        error("comando esperado após IDENT");
                    }
                    break;
                default:
                    error("comando desconhecido ou inesperado");
                    break;
            }
        }
    }
    private boolean isEndOfBlock() {
        return currentToken.getType().equals("fim_algoritmo") ||
                currentToken.getType().equals("fim_se") ||
                currentToken.getType().equals("fim_caso") ||
                currentToken.getType().equals("fim_para") ||
                currentToken.getType().equals("fim_enquanto") ||
                currentToken.getType().equals("senao") ||
                currentToken.getType().equals("ate");
    }

    private void processConditional() {
        advance(); // Consumir 'se'
        expression(); // Avaliar a condição
        expect("entao");
        commands(); // Processar comandos dentro do 'se'

        if (currentToken != null && currentToken.getType().equals("senao")) {
            advance(); // Consumir 'senao'
            commands(); // Processar comandos dentro do 'senao'
        }

        expect("fim_se");
    }
    private void processSwitchCase() {
        advance();  // Consumir 'caso'
        expression();  // Avaliar a variável do switch
        expect("seja");  // Esperar por 'seja'

        while (currentToken != null && !currentToken.getType().equals("senao") && !currentToken.getType().equals("fim_caso")) {
            if (currentToken.getType().equals("NUM_INT") || currentToken.getType().equals("IDENT")) {
                // Processar cada caso
                processCase();
            } else {
                error("Esperado número ou identificador para caso");
            }
        }

        if (currentToken != null && currentToken.getType().equals("senao")) {
            advance();  // Consumir 'senao'
            processWrite();  // Processar o comando dentro do senao
        }

        expect("fim_caso");  // Esperar por 'fim_caso'
    }
    private void processCase() {
        advance();  // Consumir o número ou identificador do caso
        if (currentToken.getType().equals("..")) {
            advance();  // Consumir '..'
            advance();
        }
        expect(":");
        processWrite();  // Processar o comando dentro do caso
    }

    private void processForLoop() {
        advance();  // Consumir 'para'
        if (currentToken != null && currentToken.getType().equals("IDENT")) {
            processAssignment();  // Processar a inicialização
            expect("ate");
            expression();  // Processar a condição de término
            expect("faca");
            commands();  // Processar os comandos dentro do loop
            expect("fim_para");
        } else {
            error("identificador esperado após 'para'");
        }
    }
    private void processWhileLoop() {
        advance();  // Consumir 'enquanto'
        expression();  // Avaliar a condição do loop
        expect("faca");
        commands();  // Processar os comandos dentro do loop
        expect("fim_enquanto");
    }

    private void processDoWhileLoop() {
        advance(); // Consumir 'faca'
        commands(); // Processar os comandos dentro do loop
        expect("ate");
        expression(); // Avaliar a condição para término do loop
    }


    private void processAssignment() {
        advance();
        if (currentToken != null && currentToken.getType().equals("<-")) {
            advance();
            expression();
        } else {
            error("<-");
        }
    }
    private Token lookAhead() {
        if (currentTokenIndex < tokens.size()) {
            return tokens.get(currentTokenIndex);
        } else {
            return null;
        }
    }

    private void processRead() {
        advance(); // Consome 'leia'
        if (currentToken != null && currentToken.getType().equals("(")) {
            advance();
            readArguments();
            if (currentToken != null && currentToken.getType().equals(")")) {
                advance();
            } else {
                error(")");
            }
        } else {
            error("(");
        }
    }

    private void readArguments() {
        do {
            if (currentToken != null && currentToken.getType().equals("IDENT")) {
                advance();
            } else {
                error("identificador");
                return;
            }
            if (currentToken != null && currentToken.getType().equals(",")) {
                advance();
            } else {
                break;
            }
        } while (true);
    }


    private void processWrite() {
        advance();
        if (currentToken != null && currentToken.getType().equals("(")) {
            advance();
            writeArguments();
            if (currentToken != null && currentToken.getType().equals(")")) {
                advance();
            } else {
                error(")");
            }
        } else {
            error("(");
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
            advance();
            declaration(); // Processa uma declaração individual
        }
    }

    private void declaration() {
        boolean first = true;
        do {
            if (!first && currentToken != null && currentToken.getType().equals(",")) {
                advance();
            } else if (!first) {
                error(",");
                return;
            }
            if (currentToken != null && currentToken.getType().equals("IDENT")) {
                advance();
                first = false;
            } else {
                error("identifier");
                return;
            }
        } while (currentToken != null && !currentToken.getType().equals(":"));

        if (currentToken != null && currentToken.getType().equals(":")) {
            advance(); // Consome ':'
            type(); // Processa o tipo da variável
        } else {
            error(":");
        }
    }


    private void type() {
        if (currentToken != null && (currentToken.getType().equals("inteiro") ||
                currentToken.getType().equals("real") ||
                currentToken.getType().equals("literal") ||
                currentToken.getType().equals("logico"))) {
            advance();
        } else {
            error("type"); //
        }
    }

    private void endAlgorithm() {
        if (currentToken != null && currentToken.getType().equals("fim_algoritmo")) {
            advance(); // Consome 'fim_algoritmo'
        } else {
            error("fim_algoritmo");
        }
    }


    // Método para tratar erros
    private void error(String expected) {
        if (currentToken != null) {
            writer.println("Linha " + currentToken.getLine() + ": erro sintático próximo a '" + currentToken.getValue() + "', esperado '" + expected + "'");
        } else {
            writer.println("Erro sintático: esperado '" + expected + "' mas nenhum token foi encontrado");
        }
        advance(); //
    }
    private void expression() {
        // Comeca com uma expressão que pode ser lógica ou relacional
        logicalOrExpression();
    }

    private void logicalOrExpression() {
        // Processa a primeira parte da expressão lógica 'e'
        logicalAndExpression();

        // Enquanto encontrar um operador 'ou', continue processando
        while (currentToken != null && currentToken.getType().equals("ou")) {
            advance();  // Consome 'ou'
            logicalAndExpression();
        }
    }

    private void logicalAndExpression() {
        // Processa a primeira parte da expressão de comparação
        equalityExpression();

        // Enquanto encontrar um operador 'e', continue processando
        while (currentToken != null && currentToken.getType().equals("e")) {
            advance();  // Consome 'e'
            equalityExpression();
        }
    }
    private void equalityExpression() {
        relationalExpression();
        while (currentToken != null && (currentToken.getType().equals("=") || currentToken.getType().equals("!=") || currentToken.getType().equals("<>"))) {
            advance();  // Consome o operador
            relationalExpression();
        }
    }

    private void relationalExpression() {
        additiveExpression();

        while (currentToken != null && isRelationalOperator(currentToken.getType())) {
            advance();  // Consome o operador relacional
            additiveExpression();  // Continua com a próxima parte da expressão
        }
    }

    private boolean isRelationalOperator(String type) {
        return type.equals("<") || type.equals(">") || type.equals("=") || type.equals("!=") ||
                type.equals("<=") || type.equals(">=") || type.equals("%");
    }

    private void additiveExpression() {
        multiplicativeExpression();

        while (currentToken != null && (currentToken.getType().equals("+") || currentToken.getType().equals("-") || currentToken.getType().equals("%"))) {
            String type = currentToken.getType();
            advance();  // Consome '+' ou '-' ou '%'
            if (type.equals("%")) {
                // Trata o operador módulo
                primaryExpression(); // Isso deve ser ajustado se primário não for adequado
            } else {
                // Trata soma ou subtração
                multiplicativeExpression();
            }
        }
    }


    private void multiplicativeExpression() {
        unaryExpression();

        // Processa multiplicação e divisão
        while (currentToken != null && (currentToken.getType().equals("*") || currentToken.getType().equals("/"))) {
            advance();  // Consome '*' ou '/'
            unaryExpression();
        }
    }

    private void unaryExpression() {
        if (currentToken != null) {
            switch (currentToken.getType()) {
                case "nao":
                    advance(); // Consome 'nao'
                    unaryExpression(); // Aplica a negação ao próximo termo
                    break;
                case "-":
                    advance(); // Consome '-'
                    primaryExpression(); // Continua com a expressão após o sinal negativo
                    break;
                default:
                    primaryExpression(); // Processa a expressão primária normalmente
                    break;
            }
        }
    }
    private void primaryExpression() {
        switch (currentToken.getType()) {
            case "NUM_INT", "NUM_REAL" -> advance();  // Consumir número
            case "IDENT" -> {
                if (lookAhead() != null && lookAhead().getType().equals("(")) {
                    functionCall(); // Processar chamada de função se próximo token é '('
                } else {
                    advance();  // Consumir identificador
                }
            }
            case "CADEIA" -> advance(); // Consumir literal de cadeia diretamente
            case "(" -> {
                advance();  // Consumir '('
                expression();  // Processar a expressão interna
                expect(")");
            }
            default -> error("número, identificador, cadeia ou '(' esperado");
        }
    }
    private void functionCall() {
        advance();  // Consumir nome da função
        advance();  // Consumir '('
        while (currentToken != null && !currentToken.getType().equals(")")) {
            expression();
            if (currentToken != null && currentToken.getType().equals(",")) {
                advance();  // Consumir ','
            } else {
                break;
            }
        }
        expect(")");
    }
    private void expect(String expected) {
        if (currentToken != null && currentToken.getType().equals(expected)) {
            advance();
        } else {
            error(expected + " esperado");
        }
    }
}