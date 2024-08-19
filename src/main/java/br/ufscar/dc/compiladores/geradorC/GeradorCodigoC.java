package br.ufscar.dc.compiladores.geradorC;

import java.util.ArrayList;
import java.util.Iterator;
import br.ufscar.dc.compiladores.geradorC.LAParser.CmdChamadaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.ComandoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.CmdEnquantoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.CmdFacaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.CmdParaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.CmdRetornoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Declaracao_globalContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Expressao_aritmeticaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.ExpressaoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.FatorContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Intervalo_numericoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.ParcelaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Parcela_unariaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.TermoContext;
import br.ufscar.dc.compiladores.geradorC.TabelaDeSimbolos.TipoSimbolo;
import java.util.List;
import java.util.function.Consumer;

public class GeradorCodigoC extends LABaseVisitor<Void> {
    public StringBuilder saida;
    private final Escopo escopo;
    private static final ArrayList<String> listaVariaveis = new ArrayList<>();

    public GeradorCodigoC() {
        saida = new StringBuilder();
        this.escopo = new Escopo();
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext contexto) {
        inicializarSaida();
        criarEscopoGlobal();

        processarDeclaracoes(contexto);
        processarMain(contexto);

        return null;
    }

    private void inicializarSaida() {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");
    }

    private void criarEscopoGlobal() {
        escopo.criarNovoEscopo();
    }

    private void processarDeclaracoes(LAParser.ProgramaContext contexto) {
        contexto.declaracoes().declaracao_variavel().forEach(this::visitDeclaracao_variavel);
        contexto.declaracoes().declaracao_global().forEach(this::visitDeclaracao_global);
    }

    private void processarMain(LAParser.ProgramaContext contexto) {
        saida.append("\nint main() {\n");
        escopo.criarNovoEscopo();

        contexto.corpo().declaracao_variavel().forEach(this::visitDeclaracao_variavel);
        contexto.corpo().comando().forEach(this::visitComando);

        saida.append("\treturn 0;\n}\n");
    }


    @Override
    public Void visitDeclaracao_global(Declaracao_globalContext contexto) {
        // Determina o tipo de retorno baseado na presença de FUNCAO
        boolean isFuncao = contexto.FUNCAO() != null;
        TipoSimbolo tipoRetorno = isFuncao 
            ? AnalisadorSemantico.getTipoEstendido(contexto.tipo_estendido(), escopo)
            : TipoSimbolo.PROCEDIMENTO;

        // Adiciona o tipo de retorno ao código de saída
        saida.append(isFuncao ? converterTipoParaC(tipoRetorno) + " " : "void ");
        saida.append(contexto.IDENT().getText()).append(" (");

        if (contexto.parametros() != null) {
            var parametro = contexto.parametros().parametro(0);
            var tipoParametro = AnalisadorSemantico.getTipoEstendido(parametro.tipo_estendido(), escopo);
            var nomeParametro = parametro.identificador(0).getText();

            saida.append(converterTipoParaC(tipoParametro));
            if (tipoParametro == TipoSimbolo.LITERAL) {
                saida.append("*");
            }
            saida.append(" ").append(nomeParametro);

            // Adiciona o parâmetro ao escopo atual
            TabelaDeSimbolos parametros = new TabelaDeSimbolos();
            parametros.adicionarSimbolo(nomeParametro, tipoParametro);
            escopo.getEscopoAtual().inserir(contexto.IDENT().getText(), tipoRetorno, parametros);
        }

        saida.append("){\n");

        // Cria um novo escopo e adiciona parâmetros ao novo escopo, se houver
        escopo.criarNovoEscopo();
        if (contexto.parametros() != null) {
            var parametro = contexto.parametros().parametro(0);
            var nomeParametro = parametro.identificador(0).getText();
            var tipoParametro = AnalisadorSemantico.getTipoEstendido(parametro.tipo_estendido(), escopo);
            escopo.getEscopoAtual().adicionarSimbolo(nomeParametro, tipoParametro);
        }

        // Visita as declarações de variáveis e comandos
        contexto.declaracao_variavel().forEach(this::visitDeclaracao_variavel);
        contexto.comando().forEach(this::visitComando);

        saida.append("}\n");

        return null;
    }



