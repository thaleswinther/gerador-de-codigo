package br.ufscar.dc.compiladores.geradorC;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.antlr.v4.runtime.Token;

import br.ufscar.dc.compiladores.geradorC.LAParser.Tipo_basicoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Tipo_estendidoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.VariavelContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Parcela_unariaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Termo_logicoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.ExpressaoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Fator_logicoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Parcela_logicaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Expressao_relacionalContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Expressao_aritmeticaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.TermoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.FatorContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.ParcelaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Parcela_nao_unariaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.IdentificadorContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Declaracao_variavelContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.CmdAtribuicaoContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.CmdChamadaContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.Declaracao_globalContext;
import br.ufscar.dc.compiladores.geradorC.LAParser.CorpoContext;
import br.ufscar.dc.compiladores.geradorC.TabelaDeSimbolos.TipoSimbolo;

public class AnalisadorSemantico extends LABaseVisitor<Void> {

    Escopo escopo = new Escopo();
    public List<String> errosSemanticos = new ArrayList<>();

    private void registrarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }
    
    
    public static Boolean isEqual(TipoSimbolo tipo1, TipoSimbolo tipo2) {
        if (tipo1 == tipo2) {
            return true;
        } else if (tipo1 == TipoSimbolo.PONTEIRO || tipo2 == TipoSimbolo.PONTEIRO) {
            return true;
        } else if ((tipo1 == TipoSimbolo.REAL && tipo2 == TipoSimbolo.INTEIRO)
                || (tipo1 == TipoSimbolo.INTEIRO && tipo2 == TipoSimbolo.REAL)) {
            return true;
        } else if ((tipo1 == TipoSimbolo.PONTEIRO && tipo2 == TipoSimbolo.ENDERECO)
                || (tipo1 == TipoSimbolo.ENDERECO && tipo2 == TipoSimbolo.PONTEIRO)) {
            return true;
        }
        return false;
    }


    public static TipoSimbolo getTipoBasico(Tipo_basicoContext contexto) {
        if (contexto.LITERAL() != null) {
            return TipoSimbolo.LITERAL;
        } else if (contexto.INTEIRO() != null) {
            return TipoSimbolo.INTEIRO;
        } else if (contexto.LOGICO() != null) {
            return TipoSimbolo.LOGICO;
        } else if (contexto.REAL() != null) {
            return TipoSimbolo.REAL;
        } else {
            return TipoSimbolo.INVALIDO;
        }
    }

    // Retorna o tipo de variável de um contexto
    public static TipoSimbolo getTipoEstendido(Tipo_estendidoContext contexto, Escopo escopo) {
        if (contexto.PONTEIRO() != null) {
            return TipoSimbolo.PONTEIRO;
        } else if (contexto.IDENT() != null) {
            List<TabelaDeSimbolos> tabelas = escopo.getAllEscopos();

            for (TabelaDeSimbolos tabela : tabelas) {
                if (tabela.simboloExiste(contexto.IDENT().getText())) {
                    return TipoSimbolo.TIPO;
                }
            }
            return TipoSimbolo.INVALIDO;
        } else {
            return getTipoBasico(contexto.tipo_basico());
        }
    }

    public static TipoSimbolo getTipo(LAParser.TipoContext contexto, Escopo escopo) {
        if (contexto.tipo_estendido() != null) {
            return getTipoEstendido(contexto.tipo_estendido(), escopo);
        } else {
            return TipoSimbolo.REGISTRO;
        }
    }

    private TipoSimbolo getTipo(VariavelContext contexto, Escopo escopo) {
        return getTipo(contexto.tipo(), escopo);
    }

    public static TipoSimbolo verificarTipo(Parcela_unariaContext contexto, Escopo escopo) {
        if (contexto.identificador() != null) {
            String nome = contexto.identificador().IDENT(0).getText();

            if (contexto.identificador().PONTO() != null) {
                for (int i = 0; i < contexto.identificador().PONTO().size(); i++) {
                    nome += "." + contexto.identificador().IDENT(i + 1);
                }
            }

            return getTodosEscopos(nome, escopo);
        }

        if (contexto.PONTEIRO() != null) {
            return TipoSimbolo.PONTEIRO;
        }

        if (contexto.NUMERO_INTEIRO() != null) {
            return TipoSimbolo.INTEIRO;
        }

        if (contexto.NUMERO_REAL() != null) {
            return TipoSimbolo.REAL;
        }

        if (contexto.exp_unica != null) {
            return verificarTipo(contexto.exp_unica, escopo);
        }

        if (contexto.cmdChamada() != null) {
            return verificarTipo(contexto.cmdChamada(), escopo);
        }

        return TipoSimbolo.INVALIDO;
    }

    public static TipoSimbolo verificarTipo(CmdChamadaContext contexto, Escopo escopo) {
        String nome = contexto.IDENT().getText();
        return getTodosEscopos(nome, escopo);
    }

    public static TipoSimbolo verificarTipo(Termo_logicoContext contexto, Escopo escopo) {
        TipoSimbolo tipo = verificarTipo(contexto.fator_logico(0), escopo);

        if (tipo != TipoSimbolo.INVALIDO) {
            for (LAParser.Fator_logicoContext fator_logico : contexto.fator_logico()) {
                TipoSimbolo tipoTestado = verificarTipo(fator_logico, escopo);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }

            if (!contexto.operador_logico_e().isEmpty()) {
                return TipoSimbolo.LOGICO;
            }
        }
        return tipo;
    }

    public static TipoSimbolo verificarTipo(ExpressaoContext contexto, Escopo escopo) {
        TipoSimbolo tipo = verificarTipo(contexto.termo_logico(0), escopo);

        if (tipo != TipoSimbolo.INVALIDO) {
            for (Termo_logicoContext termo_logico : contexto.termo_logico()) {
                TipoSimbolo tipoTestado = verificarTipo(termo_logico, escopo);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }

            if (!contexto.operador_logico_ou().isEmpty()) {
                return TipoSimbolo.LOGICO;
            }
        }
        return tipo;
    }

    public static TipoSimbolo verificarTipo(Fator_logicoContext contexto, Escopo escopo) {
        if (contexto.parcela_logica() != null) {
            return verificarTipo(contexto.parcela_logica(), escopo);
        }
        return TipoSimbolo.INVALIDO;
    }

    public static TipoSimbolo verificarTipo(Parcela_logicaContext contexto, Escopo escopo) {
        if (contexto.TRUE() != null || contexto.FALSE() != null) {
            return TipoSimbolo.LOGICO;
        }
        return verificarTipo(contexto.expressao_relacional(), escopo);
    }

    public static TipoSimbolo verificarTipo(Expressao_relacionalContext contexto, Escopo escopo) {
        TipoSimbolo tipo = verificarTipo(contexto.expressao_aritmetica(0), escopo);

        if (tipo != TipoSimbolo.INVALIDO) {
            for (Expressao_aritmeticaContext expressao_aritmetica : contexto.expressao_aritmetica()) {
                TipoSimbolo tipoTestado = verificarTipo(expressao_aritmetica, escopo);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }

            if (contexto.operador_relacional() != null) {
                return TipoSimbolo.LOGICO;
            }
        }
        return tipo;
    }

    public static TipoSimbolo verificarTipo(Expressao_aritmeticaContext contexto, Escopo escopo) {
        TipoSimbolo tipo = verificarTipo(contexto.termo(0), escopo);

        if (tipo != TipoSimbolo.INVALIDO) {
            for (TermoContext termo : contexto.termo()) {
                TipoSimbolo tipoTestado = verificarTipo(termo, escopo);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }
        }
        return tipo;
    }

    public static TipoSimbolo verificarTipo(TermoContext contexto, Escopo escopo) {
        TipoSimbolo tipo = verificarTipo(contexto.fator(0), escopo);

        if (tipo != TipoSimbolo.INVALIDO) {
            for (FatorContext fator : contexto.fator()) {
                TipoSimbolo tipoTestado = verificarTipo(fator, escopo);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }
        }
        return tipo;
    }

    public static TipoSimbolo verificarTipo(FatorContext contexto, Escopo escopo) {
        TipoSimbolo tipo = verificarTipo(contexto.parcela(0), escopo);

        if (tipo == TipoSimbolo.INVALIDO) {
            for (ParcelaContext parcela : contexto.parcela()) {
                TipoSimbolo tipoTestado = verificarTipo(parcela, escopo);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }
        }
        return tipo;
    }

    public static TipoSimbolo verificarTipo(ParcelaContext contexto, Escopo escopo) {
        if (contexto.parcela_unaria() != null) {
            return verificarTipo(contexto.parcela_unaria(), escopo);
        } else if (contexto.parcela_nao_unaria() != null) {
            return verificarTipo(contexto.parcela_nao_unaria(), escopo);
        }
        return TipoSimbolo.INVALIDO;
    }

    public static TipoSimbolo verificarTipo(Parcela_nao_unariaContext contexto, Escopo escopo) {
        TipoSimbolo tipoIdentificador = TipoSimbolo.INVALIDO;

        if (contexto.identificador() != null) {
            tipoIdentificador = verificarTipo(contexto.identificador(), escopo);
        }

        if (contexto.CADEIA() != null) {
            tipoIdentificador = TipoSimbolo.LITERAL;
        }

        if (contexto.ENDERECO() != null) {
            tipoIdentificador = TipoSimbolo.ENDERECO;
        }

        return tipoIdentificador;
    }

    public static TipoSimbolo verificarTipo(IdentificadorContext contexto, Escopo escopo) {

        if (contexto.IDENT() != null){   
            String nome = contexto.IDENT(0).getText();
    
            if (contexto.PONTO() != null){
                for (int i = 0; i < contexto.PONTO().size(); i++){
                    nome += "." + contexto.IDENT(i+1);
                }
            }
            return getTodosEscopos(nome, escopo);
        }
        return TipoSimbolo.INVALIDO;
    }

    private void processarParametro(TabelaDeSimbolos tabela, LAParser.ParametroContext param, TipoSimbolo tipoIdent, Escopo escopo) {
        param.identificador().forEach(ident -> {
            String nomeIdent = ident.IDENT(0).getText();

            if (tabela.simboloExiste(ident.getText()) || tabela.simboloExiste(nomeIdent)) {
                registrarErroSemantico(ident.start, "identificador " + nomeIdent + " ja declarado anteriormente");
            } else {
                if (tipoIdent == TipoSimbolo.TIPO) {
                    TabelaDeSimbolos detalhesTipo = getEstrutura(param.tipo_estendido().IDENT().getText(), escopo);
                    tabela.inserirEstrutura(nomeIdent, detalhesTipo);
                } else {
                    tabela.adicionarSimbolo(nomeIdent, tipoIdent);
                }
            }
        });
    }

    private void registrarVariaveis(TabelaDeSimbolos tabela, VariavelContext contexto, Escopo escopo) {
        TipoSimbolo tipo = getTipo(contexto, escopo);

        if (tipo == TipoSimbolo.INVALIDO) {
            registrarErroSemantico(contexto.tipo().start, "tipo " + contexto.tipo().getText() + " nao declarado");
        }
        contexto.identificador().forEach(ident -> {
            if (tabela.simboloExiste(ident.getText()) || tabela.simboloExiste(ident.IDENT(0).getText())) {
                registrarErroSemantico(ident.start,
                        "identificador " + ident.IDENT(0).getText() + " ja declarado anteriormente");
            } else {
                switch (tipo) {
                    case REGISTRO:
                        adicionarRegistro(contexto.tipo().estrutura(), ident.getText(), false, escopo);
                        break;
                    case TIPO:
                        TabelaDeSimbolos detalhes = getEstrutura(contexto.tipo().tipo_estendido().IDENT().getText(), escopo);
                        tabela.inserirEstrutura(ident.IDENT(0).getText(), detalhes);
                        break;

                    default:
                        tabela.adicionarSimbolo(ident.IDENT(0).getText(), tipo);
                        break;
                }
            }
        });
    }

    private void adicionarRegistro(LAParser.EstruturaContext contexto, String nome, boolean isType, Escopo escopo) {
        TabelaDeSimbolos tabelaAtual = escopo.getEscopoAtual();
        TabelaDeSimbolos detalhes = new TabelaDeSimbolos();

        contexto.variavel().forEach(variavel -> registrarVariaveis(detalhes, variavel, escopo));

        Consumer<TabelaDeSimbolos> acao = isType
            ? tipo -> tabelaAtual.definirTipo(nome, tipo)
            : estrutura -> tabelaAtual.inserirEstrutura(nome, estrutura);

        acao.accept(detalhes);
    }

    private TabelaDeSimbolos getEstrutura(String nomeRegistro, Escopo escopo) {
        LinkedList<TabelaDeSimbolos> todosEscopos = escopo.getAllEscopos();

        for (TabelaDeSimbolos tabela : todosEscopos) {
            if (tabela.simboloExiste(nomeRegistro)) {
                return tabela.getEstrutura(nomeRegistro);
            }
        }
        return null;
    }

    private Boolean existeSimbolo(IdentificadorContext contexto, Escopo escopo) {
        LinkedList<TabelaDeSimbolos> tabelas = escopo.getAllEscopos();
        String nome = contexto.IDENT().get(0).getText();
        boolean existe = false;

        for (int i = 0; i < contexto.PONTO().size(); i++) {
            nome += "." + contexto.IDENT(i + 1);
        }

        for (TabelaDeSimbolos tabela : tabelas) {
            if (tabela.simboloExiste(nome)) {
                existe = true;
                break;
            }
        }
        return existe;
    }

    public static TipoSimbolo getTodosEscopos(String nome, Escopo escopo) {
        LinkedList<TabelaDeSimbolos> tabelas = escopo.getAllEscopos();

        for (TabelaDeSimbolos tabela : tabelas) {
            if (tabela.simboloExiste(nome)) {
                return tabela.getTipo(nome);
            }
        }
        return TipoSimbolo.INVALIDO;
    }

    @Override
    public Void visitDeclaracoes(DeclaracoesContext contexto) {
        escopo.criarNovoEscopo();
        return super.visitDeclaracoes(contexto);
    }

    @Override
    public Void visitDeclaracao_variavel(Declaracao_variavelContext contexto) {
        TabelaDeSimbolos tabelaAtual = escopo.getEscopoAtual();

        if (contexto.DECLARE() != null) {
            registrarVariaveis(tabelaAtual, contexto.variavel(), escopo);
        }
        if (contexto.TIPO() != null) {
            adicionarRegistro(contexto.estrutura(), contexto.IDENT().getText(), true, escopo);
        }
        if (contexto.CONSTANTE() != null) {
            TipoSimbolo tipoConstante = getTipoBasico(contexto.tipo_basico());

            tabelaAtual.adicionarSimbolo(contexto.IDENT().getText(), tipoConstante);
        }
        return super.visitDeclaracao_variavel(contexto);
    }

    @Override
    public Void visitIdentificador(IdentificadorContext contexto) {
        StringBuilder nomeBuilder = new StringBuilder(contexto.IDENT(0).getText());

        contexto.PONTO().forEach(ponto -> {
            int index = contexto.PONTO().indexOf(ponto) + 1;
            nomeBuilder.append(".").append(contexto.IDENT(index).getText());
        });

        String nomeCompleto = nomeBuilder.toString();
        Boolean existeIdentificador = existeSimbolo(contexto, escopo);

        boolean erroSemantico = !existeIdentificador &&
                                !(contexto.parent.parent instanceof LAParser.EstruturaContext) &&
                                !(contexto.parent.parent.parent instanceof LAParser.Declaracao_globalContext);

        if (erroSemantico) {
            registrarErroSemantico(contexto.start, "identificador " + nomeCompleto + " nao declarado");
        }

        return super.visitIdentificador(contexto);
    }

    @Override
    public Void visitCmdAtribuicao(CmdAtribuicaoContext contexto) {
        if (existeSimbolo(contexto.identificador(), escopo)) {
            String nome = contexto.identificador().IDENT(0).getText();

            if (contexto.identificador().PONTO() != null) {
                for (int i = 0; i < contexto.identificador().PONTO().size(); i++) {
                    nome += "." + contexto.identificador().IDENT(i + 1);
                }
            }

            TipoSimbolo tipo = getTodosEscopos(nome, escopo);

            if (contexto.PONTEIRO() != null) {
                tipo = TipoSimbolo.PONTEIRO;
            }

            if (tipo == TipoSimbolo.INVALIDO) {
                registrarErroSemantico(contexto.start, "identificador " + nome + " com tipo invalido");
            } else {
                TipoSimbolo tipoExpressao = verificarTipo(contexto.expressao(), escopo);

                if (tipoExpressao == TipoSimbolo.INVALIDO
                        || !isEqual(tipoExpressao, tipo)) {
                    String prefixo = "";

                    if (contexto.PONTEIRO() != null) {
                        prefixo = contexto.PONTEIRO().getText();
                    }

                    registrarErroSemantico(contexto.start,
                            "atribuicao nao compativel para " + prefixo + contexto.identificador().getText());
                }
            }
        }
        return super.visitCmdAtribuicao(contexto);
    }

    @Override
    public Void visitCmdChamada(CmdChamadaContext contexto) {
        String nomeChamado = contexto.IDENT().getText();
        TabelaDeSimbolos detalhesParametros = getEstrutura(nomeChamado, escopo);
        int tamanhoParametros = detalhesParametros.tamanho();
        int tamanhoExpressoes = contexto.expressao().size();

        if (tamanhoParametros != tamanhoExpressoes) {
            registrarErroSemantico(contexto.start, "incompatibilidade de parametros na chamada de " + nomeChamado);
        } else {
            for (int i = 0; i < tamanhoParametros; i++) {
                if (detalhesParametros.getTipoPorIndice(i) != verificarTipo(contexto.expressao(i), escopo)) {
                    registrarErroSemantico(contexto.start, "incompatibilidade de parametros na chamada de " + nomeChamado);
                    break;
                }
            }
        }

        return super.visitCmdChamada(contexto);
    }

    @Override
    public Void visitDeclaracao_global(Declaracao_globalContext contexto) {
        TabelaDeSimbolos tabelaForaFuncao = escopo.getEscopoAtual();
        String nome = contexto.IDENT().getText();
        TipoSimbolo tipoDeclarado = determinarTipoDeclarado(contexto, escopo);

        if (tabelaForaFuncao.simboloExiste(nome)) {
            registrarErroSemantico(contexto.start, "ja declarado");
            return null;
        }

        escopo.criarNovoEscopo();
        TabelaDeSimbolos tabelaDentroFuncao = escopo.getEscopoAtual();
        TabelaDeSimbolos detalhesParametros = processarParametros(contexto, tabelaDentroFuncao, escopo);

        tabelaForaFuncao.inserir(nome, tipoDeclarado, detalhesParametros);

        super.visitDeclaracao_global(contexto);

        validarComandos(contexto, tipoDeclarado);

        escopo.removerEscopo();
        return null;
    }

    private TipoSimbolo determinarTipoDeclarado(Declaracao_globalContext contexto, Escopo escopo) {
        return (contexto.FUNCAO() != null) ? getTipoEstendido(contexto.tipo_estendido(), escopo) : TipoSimbolo.PROCEDIMENTO;
    }

    private TabelaDeSimbolos processarParametros(Declaracao_globalContext contexto, TabelaDeSimbolos tabelaDentroFuncao, Escopo escopo) {
        TabelaDeSimbolos detalhesParametros = new TabelaDeSimbolos();

        if (contexto.parametros() != null) {
            for (LAParser.ParametroContext parametro : contexto.parametros().parametro()) {
                TipoSimbolo tipoParametro = getTipoEstendido(parametro.tipo_estendido(), escopo);
                processarParametro(tabelaDentroFuncao, parametro, tipoParametro, escopo);
                processarParametro(detalhesParametros, parametro, tipoParametro, escopo);
            }
        }

        return detalhesParametros;
    }

    private void validarComandos(Declaracao_globalContext contexto, TipoSimbolo tipoDeclarado) {
        for (LAParser.ComandoContext comando : contexto.comando()) {
            if (comando.cmdRetorno() != null && tipoDeclarado == TipoSimbolo.PROCEDIMENTO) {
                registrarErroSemantico(comando.start, "comando retorne nao permitido nesse escopo");
            }
        }
    }

    @Override
    public Void visitCorpo(CorpoContext contexto) {
        for (LAParser.ComandoContext comando : contexto.comando()) {
            if (comando.cmdRetorno() != null) {
                registrarErroSemantico(comando.start, "comando retorne nao permitido nesse escopo");
            }
        }
        return super.visitCorpo(contexto);
    }
}
