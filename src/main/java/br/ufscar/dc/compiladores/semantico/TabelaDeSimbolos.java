package br.ufscar.dc.compiladores.semantico;

import java.util.HashMap;

public class TabelaDeSimbolos {
    public enum TipoSimbolo{
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        REGISTRO,
        PONTEIRO,
        FUNCAO,
        PROCEDIMENTO,
        INVALIDO
    }
    
    class EntradaSimbolo {
        TipoSimbolo tipo;
        

        private EntradaSimbolo(TipoSimbolo tipo) {
            this.tipo = tipo;
        }
    }
    
    private final HashMap<String, EntradaSimbolo> mapaSimbolos;
    
    public TabelaDeSimbolos() {
        this.mapaSimbolos = new HashMap<>();
    }
    
    public void adicionarSimbolo(String nome, TipoSimbolo tipo) {
        mapaSimbolos.put(nome, new EntradaSimbolo(tipo));
    }
    
    public boolean simboloExiste(String nome) {
        return mapaSimbolos.containsKey(nome);
    }
    
    public TipoSimbolo getTipo(String nome) {
        EntradaSimbolo entrada = mapaSimbolos.get(nome);
        if (entrada != null) {
            return entrada.tipo;
        } else {
            return TipoSimbolo.INVALIDO; // Retorna um tipo inválido se o símbolo não for encontrado
        }
    }    
}