    @Override
    public Void visitDeclaracao_variavel(LAParser.Declaracao_variavelContext contexto) {
        // Trata o caso de declaração de variável
        if (contexto.DECLARE() != null) {
            visitVariavel(contexto.variavel());
            return null;
        }

        // Trata o caso de declaração de constante
        if (contexto.CONSTANTE() != null) {
            saida.append("#define ")
                 .append(contexto.IDENT().getText())
                 .append(" ")
                 .append(contexto.tipo_constante().getText())
                 .append("\n");
            return null;
        }

        // Trata o caso de declaração de novo tipo
        if (contexto.TIPO() != null) {
            escopo.criarNovoEscopo();
            saida.append("\ttypedef struct{\n");
            visitEstrutura(contexto.estrutura());
            saida.append("\t} ")
                 .append(contexto.IDENT().getText())
                 .append(";\n");

            escopo.getEscopoAtual().definirTipo(contexto.IDENT().getText(), escopo.getEscopoAtual());
            return null;
        }

        return null;
    }


    @Override
    public Void visitVariavel(LAParser.VariavelContext contexto) {
        TipoSimbolo tipo = AnalisadorSemantico.getTipo(contexto.tipo(), escopo);

        // Se o tipo é um registro ou um tipo definido pelo usuário
        if (tipo == TipoSimbolo.REGISTRO || tipo == TipoSimbolo.TIPO) {
            processaRegistroOuTipoDefinido(contexto, tipo);
        } else {
            // Processa tipos básicos e ponteiros
            processaTiposBasicosEPonteiros(contexto, tipo);
        }

        saida.append(";\n");
        return null;
    }

    private void processaRegistroOuTipoDefinido(LAParser.VariavelContext contexto, TipoSimbolo tipo) {
        if (tipo == TipoSimbolo.REGISTRO) {
            escopo.criarNovoEscopo();
            saida.append("\tstruct {\n");
            visitEstrutura(contexto.tipo().estrutura());
            saida.append("\t} ").append(contexto.identificador(0).getText());

            TabelaDeSimbolos dadosRegistro = escopo.getEscopoAtual();
            escopo.getEscopoAtual().inserirEstrutura(contexto.identificador(0).getText(), dadosRegistro);
        } else { // Tipo definido pelo usuário
            String nomeTipo = contexto.tipo().getText();
            TabelaDeSimbolos dadosRegistro = escopo.getEscopoAtual().getEstrutura(nomeTipo);

            saida.append("\t").append(nomeTipo)
                 .append(" ").append(contexto.identificador(0).getText());
            escopo.getEscopoAtual().inserirEstrutura(contexto.identificador(0).getText(), dadosRegistro);

            for (int i = 1; i < contexto.identificador().size(); i++) {
                saida.append(", ").append(contexto.identificador(i).getText());
                escopo.getEscopoAtual().inserirEstrutura(contexto.identificador(i).getText(), dadosRegistro);
            }
        }
    }

    private void processaTiposBasicosEPonteiros(LAParser.VariavelContext contexto, TipoSimbolo tipo) {
        String strCtipo = converterTipoParaC(tipo);

        if (tipo == TipoSimbolo.PONTEIRO) {
            TipoSimbolo tipoSemPonteiro = converterStringParaTipoSimbolo(contexto.tipo().getText().replace("^", ""));
            strCtipo = converterTipoParaC(tipoSemPonteiro);
        }

        saida.append("\t").append(strCtipo);

        if (tipo == TipoSimbolo.PONTEIRO) {
            saida.append("*");
        }

        saida.append(" ").append(contexto.identificador(0).getText());
        escopo.getEscopoAtual().adicionarSimbolo(contexto.identificador(0).IDENT(0).getText(), tipo);

        if (tipo == TipoSimbolo.LITERAL) {
            saida.append("[80]");
        }

        for (int i = 1; i < contexto.identificador().size(); i++) {
            saida.append(", ").append(contexto.identificador(i).getText());
            escopo.getEscopoAtual().adicionarSimbolo(contexto.identificador(i).IDENT(0).getText(), tipo);

            if (tipo == TipoSimbolo.LITERAL) {
                saida.append("[80]");
            }
        }
    }


