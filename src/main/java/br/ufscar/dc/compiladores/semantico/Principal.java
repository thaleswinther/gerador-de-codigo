package br.ufscar.dc.compiladores.semantico;

import org.antlr.v4.runtime.*;
import java.io.FileWriter;
import java.io.IOException;
import br.ufscar.dc.compiladores.semantico.LAParser.ProgramaContext;

public class Principal {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Erro: Argumentos insuficientes. Forneça o arquivo de entrada e o arquivo de saída.");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];
        processarArquivo(inputFile, outputFile);
    }

    public static void processarArquivo(String inputFile, String outputFile) {
        try {
            CharStream stream = CharStreams.fromFileName(inputFile);
            LALexer lexer = new LALexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LAParser parser = new LAParser(tokens);
            parser.setBuildParseTree(true); // garantir a construção de uma árvore de parse

            try (FileWriter writer = new FileWriter(outputFile)) {
                CustomSyntaxErrorListener errorListener = new CustomSyntaxErrorListener(writer);
                parser.removeErrorListeners(); // remove os ouvintes de erro padrão
                parser.addErrorListener(errorListener); // adiciona o ouvinte de erro personalizado
                
                ProgramaContext arvore = parser.programa(); // método de entrada da gramática

                // Realizar a análise semântica
                
                if (!errorListener.hasError()) {
                    AnalisadorSemantico analisadorSemantico = new AnalisadorSemantico();
                    analisadorSemantico.visitPrograma(arvore);
                    for (String erro : AnalisadorSemantico.errosSemanticos) {
                        writer.write(erro + "\n");
                    }
                }

                writer.write("Fim da compilacao\n");
            }
        } catch (IOException e) {
            System.err.println("Erro ao acessar arquivos: " + e.getMessage());
        }
    }

    // Classe interna para personalizar como os erros são reportados
    private static class CustomSyntaxErrorListener extends BaseErrorListener {
        private final FileWriter writer;
        private boolean error;

        public CustomSyntaxErrorListener(FileWriter writer) {
            this.writer = writer;
            this.error = false;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object TokenDeErro, int linha, int posicao, String msg, RecognitionException e) {
            if (error) return; // ignora erros após o primeiro

            try {
                Token token = (Token) TokenDeErro;
                String nomeToken = LALexer.VOCABULARY.getDisplayName(token.getType());
                
                switch (nomeToken) {
                    case "ERRO_SIMBOLO_INVALIDO":
                        writer.write("Linha " + linha + ": " + token.getText() + " - simbolo nao identificado\n");
                        break;
                    case "ERRO_COMENTARIO":
                        writer.write("Linha " + linha + ": comentario nao fechado\n");
                        break;
                    case "ERRO_CADEIA":
                        writer.write("Linha " + linha + ": cadeia literal nao fechada\n");
                        break;
                    case "EOF":
                        writer.write("Linha " + linha + ": erro sintatico proximo a EOF\n");
                        break;
                    default:
                        writer.write("Linha " + linha + ": erro sintatico proximo a " + token.getText() + "\n");
                }
                writer.write("Fim da compilacao\n");
                error = true;
            } catch (IOException ex) {
                System.err.println("Erro ao escrever no arquivo: " + ex.getMessage());
            }
        }

        public boolean hasError() {
            return error;
        }
    }
}
