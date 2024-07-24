package br.ufscar.dc.compiladores.semantico;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import br.ufscar.dc.compiladores.semantico.LAParser.CmdAtribuicaoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Declaracao_variavelContext;
import br.ufscar.dc.compiladores.semantico.LAParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Expressao_aritmeticaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Expressao_relacionalContext;
import br.ufscar.dc.compiladores.semantico.LAParser.ExpressaoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.FatorContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Fator_logicoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.IdentificadorContext;
import br.ufscar.dc.compiladores.semantico.LAParser.ParcelaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Parcela_logicaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Parcela_nao_unariaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Parcela_unariaContext;
import br.ufscar.dc.compiladores.semantico.LAParser.TermoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Termo_logicoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.TipoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Tipo_basicoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.Tipo_estendidoContext;
import br.ufscar.dc.compiladores.semantico.LAParser.VariavelContext;
import br.ufscar.dc.compiladores.semantico.TabelaDeSimbolos.TipoSimbolo;

public class AnalisadorSemantico extends LABaseVisitor<Void> {

    Escopo escopo = new Escopo();
    public static List<String> errosSemanticos = new ArrayList<>();

    // Registra um erro semântico na lista de erros
    public static void registrarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    // Retorna o tipo básico de um contexto
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
    public static TipoSimbolo getTipoVariavel(Escopo escopo, Tipo_estendidoContext contexto) {
        if (contexto.PONTEIRO() != null) {
            return TipoSimbolo.PONTEIRO;
        }

        if (contexto.IDENT() != null) {
            String ident = contexto.IDENT().getText();
            TabelaDeSimbolos tabelaSimbolos = escopo.getEscopoAtual();
            return tabelaSimbolos.simboloExiste(ident) ? TipoSimbolo.REGISTRO : TipoSimbolo.INVALIDO;
        }

        return getTipoBasico(contexto.tipo_basico());
    }


    // Retorna o tipo de um contexto
    public static TipoSimbolo getTipo(Escopo escopo, TipoContext contexto) {
        return getTipoVariavel(escopo, contexto.tipo_estendido());
    }

    // Verifica e registra o tipo de uma variável no escopo atual
    public static TipoSimbolo getTipo(Escopo escopo, VariavelContext contexto) {
        TipoSimbolo tipo = getTipo(escopo, contexto.tipo());
        TabelaDeSimbolos tabela = escopo.getEscopoAtual();

        for (IdentificadorContext ident : contexto.identificador()) {
            String nomeIdent = ident.getText();
            if (tabela.simboloExiste(nomeIdent)) {
                registrarErroSemantico(ident.start, "identificador " + nomeIdent + " ja declarado anteriormente");
            } else {
                tabela.adicionarSimbolo(nomeIdent, tipo);
            }
        }

        if (tipo == TipoSimbolo.INVALIDO) {
            registrarErroSemantico(contexto.tipo().start, "tipo " + contexto.tipo().getText() + " nao declarado");
        }

        return tipo;
    }


    // Verifica se um símbolo existe no escopo atual
    public static Boolean existeSimbolo(IdentificadorContext contexto, Escopo escopo) {
        String nome = contexto.IDENT().get(0).getText();

        return escopo.getAllEscopos().stream()
                     .anyMatch(tabela -> tabela.simboloExiste(nome));
    }


    // Retorna o tipo de um identificador em todos os escopos
    public static TipoSimbolo getTipoDeTodosEscopos(Escopo escopo, String nome) {
        return escopo.getAllEscopos().stream()
                     .filter(tabela -> tabela.simboloExiste(nome))
                     .map(tabela -> tabela.getTipo(nome))
                     .findFirst()
                     .orElse(TipoSimbolo.INVALIDO);
    }