    @Override
    public Void visitEstrutura(LAParser.EstruturaContext contexto) {
        contexto.variavel().forEach(var -> {
            saida.append("\t");
            visitVariavel(var);
        });
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext contexto) {
        TipoSimbolo tipo = AnalisadorSemantico.verificarTipo(contexto.identificador(), escopo);

        saida.append("\t");

        boolean isLiteral = tipo == TipoSimbolo.LITERAL;
        boolean isPonteiro = contexto.PONTEIRO() != null;

        if (isLiteral) {
            saida.append("strcpy(")
                 .append(contexto.identificador().getText())
                 .append(", ");
        } else {
            if (isPonteiro) {
                saida.append("*");
            }
            saida.append(contexto.identificador().getText())
                 .append(" = ");
        }

        visitExpressao(contexto.expressao());

        if (isLiteral) {
            saida.append(")");
        }

        saida.append(";\n");
        return null;
    }


    @Override
    public Void visitCmdSe(LAParser.CmdSeContext contexto) {
        saida.append("\tif (");
        visitExpressao(contexto.expressao());
        saida.append(") {\n");

        // Processa comandos dentro do bloco if
        processaComandos(contexto.cmdIf);

        saida.append("\t}\n");

        // Verifica e processa o bloco else, se presente
        if (contexto.ELSE() != null) {
            saida.append("\telse {\n");
            processaComandos(contexto.cmdElse);
            saida.append("\t}\n");
        }

        return null;
    }

    // Método auxiliar para processar comandos dentro de um bloco de código
    private void processaComandos(List<LAParser.ComandoContext> comandos) {
        for (LAParser.ComandoContext cmdContexto : comandos) {
            saida.append("\t");
            visitComando(cmdContexto);
        }
    }


    @Override
    public Void visitOperador_relacional(LAParser.Operador_relacionalContext contexto) {
        switch (contexto.getStart().getType()) {
            case LAParser.IGUAL:
                saida.append(" == ");
                break;
            case LAParser.DIFERENTE:
                saida.append(" != ");
                break;
            case LAParser.MAIORIGUAL:
                saida.append(" >= ");
                break;
            case LAParser.MENORIGUAL:
                saida.append(" <= ");
                break;
            case LAParser.MAIOR:
                saida.append(" > ");
                break;
            case LAParser.MENOR:
                saida.append(" < ");
                break;
            default:
                break;
        }
        return null;
    }


    @Override
    public Void visitOperador_logico_ou(LAParser.Operador_logico_ouContext contexto) {
        saida.append(" || ");
        return null;
    }

    @Override
    public Void visitOperador_logico_e(LAParser.Operador_logico_eContext contexto) {
        saida.append(" && ");
        return null;
    }

    @Override
    public Void visitCmdLeitura(LAParser.CmdLeituraContext contexto) {
        for (LAParser.IdentificadorContext identificador : contexto.identificador()) {
            String nomeVar = identificador.getText();
            TipoSimbolo tipoVar = escopo.getEscopoAtual().getTipo(nomeVar);
            String formatString = converterTipoParaFormatString(tipoVar);

            if (formatString.equals("%s")) {
                formatString = "%[^\\n]";
            } else {
                nomeVar = "&" + nomeVar;
            }

            saida.append("\tscanf(\"")
                 .append(formatString)
                 .append("\", ")
                 .append(nomeVar)
                 .append(");\n\n");
        }

        return null;
    }


    @Override
    public Void visitExpressao(LAParser.ExpressaoContext contexto) {
        return visitarElementosLogicos(
            contexto.termo_logico(), 
            contexto.operador_logico_ou(), 
            this::visitTermo_logico, 
            this::visitOperador_logico_ou
        );
    }

    @Override
    public Void visitTermo_logico(LAParser.Termo_logicoContext contexto) {
        return visitarElementosLogicos(
            contexto.fator_logico(), 
            contexto.operador_logico_e(), 
            this::visitFator_logico, 
            this::visitOperador_logico_e
        );
    }

    private <T, O> Void visitarElementosLogicos(
            List<T> elementos, 
            List<O> operadores, 
            Consumer<T> visitElemento, 
            Consumer<O> visitOperador) {

        visitElemento.accept(elementos.get(0)); // Visita o primeiro elemento lógico
        if (operadores != null) {
            for (int i = 0; i < operadores.size(); i++) {
                visitOperador.accept(operadores.get(i)); // Visita o operador lógico
                visitElemento.accept(elementos.get(i + 1)); // Visita o próximo elemento lógico
            }
        }
        return null;
    }

