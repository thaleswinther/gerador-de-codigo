package br.ufscar.dc.compiladores.semantico;

import java.util.LinkedList;

public final class Escopo {
    private final LinkedList<TabelaDeSimbolos> pilhaDeEscopos;

    public Escopo(){
        pilhaDeEscopos = new LinkedList<>();
        criarNovoEscopo();
    }

    public void criarNovoEscopo(){
        pilhaDeEscopos.push(new TabelaDeSimbolos());
    }

    public void removerEscopo(){
        pilhaDeEscopos.pop();
    }

    public TabelaDeSimbolos getEscopoAtual(){
        return pilhaDeEscopos.peek();
    }

    public LinkedList<TabelaDeSimbolos> getAllEscopos(){
        return pilhaDeEscopos;
    }
}