package br.ufscar.dc.compiladores.geradorC;

import java.util.HashMap;

public class TabelaDeSimbolos {
    public enum TipoSimbolo {
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        REGISTRO,
        PONTEIRO,
        PROCEDIMENTO,
        ENDERECO,
        TIPO,
        INVALIDO
    }

    private class EntradaSimbolo {
        TipoSimbolo tipo;
        TabelaDeSimbolos detalhes = null;

        private EntradaSimbolo(TipoSimbolo tipo) {
            this.tipo = tipo;
            this.detalhes = null;
        }

        private EntradaSimbolo(TipoSimbolo tipo, TabelaDeSimbolos dados) {
            this.tipo = tipo;
            this.detalhes = dados;
        }
    }

    private final HashMap<String, EntradaSimbolo> mapaSimbolos;

    public TabelaDeSimbolos() {
        this.mapaSimbolos = new HashMap<>();
    }

    public void adicionarSimbolo(String nome, TipoSimbolo tipo) {
        mapaSimbolos.put(nome, new EntradaSimbolo(tipo));
    }

    public void inserirEstrutura(String identificador, TabelaDeSimbolos detalhesEstrutura) {
        mapaSimbolos.put(identificador, new EntradaSimbolo(TipoSimbolo.REGISTRO, detalhesEstrutura));
    }

    public void definirTipo(String identificador, TabelaDeSimbolos detalhesTipo) {
        mapaSimbolos.put(identificador, new EntradaSimbolo(TipoSimbolo.TIPO, detalhesTipo));
    }

    public void inserir(String identificador, TipoSimbolo tipo, TabelaDeSimbolos detalhes) {
        mapaSimbolos.put(identificador, new EntradaSimbolo(tipo, detalhes));
    }
    
    public TabelaDeSimbolos getEstrutura(String identificador) {
        EntradaSimbolo entrada = mapaSimbolos.get(identificador);
        return (entrada != null) ? entrada.detalhes : null;
    }

    public Integer tamanho() {
        return mapaSimbolos.size();
    }

    public boolean simboloExiste(String nome) {
        String[] partesNome = nome.split("\\.");
        HashMap<String, EntradaSimbolo> tabelaAtual = mapaSimbolos;

        for (int i = 0; i < partesNome.length; i++) {
            String parteAtual = partesNome[i];
            EntradaSimbolo entrada = tabelaAtual.get(parteAtual);

            if (entrada == null) {
                return false;
            }

            if (i < partesNome.length - 1) {
                if (entrada.tipo != TipoSimbolo.REGISTRO) {
                    return false;
                }
                tabelaAtual = entrada.detalhes.mapaSimbolos;
            }
        }

        return true;
    }

    public TipoSimbolo getTipo(String nome) {
        String[] partesNome = nome.split("\\.");
        HashMap<String, EntradaSimbolo> tabelaAtual = mapaSimbolos;

        for (int i = 0; i < partesNome.length; i++) {
            String parteAtual = partesNome[i];

            EntradaSimbolo entrada = tabelaAtual.get(parteAtual);
            if (entrada == null) {
                System.out.println("ERRO: Chave \"" + nome + "\" não encontrada na tabela!");
                return TipoSimbolo.INVALIDO;
            }

            if (i == partesNome.length - 1) {
                return entrada.tipo;
            }

            if (entrada.tipo != TipoSimbolo.REGISTRO) {
                return TipoSimbolo.INVALIDO;
            }

            tabelaAtual = entrada.detalhes.mapaSimbolos;
        }

        return TipoSimbolo.INVALIDO;
    }


    public TipoSimbolo getTipoPorIndice(Integer indice) {
        return mapaSimbolos.values()
                           .stream()
                           .skip(indice)
                           .findFirst()
                           .map(entrada -> entrada.tipo)
                           .orElse(TipoSimbolo.INVALIDO);
    }
}