    @Override
    public Void visitFator_logico(LAParser.Fator_logicoContext contexto) {
        if (contexto.NOT() != null) {
            saida.append("!(");
        }
        visitParcela_logica(contexto.parcela_logica());
        if (contexto.NOT() != null) {
            saida.append(")");
        }
        return null;
    }

    @Override
    public Void visitParcela_logica(LAParser.Parcela_logicaContext contexto) {
        if (contexto.TRUE() != null) {
            saida.append("1");
        } else if (contexto.FALSE() != null) {
            saida.append("0");
        } else {
            visitExpressao_relacional(contexto.expressao_relacional());
        }
        return null;
    }


    @Override
    public Void visitExpressao_relacional(LAParser.Expressao_relacionalContext contexto) {
        visitExpressao_aritmetica(contexto.expressao_aritmetica(0));
        if (contexto.operador_relacional() != null) {
            visitOperador_relacional(contexto.operador_relacional());
            visitExpressao_aritmetica(contexto.expressao_aritmetica(1));
        }
        return null;
    }

    @Override
    public Void visitExpressao_aritmetica(Expressao_aritmeticaContext contexto) {
        visitTermo(contexto.termo(0));

        for (int i = 0; i < contexto.operador1().size(); i++) {
            saida.append(contexto.operador1(i).getText());
            visitTermo(contexto.termo(i + 1));
        }

        return null;
    }

    @Override
    public Void visitTermo(TermoContext contexto) {
        visitFator(contexto.fator(0));

        for (int i = 0; i < contexto.operador2().size(); i++) {
            saida.append(contexto.operador2(i).getText());
            visitFator(contexto.fator(i + 1));
        }

        return null;
    }

    @Override
    public Void visitFator(FatorContext contexto) {
        visitParcela(contexto.parcela(0));

        for (int i = 0; i < contexto.operador3().size(); i++) {
            saida.append(contexto.operador3(i).getText());
            visitParcela(contexto.parcela(i + 1));
        }

        return null;
    }

    @Override
    public Void visitParcela(ParcelaContext contexto) {
        if (contexto.operador_unario() != null) {
            saida.append(contexto.operador_unario().getText());
        }

        if (contexto.parcela_unaria() != null) {
            visitParcela_unaria(contexto.parcela_unaria());
        } else {
            saida.append(contexto.parcela_nao_unaria().getText());
        }

        return null;
    }

    @Override
    public Void visitParcela_unaria(Parcela_unariaContext contexto) {
        if (contexto.expressao() != null) {
            visitExpressao(contexto.expressao());
        } else {
            saida.append(contexto.getText());
        }

        return null;
    }

    @Override
    public Void visitCmdEscrita(LAParser.CmdEscritaContext contexto) {
        saida.append("\tprintf(\"");

        for (ExpressaoContext expressao : contexto.expressao()) {
            TipoSimbolo tipoExpressao = AnalisadorSemantico.verificarTipo(expressao, escopo);

            if (expressao.getText().contains("\"")) {
                saida.append(expressao.getText().replace("\"", ""));
            } else {
                adicionarNomeVariavel(expressao.getText());
                String formatoString = converterTipoParaFormatString(tipoExpressao);
                saida.append(formatoString);
            }
        }

        saida.append("\"");

        Iterator<String> itLista = obterIteradorNomesVariaveis();

        if (itLista.hasNext()) {
            saida.append(", ");

            while (itLista.hasNext()) {
                saida.append(itLista.next());

                if (itLista.hasNext()) {
                    saida.append(", ");
                }
            }
        }

        saida.append(");\n");

        limparNomesVariaveis();

        return null;
    }

    @Override
    public Void visitCmdCaso(LAParser.CmdCasoContext contexto) {
        saida.append("\tswitch (").append(contexto.expressao_aritmetica().getText()).append(")\n\t{\n");
        visitSelecao(contexto.selecao());
        if (contexto.ELSE() != null) {
            saida.append("\t\tdefault: \n");
            for (int i = 0; i < contexto.comando().size(); i++) {
                saida.append("\t");
                visitComando(contexto.comando(i));
            }
        }
        saida.append("\t}\n");
        return null;
    }