    // Verifica o tipo de uma parcela unária
    public static TipoSimbolo verificarTipo(Parcela_unariaContext contexto, Escopo escopo) {
        if (contexto.identificador() != null) {
            return verificarTipoIdentificador(contexto, escopo);
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

        if (contexto.CADEIA() != null) {
            return TipoSimbolo.LITERAL;
        }

        if (contexto.exp_unica != null) {
            return verificarTipo(contexto.exp_unica, escopo);
        }

        return TipoSimbolo.INVALIDO;
    }

    // Método auxiliar para verificar o tipo de um identificador
    private static TipoSimbolo verificarTipoIdentificador(Parcela_unariaContext contexto, Escopo escopo) {
        String nome = contexto.identificador().IDENT(0).getText();
        return getTipoDeTodosEscopos(escopo, nome);
    }


    // Verifica o tipo de um termo lógico
    public static TipoSimbolo verificarTipo(Termo_logicoContext contexto, Escopo escopo) {
        if (!contexto.operador_logico_e().isEmpty()) {
            return TipoSimbolo.LOGICO;
        }
        return verificarTipo(contexto.fator_logico(0), escopo);
    }

    // Verifica o tipo de uma expressão
    public static TipoSimbolo verificarTipo(ExpressaoContext contexto, Escopo escopo) {
        return verificarTipo(contexto.termo_logico(0), escopo);
    }

    // Verifica o tipo de um fator lógico
    public static TipoSimbolo verificarTipo(Fator_logicoContext contexto, Escopo escopo) {
        if (contexto.parcela_logica() != null) {
            return AnalisadorSemantico.verificarTipo(contexto.parcela_logica(), escopo);
        }

        return TipoSimbolo.INVALIDO;
    }

    // Verifica o tipo de uma parcela lógica
    public static TipoSimbolo verificarTipo(Parcela_logicaContext contexto, Escopo escopo) {
        if (contexto.TRUE() != null || contexto.FALSE() != null) {
            return TipoSimbolo.LOGICO;
        }

        return AnalisadorSemantico.verificarTipo(contexto.expressao_relacional(), escopo);
    }

    // Verifica o tipo de uma expressão relacional
    public static TipoSimbolo verificarTipo(Expressao_relacionalContext contexto, Escopo escopo) {
        return verificarTipo(contexto.expressao_aritmetica(0), escopo);
    }

    // Verifica se dois tipos são equivalentes
    public static Boolean isEquivalente(TipoSimbolo tipo1, TipoSimbolo tipo2) {
        if (tipo1 == tipo2) {
            return true;
        }
        return (tipo1 == TipoSimbolo.REAL && tipo2 == TipoSimbolo.INTEIRO) || (tipo1 == TipoSimbolo.INTEIRO && tipo2 == TipoSimbolo.REAL);
    }

    // Verifica o tipo de uma expressão aritmética
    public static TipoSimbolo verificarTipo(Expressao_aritmeticaContext contexto, Escopo escopo) {
        TipoSimbolo tipo = verificarTipo(contexto.termo(0), escopo);

        if (tipo != TipoSimbolo.INVALIDO) {
            for (TermoContext termo : contexto.termo()) {
                TipoSimbolo tipoTestado = verificarTipo(termo, escopo);

                if (!isEquivalente(tipoTestado, tipo)) {
                    return TipoSimbolo.INVALIDO;
                }
            }
        }

        return tipo;
    }

    // Verifica o tipo de um termo
    public static TipoSimbolo verificarTipo(TermoContext contexto, Escopo escopo) {
        return verificarTipo(contexto.fator(0), escopo);
    }

    // Verifica o tipo de um fator
    public static TipoSimbolo verificarTipo(FatorContext contexto, Escopo escopo) {
        return verificarTipo(contexto.parcela(0), escopo);
    }

    // Verifica o tipo de uma parcela
    public static TipoSimbolo verificarTipo(ParcelaContext contexto, Escopo escopo) {
        if (contexto.parcela_unaria() != null) {
            return verificarTipo(contexto.parcela_unaria(), escopo);
        } else if (contexto.parcela_nao_unaria() != null) {
            return verificarTipo(contexto.parcela_nao_unaria(), escopo);
        }

        return TipoSimbolo.INVALIDO;
    }

    // Verifica o tipo de uma parcela não unária
    public static TipoSimbolo verificarTipo(Parcela_nao_unariaContext contexto, Escopo escopo) {
        if (contexto.identificador() != null) {
            return AnalisadorSemantico.verificarTipo(contexto.identificador(), escopo);
        }

        return TipoSimbolo.INVALIDO;
    }

    // Verifica o tipo de um identificador
    public static TipoSimbolo verificarTipo(IdentificadorContext contexto, Escopo escopo) {
        if (contexto.IDENT(0) != null) {
            return getTipoDeTodosEscopos(escopo, contexto.IDENT(0).getText());
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
        TabelaDeSimbolos tabela = escopo.getEscopoAtual();

        if (contexto.DECLARE() != null) {
            String nome = contexto.DECLARE().getText();

            if (tabela.simboloExiste(nome)) {
                System.out.println("Variavel " + nome + " ja esta declarada");
            } else {
                AnalisadorSemantico.getTipo(escopo, contexto.variavel());
            }
        }
        return super.visitDeclaracao_variavel(contexto);
    }

    // Verifica se um identificador está declarado ao visitar um identificador
    @Override
    public Void visitIdentificador(IdentificadorContext contexto) {
        Boolean existeIdentificador = AnalisadorSemantico.existeSimbolo(contexto, escopo);
        String nome = contexto.IDENT().get(0).getText();

        if (!existeIdentificador) {
            AnalisadorSemantico.registrarErroSemantico(contexto.start, "identificador " + nome + " nao declarado");
        }

        return super.visitIdentificador(contexto);
    }

    // Verifica a compatibilidade de atribuição ao visitar um comando de atribuição
    @Override
    public Void visitCmdAtribuicao(CmdAtribuicaoContext contexto) {
        IdentificadorContext identificador = contexto.identificador();
        if (AnalisadorSemantico.existeSimbolo(identificador, escopo)) {
            String nome = identificador.IDENT(0).getText();
            TipoSimbolo tipo = contexto.PONTEIRO() != null ? TipoSimbolo.PONTEIRO : AnalisadorSemantico.getTipoDeTodosEscopos(escopo, nome);

            if (tipo == TipoSimbolo.INVALIDO) {
                AnalisadorSemantico.registrarErroSemantico(contexto.start, "identificador " + nome + " com tipo invalido");
            } else {
                TipoSimbolo tipoExpressao = AnalisadorSemantico.verificarTipo(contexto.expressao(), escopo);
                if (tipoExpressao == TipoSimbolo.INVALIDO || !AnalisadorSemantico.isEquivalente(tipoExpressao, tipo)) {
                    AnalisadorSemantico.registrarErroSemantico(contexto.start, "atribuicao nao compativel para " + nome);
                }
            }
        }

        return super.visitCmdAtribuicao(contexto);
    }

}
