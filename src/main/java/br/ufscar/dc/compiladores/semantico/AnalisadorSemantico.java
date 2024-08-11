package br.ufscar.dc.compiladores.semantico;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import br.ufscar.dc.compiladores.semantico.LAParser.Tipo_basicoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Tipo_estendidoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.VariavelContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Parcela_unariaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Termo_logicoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.ExpressaoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Fator_logicoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Parcela_logicaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Expressao_relacionalContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Expressao_aritmeticaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.TermoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.FatorContext;
import br.ufscar.dc.compiladores.semantico.LAParser.ParcelaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Parcela_nao_unariaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.IdentificadorContext;
import br.ufscar.dc.compiladores.semantico.LAParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Declaracao_variavelContext;
import br.ufscar.dc.compiladores.semantico.LAParser.CmdAtribuicaoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.CmdChamadaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Declaracao_globalContext;
import br.ufscar.dc.compiladores.semantico.LAParser.CorpoContext;
import br.ufscar.dc.compiladores.semantico.TabelaDeSimbolos.TipoSimbolo;
import java.util.function.Consumer;

public class AnalisadorSemantico extends LABaseVisitor<Void> {

    Escopo escopo = new Escopo();
    public List<String> errosSemanticos = new ArrayList<>();

    // Registra um erro semântico na lista de erros
    private void registrarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    // Verifica se dois tipos são compatíveis
    private Boolean isEqual(TipoSimbolo tipo1, TipoSimbolo tipo2) {
        if (tipo1 == tipo2) {
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

    // Retorna o tipo básico de um contexto
    private TipoSimbolo getTipoBasico(Tipo_basicoContext contexto) {
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
    private TipoSimbolo getTipoEstendido(Tipo_estendidoContext contexto) {
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

    // Retorna o tipo de um contexto
    private TipoSimbolo getTipo(LAParser.TipoContext contexto) {
        if (contexto.tipo_estendido() != null) {
            return getTipoEstendido(contexto.tipo_estendido());
        } else {
            return TipoSimbolo.REGISTRO;
        }
    }

    // Retorna o tipo de uma variável
    private TipoSimbolo getTipo(VariavelContext contexto) {
        return getTipo(contexto.tipo());
    }

    // Verifica o tipo de uma parcela unária
    private TipoSimbolo verificarTipo(Parcela_unariaContext contexto) {
        if (contexto.identificador() != null) {
            String nome = contexto.identificador().IDENT(0).getText();

            if (contexto.identificador().PONTO() != null) {
                for (int i = 0; i < contexto.identificador().PONTO().size(); i++) {
                    nome += "." + contexto.identificador().IDENT(i + 1);
                }
            }

            return getTodosEscopos(nome);
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
            return verificarTipo(contexto.exp_unica);
        }

        if (contexto.cmdChamada() != null) {
            return verificarTipo(contexto.cmdChamada());
        }

        return TipoSimbolo.INVALIDO;
    }

    // Verifica o tipo de um comando de chamada
    private TipoSimbolo verificarTipo(CmdChamadaContext contexto) {
        String nome = contexto.IDENT().getText();
        return getTodosEscopos(nome);
    }

    // Verifica o tipo de um termo lógico
    private TipoSimbolo verificarTipo(Termo_logicoContext contexto) {
        TipoSimbolo tipo = verificarTipo(contexto.fator_logico(0));

        if (tipo != TipoSimbolo.INVALIDO) {
            for (LAParser.Fator_logicoContext fator_logico : contexto.fator_logico()) {
                TipoSimbolo tipoTestado = verificarTipo(fator_logico);

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

    // Verifica o tipo de uma expressão
    private TipoSimbolo verificarTipo(ExpressaoContext contexto) {
        TipoSimbolo tipo = verificarTipo(contexto.termo_logico(0));

        if (tipo != TipoSimbolo.INVALIDO) {
            for (Termo_logicoContext termo_logico : contexto.termo_logico()) {
                TipoSimbolo tipoTestado = verificarTipo(termo_logico);

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

    // Verifica o tipo de um fator lógico
    private TipoSimbolo verificarTipo(Fator_logicoContext contexto) {
        if (contexto.parcela_logica() != null) {
            return verificarTipo(contexto.parcela_logica());
        }
        return TipoSimbolo.INVALIDO;
    }

    // Verifica o tipo de uma parcela lógica
    private TipoSimbolo verificarTipo(Parcela_logicaContext contexto) {
        if (contexto.TRUE() != null || contexto.FALSE() != null) {
            return TipoSimbolo.LOGICO;
        }
        return verificarTipo(contexto.expressao_relacional());
    }

    // Verifica o tipo de uma expressão relacional
    private TipoSimbolo verificarTipo(Expressao_relacionalContext contexto) {
        TipoSimbolo tipo = verificarTipo(contexto.expressao_aritmetica(0));

        if (tipo != TipoSimbolo.INVALIDO) {
            for (Expressao_aritmeticaContext expressao_aritmetica : contexto.expressao_aritmetica()) {
                TipoSimbolo tipoTestado = verificarTipo(expressao_aritmetica);

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

    // Verifica o tipo de uma expressão aritmética
    private TipoSimbolo verificarTipo(Expressao_aritmeticaContext contexto) {
        TipoSimbolo tipo = verificarTipo(contexto.termo(0));

        if (tipo != TipoSimbolo.INVALIDO) {
            for (TermoContext termo : contexto.termo()) {
                TipoSimbolo tipoTestado = verificarTipo(termo);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }
        }
        return tipo;
    }

    // Verifica o tipo de um termo
    private TipoSimbolo verificarTipo(TermoContext contexto) {
        TipoSimbolo tipo = verificarTipo(contexto.fator(0));

        if (tipo != TipoSimbolo.INVALIDO) {
            for (FatorContext fator : contexto.fator()) {
                TipoSimbolo tipoTestado = verificarTipo(fator);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }
        }
        return tipo;
    }

    // Verifica o tipo de um fator
    private TipoSimbolo verificarTipo(FatorContext contexto) {
        TipoSimbolo tipo = verificarTipo(contexto.parcela(0));

        if (tipo == TipoSimbolo.INVALIDO) {
            for (ParcelaContext parcela : contexto.parcela()) {
                TipoSimbolo tipoTestado = verificarTipo(parcela);

                if (!isEqual(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }
        }
        return tipo;
    }

    // Verifica o tipo de uma parcela
    private TipoSimbolo verificarTipo(ParcelaContext contexto) {
        if (contexto.parcela_unaria() != null) {
            return verificarTipo(contexto.parcela_unaria());
        } else if (contexto.parcela_nao_unaria() != null) {
            return verificarTipo(contexto.parcela_nao_unaria());
        }
        return TipoSimbolo.INVALIDO;
    }

    // Verifica o tipo de uma parcela não unária
    private TipoSimbolo verificarTipo(Parcela_nao_unariaContext contexto) {
        TipoSimbolo tipoIdentificador = TipoSimbolo.INVALIDO;

        if (contexto.identificador() != null) {
            tipoIdentificador = verificarTipo(contexto.identificador());
        }

        if (contexto.CADEIA() != null) {
            tipoIdentificador = TipoSimbolo.LITERAL;
        }

        if (contexto.ENDERECO() != null) {
            tipoIdentificador = TipoSimbolo.ENDERECO;
        }

        return tipoIdentificador;
    }

    // Verifica o tipo de um identificador
    private TipoSimbolo verificarTipo(IdentificadorContext contexto) {

        if (contexto.IDENT() != null) {
            String nome = contexto.IDENT(0).getText();

            if (contexto.PONTO() != null) {
                for (int i = 0; i < contexto.PONTO().size(); i++) {
                    nome += "." + contexto.IDENT(i + 1);
                }
            }
            return getTodosEscopos(nome);
        }
        return TipoSimbolo.INVALIDO;
    }

    // Adiciona um parâmetro na tabela de símbolos
    private void processarParametro(TabelaDeSimbolos tabela, LAParser.ParametroContext param, TipoSimbolo tipoIdent) {
        param.identificador().forEach(ident -> {
            String nomeIdent = ident.IDENT(0).getText();

            if (tabela.simboloExiste(ident.getText()) || tabela.simboloExiste(nomeIdent)) {
                registrarErroSemantico(ident.start, "identificador " + nomeIdent + " ja declarado anteriormente");
            } else {
                if (tipoIdent == TipoSimbolo.TIPO) {
                    TabelaDeSimbolos detalhesTipo = getEstrutura(param.tipo_estendido().IDENT().getText());
                    tabela.inserirEstrutura(nomeIdent, detalhesTipo);
                } else {
                    tabela.adicionarSimbolo(nomeIdent, tipoIdent);
                }
            }
        });
    }

    // Adiciona variáveis na tabela de símbolos
    private void registrarVariaveis(TabelaDeSimbolos tabela, VariavelContext contexto) {
        TipoSimbolo tipo = getTipo(contexto);

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
                        adicionarRegistro(contexto.tipo().estrutura(), ident.getText(), false);
                        break;
                    case TIPO:
                        TabelaDeSimbolos detalhes = getEstrutura(contexto.tipo().tipo_estendido().IDENT().getText());
                        tabela.inserirEstrutura(ident.IDENT(0).getText(), detalhes);
                        break;

                    default:
                        tabela.adicionarSimbolo(ident.IDENT(0).getText(), tipo);
                        break;
                }
            }
        });
    }

    // Adiciona um registro no escopo
    private void adicionarRegistro(LAParser.EstruturaContext contexto, String nome, boolean isType) {
        TabelaDeSimbolos tabelaAtual = escopo.getEscopoAtual();
        TabelaDeSimbolos detalhes = new TabelaDeSimbolos();

        contexto.variavel().forEach(variavel -> registrarVariaveis(detalhes, variavel));

        Consumer<TabelaDeSimbolos> acao = isType
            ? tipo -> tabelaAtual.definirTipo(nome, tipo)
            : estrutura -> tabelaAtual.inserirEstrutura(nome, estrutura);


        acao.accept(detalhes);
    }


    // Recupera a estrutura do tipo de um registro
    private TabelaDeSimbolos getEstrutura(String nomeRegistro) {
        LinkedList<TabelaDeSimbolos> todosEscopos = escopo.getAllEscopos();

        for (TabelaDeSimbolos tabela : todosEscopos) {
            if (tabela.simboloExiste(nomeRegistro)) {
                return tabela.getEstrutura(nomeRegistro);
            }
        }
        return null;
    }

    // Verifica se um identificador existe em todos os escopos
    private Boolean existeSimbolo(IdentificadorContext contexto) {
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

    // Retorna o tipo de um identificador em todos os escopos
    private TipoSimbolo getTodosEscopos(String nome) {
        LinkedList<TabelaDeSimbolos> tabelas = escopo.getAllEscopos();

        for (TabelaDeSimbolos tabela : tabelas) {
            if (tabela.simboloExiste(nome)) {
                return tabela.getTipo(nome);
            }
        }
        return TipoSimbolo.INVALIDO;
    }

    // Cria um novo escopo ao visitar declarações
    @Override
    public Void visitDeclaracoes(DeclaracoesContext contexto) {
        escopo.criarNovoEscopo();
        return super.visitDeclaracoes(contexto);
    }

    // Verifica e adiciona uma declaração de variável ao visitar declaração de variável
    @Override
    public Void visitDeclaracao_variavel(Declaracao_variavelContext contexto) {
        TabelaDeSimbolos tabelaAtual = escopo.getEscopoAtual();

        if (contexto.DECLARE() != null) {
            registrarVariaveis(tabelaAtual, contexto.variavel());
        }
        if (contexto.TIPO() != null) {
            adicionarRegistro(contexto.estrutura(), contexto.IDENT().getText(), true);
        }
        if (contexto.CONSTANTE() != null) {
            TipoSimbolo tipoConstante = getTipoBasico(contexto.tipo_basico());

            tabelaAtual.adicionarSimbolo(contexto.IDENT().getText(), tipoConstante);
        }
        return super.visitDeclaracao_variavel(contexto);
    }

    // Verifica se um identificador está declarado ao visitar um identificador
    @Override
    public Void visitIdentificador(IdentificadorContext contexto) {
        StringBuilder nomeBuilder = new StringBuilder(contexto.IDENT(0).getText());

        contexto.PONTO().forEach(ponto -> {
            int index = contexto.PONTO().indexOf(ponto) + 1;
            nomeBuilder.append(".").append(contexto.IDENT(index).getText());
        });

        String nomeCompleto = nomeBuilder.toString();
        Boolean existeIdentificador = existeSimbolo(contexto);

        boolean erroSemantico = !existeIdentificador &&
                                !(contexto.parent.parent instanceof LAParser.EstruturaContext) &&
                                !(contexto.parent.parent.parent instanceof LAParser.Declaracao_globalContext);

        if (erroSemantico) {
            registrarErroSemantico(contexto.start, "identificador " + nomeCompleto + " nao declarado");
        }

        return super.visitIdentificador(contexto);
    }


    // Verifica a compatibilidade de atribuição ao visitar um comando de atribuição
    @Override
    public Void visitCmdAtribuicao(CmdAtribuicaoContext contexto) {
        if (existeSimbolo(contexto.identificador())) {
            String nome = contexto.identificador().IDENT(0).getText();

            if (contexto.identificador().PONTO() != null) {
                for (int i = 0; i < contexto.identificador().PONTO().size(); i++) {
                    nome += "." + contexto.identificador().IDENT(i + 1);
                }
            }

            TipoSimbolo tipo = getTodosEscopos(nome);

            if (contexto.PONTEIRO() != null) {
                tipo = TipoSimbolo.PONTEIRO;
            }

            if (tipo == TipoSimbolo.INVALIDO) {
                registrarErroSemantico(contexto.start, "identificador " + nome + " com tipo invalido");
            } else {
                TipoSimbolo tipoExpressao = verificarTipo(contexto.expressao());

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

    // Verifica a compatibilidade dos parâmetros ao visitar um comando de chamada
    @Override
    public Void visitCmdChamada(CmdChamadaContext contexto) {
        String nomeChamado = contexto.IDENT().getText();
        TabelaDeSimbolos detalhesParametros = getEstrutura(nomeChamado);
        int tamanhoParametros = detalhesParametros.tamanho();
        int tamanhoExpressoes = contexto.expressao().size();

        if (tamanhoParametros != tamanhoExpressoes) {
            registrarErroSemantico(contexto.start, "incompatibilidade de parametros na chamada de " + nomeChamado);
        } else {
            for (int i = 0; i < tamanhoParametros; i++) {
                if (detalhesParametros.getTipoPorIndice(i) != verificarTipo(contexto.expressao(i))) {
                    registrarErroSemantico(contexto.start, "incompatibilidade de parametros na chamada de " + nomeChamado);
                    break; // Interrompe a verificação após encontrar o primeiro erro
                }
            }
        }

        return super.visitCmdChamada(contexto);
    }


    // Verifica e adiciona uma declaração global ao visitar declaração global
    @Override
    public Void visitDeclaracao_global(Declaracao_globalContext contexto) {
        TabelaDeSimbolos tabelaForaFuncao = escopo.getEscopoAtual();
        String nome = contexto.IDENT().getText();
        TipoSimbolo tipoDeclarado = determinarTipoDeclarado(contexto);

        if (tabelaForaFuncao.simboloExiste(nome)) {
            registrarErroSemantico(contexto.start, "ja declarado");
            return null; // Retorna imediatamente em caso de erro
        }

        escopo.criarNovoEscopo();
        TabelaDeSimbolos tabelaDentroFuncao = escopo.getEscopoAtual();
        TabelaDeSimbolos detalhesParametros = processarParametros(contexto, tabelaDentroFuncao);

        tabelaForaFuncao.inserir(nome, tipoDeclarado, detalhesParametros);

        super.visitDeclaracao_global(contexto);

        validarComandos(contexto, tipoDeclarado);

        escopo.removerEscopo();
        return null;
    }

    private TipoSimbolo determinarTipoDeclarado(Declaracao_globalContext contexto) {
        return (contexto.FUNCAO() != null) ? getTipoEstendido(contexto.tipo_estendido()) : TipoSimbolo.PROCEDIMENTO;
    }

    private TabelaDeSimbolos processarParametros(Declaracao_globalContext contexto, TabelaDeSimbolos tabelaDentroFuncao) {
        TabelaDeSimbolos detalhesParametros = new TabelaDeSimbolos();

        if (contexto.parametros() != null) {
            for (LAParser.ParametroContext parametro : contexto.parametros().parametro()) {
                TipoSimbolo tipoParametro = getTipoEstendido(parametro.tipo_estendido());
                processarParametro(tabelaDentroFuncao, parametro, tipoParametro);
                processarParametro(detalhesParametros, parametro, tipoParametro);
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


    // Verifica o corpo de uma função ou procedimento ao visitar o corpo
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