    @Override
    public Void visitSelecao(LAParser.SelecaoContext contexto) {
        contexto.item_selecao().forEach(item -> visitItem_selecao(item));
        return null;
    }

    @Override
    public Void visitItem_selecao(LAParser.Item_selecaoContext contexto) {
        for (Intervalo_numericoContext intervalo : contexto.constantes().intervalo_numerico()) {
            int inicio = parseIntervalo(intervalo.inicio.getText(), intervalo.operador_inicio != null);
            int fim = intervalo.fim != null ? parseIntervalo(intervalo.fim.getText(), intervalo.operador_fim != null) : inicio;

            for (int i = inicio; i <= fim; i++) {
                saida.append("\t\tcase ").append(i).append(":\n");
            }

            for (LAParser.ComandoContext comando : contexto.comando()) {
                saida.append("\t");
                visitComando(comando);
            }
        }
        saida.append("\t\tbreak;\n\n");
        return null;
    }

    private int parseIntervalo(String valor, boolean negativo) {
        int numero = Integer.parseInt(valor);
        return negativo ? -numero : numero;
    }


    @Override
    public Void visitCmdPara(CmdParaContext contexto) {
        saida.append("\n\tfor (").append(contexto.IDENT().getText()).append(" = ").append(contexto.inicio.getText()).append("; ").append(contexto.IDENT().getText()).append(" <= ").append(contexto.fim.getText()).append("; ").append(contexto.IDENT().getText()).append("++) {\n");

        for (ComandoContext cmdContexto : contexto.comando()) {
            saida.append("\t");
            visitComando(cmdContexto);
        }

        saida.append("\t}\n\n");

        return null;
    }

    @Override
    public Void visitCmdEnquanto(CmdEnquantoContext contexto) {
        saida.append("\n\twhile (");
        visitExpressao(contexto.expressao());
        saida.append(") {\n");

        for (ComandoContext cmdContexto : contexto.comando()) {
            saida.append("\t");
            visitComando(cmdContexto);
        }

        saida.append("\t}\n\n");

        return null;
    }

    @Override
    public Void visitCmdFaca(CmdFacaContext contexto) {
        saida.append("\n\tdo {\n");

        for (ComandoContext cmdContexto : contexto.comando()) {
            saida.append("\t");
            visitComando(cmdContexto);
        }

        saida.append("\t} while (");
        visitExpressao(contexto.expressao());
        saida.append(");\n\n");

        return null;
    }

    @Override
    public Void visitCmdChamada(CmdChamadaContext contexto) {
        saida.append("\t" + contexto.IDENT().getText() + "(");

        visitExpressao(contexto.expressao(0));

        saida.append(");\n");

        return null;
    }

    @Override
    public Void visitCmdRetorno(CmdRetornoContext contexto) {
        saida.append("\treturn ");
        visitExpressao(contexto.expressao());
        saida.append(";\n");

        return null;
    }


    public static Void adicionarNomeVariavel(String nomeVariavel) {
        listaVariaveis.add(nomeVariavel);
        return null;
    }


    public static Void limparNomesVariaveis() {
        listaVariaveis.clear();
        return null;
    }


    public static Iterator<String> obterIteradorNomesVariaveis() {
        return listaVariaveis.iterator();
    }


    public static String converterTipoParaC(TipoSimbolo tipo) {
        switch (tipo) {
            case LITERAL:
                return "char";
            case INTEIRO:
                return "int";
            case REAL:
                return "float";
            case LOGICO:
                return "int";
            default:
                return "";
        }
    }


    public static TipoSimbolo converterStringParaTipoSimbolo(String strLA) {
        switch (strLA) {
            case "literal":
                return TipoSimbolo.LITERAL;
            case "inteiro":
                return TipoSimbolo.INTEIRO;
            case "real":
                return TipoSimbolo.REAL;
            case "logico":
                return TipoSimbolo.LOGICO;
            case "registro":
                return TipoSimbolo.REGISTRO;
            default:
                return TipoSimbolo.INVALIDO;
        }
    }


    public static String converterTipoParaFormatString(TipoSimbolo tipo) {
        switch (tipo) {
            case INTEIRO:
                return "%d";
            case REAL:
                return "%f";
            case LITERAL:
                return "%s";
            default:
                System.out.println("Erro ao converter tipo \"" + tipo + "\" para Format String");
                return "ERR";
        }
    }

}
