package br.ufscar.dc.compiladores.AnalisadorSintatico.lexico;



import java.io.*;
import java.util.*;

public class Lexer {
    private final PushbackReader reader;
    private int currentChar;
    private boolean endOfFile = false;
    private int lineNumber = 1;
    private final List<String> errors = new ArrayList<>();

    public List<String> getErrors() {
        return errors;
    }

    // Lista de palavras reservadas
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            "algoritmo", "declare", "leia", "escreva", "fim_algoritmo", "literal", "inteiro", "real",
            "logico", "e", "ou", "nao", "se", "senao", "fim_se", "entao", "caso", "seja", "fim_caso",
            "para", "ate", "faca", "fim_para", "enquanto", "fim_enquanto", "registro", "fim_registro",
            "tipo", "procedimento", "var", "fim_procedimento", "funcao", "retorne", "fim_funcao",
            "constante", "falso", "verdadeiro"
    ));

    public Lexer(String filePath) throws IOException {
        reader = new PushbackReader(new FileReader(filePath), 2);  // Capacidade para ".."
        advance();
    }


    // Avança para o próximo caractere no fluxo de entrada.
    private void advance() throws IOException {
        currentChar = reader.read();
        if (currentChar == '\n') {
            lineNumber++;
        }
        if (currentChar == -1) {
            endOfFile = true;
        }
    }

    /*
     * Analisa o arquivo de entrada e identifica os tokens conforme as regras léxicas.
     * A função retorna uma lista de tokens identificados ou termina a execução se encontrar erros fatais.
     * retorna a Lista de tokens identificados.
     *
     */
    public List<Token> tokenize() throws IOException {
        List<Token> tokens = new ArrayList<>();
        while (!endOfFile) {
            while (Character.isWhitespace(currentChar)) {
                advance();
            }
            if (endOfFile)  {
                break;
            } else if (Character.isLetter(currentChar)) {
                tokens.add(word());
            } else if (Character.isDigit(currentChar)) {
                tokens.add(number());
            } else if (currentChar == '.') {
                tokens.add(handleDot());
                advance();
            } else if (currentChar == '"') {
                Token strToken = stringLiteral();
                if (strToken != null) {
                    tokens.add(strToken);
                }
                if (endOfFile && !errors.isEmpty()) {
                    return tokens; // Parar se um erro fatal foi encontrado
                }
            } else if (currentChar == '{') {
                skipComment();
                if (!errors.isEmpty()) {
                    break;
                }
            } else if ("+-*/(),;:%^&[]".indexOf(currentChar) != -1) {
                tokens.add(new Token(String.valueOf((char) currentChar), String.valueOf((char) currentChar), lineNumber));
                advance();
            } else if (currentChar == '<' || currentChar == '>' || currentChar == '!') {
                processComparisonOperators(tokens);
            } else if (currentChar == '=') {
                processEquals(tokens);
            } else {
                errors.add("Linha " + lineNumber + ": " + (char) currentChar + " - simbolo nao identificado");
                break; // Interrompe o processamento se um erro não reconhecido ocorrer
            }
        }
        return tokens;
    }

    /*
    Processa operadores de comparação (<, >, !=, <=, >=, <>).
    A função lê um operador inicial e verifica o próximo caractere para determinar
    se forma um operador composto. Os tokens são adicionados à lista de tokens.
    */
    private void processComparisonOperators(List<Token> tokens) throws IOException {
        char currentOperator = (char) currentChar;
        advance();
        if (currentChar == '=' && (currentOperator == '>' || currentOperator == '<' || currentOperator == '!')) {
            tokens.add(new Token(currentOperator + "=", currentOperator + "=", lineNumber));
            advance();
        } else if (currentOperator == '<' && currentChar == '-') {
            tokens.add(new Token("<-", "<-", lineNumber));
            advance();
        } else if (currentOperator == '<' && currentChar == '>') {
            tokens.add(new Token("<>", "<>", lineNumber));
            advance();
        } else {
            tokens.add(new Token(String.valueOf(currentOperator), String.valueOf(currentOperator), lineNumber));
        }
    }

    // Processa o operador de igualdade (=) e o operador de comparação de igualdade (==).
    private void processEquals(List<Token> tokens) throws IOException {
        advance();
        if (currentChar == '=') {
            tokens.add(new Token("==", "==", lineNumber));
            advance();
        } else {
            tokens.add(new Token("=", "=", lineNumber));
        }
    }

    // Reconhece palavras no fluxo de entrada e determina se são palavras reservadas ou identificadores.
    private Token word() throws IOException {
        StringBuilder builder = new StringBuilder();
        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            builder.append((char) currentChar);
            advance();
        }
        String word = builder.toString();
        if (RESERVED_WORDS.contains(word.toLowerCase())) {
            return new Token(word.toLowerCase(), word.toLowerCase(), lineNumber); // Convert to lowercase for consistency
        } else {
            return new Token("IDENT", word, lineNumber); // Treat as identifier
        }
    }

    // Reconhece números inteiros ou reais. Trata casos onde um ponto é parte de um número real ou um operador de intervalo.
    private Token number() throws IOException {
        StringBuilder builder = new StringBuilder();
        boolean isReal = false;
        while (Character.isDigit(currentChar)) {
            builder.append((char) currentChar);
            advance();
            if (currentChar == '.') {
                int lookahead = reader.read();
                if (Character.isDigit(lookahead)) {
                    // Trata como número real
                    isReal = true;
                    builder.append('.');
                    builder.append((char) lookahead);
                    advance();
                } else if (lookahead == '.') {
                    // Operador de intervalo ".."
                    reader.unread(lookahead); // Devolve o segundo '.'
                    break; // Sai do loop, pois trata-se de um operador de intervalo
                } else {
                    // Não é um número real nem um operador de intervalo
                    reader.unread(lookahead);
                    break;
                }
            }
        }
        if (isReal) {
            return new Token("NUM_REAL", builder.toString(), lineNumber);
        } else {
            return new Token("NUM_INT", builder.toString(), lineNumber);
        }
    }

    // Trata o ponto como um operador de intervalo ".." ou como um ponto singular.
    // Se o ponto é seguido por outro ponto, trata como operador de intervalo ".."
    private Token handleDot() throws IOException {
        advance();
        if (currentChar == '.') {
            // Confirma que é o operador ".."
            return new Token("..", "..", lineNumber);
        } else {
            // Não é um operador de intervalo, trata-se apenas de um ponto.
            reader.unread(currentChar); // Devolve o caracter que não faz parte do operador de intervalo
            return new Token(".", ".", lineNumber);
        }
    }

    // Ignora comentários delimitados por chaves {}. Conta o aninhamento de chaves para garantir
    // que todos os comentários sejam fechados corretamente.
    private void skipComment() throws IOException {
        int depth = 1; // Start with a depth of 1 because we're already in a comment
        int startLine = lineNumber;

        while (depth > 0 && !endOfFile) {
            advance();

            if (currentChar == '{') {
                depth++;
                advance();
            } else if (currentChar == '}') {
                depth--;
                advance();
            }
        }
        if (depth > 0) {
            errors.add("Linha " + startLine + ": comentario nao fechado");
        }
    }

    // Reconhece e processa cadeias literais delimitadas por aspas duplas.
    // Se a cadeia não for fechada antes de uma quebra de linha ou fim de arquivo, registra um erro.
    private Token stringLiteral() throws IOException {
        StringBuilder builder = new StringBuilder();
        advance(); // Começa depois da aspa inicial
        while (currentChar != '"' && !endOfFile) {
            if (currentChar == '\n' || currentChar == -1) { // Verifica nova linha ou EOF
                errors.add("Linha " + (lineNumber-1) + ": cadeia literal nao fechada");
                endOfFile = true; // Seta endOfFile para verdadeiro para parar o processamento
                return null; // Não adiciona o token se houver um erro
            }
            builder.append((char) currentChar);
            advance();
        }
        if (currentChar == '"') {
            advance(); // Pula a aspa de fechamento
            return new Token("CADEIA", "\"" + builder.toString() + "\"", lineNumber);
        }
        return null; // Em caso de EOF antes de uma aspa de fechamento
    }
}