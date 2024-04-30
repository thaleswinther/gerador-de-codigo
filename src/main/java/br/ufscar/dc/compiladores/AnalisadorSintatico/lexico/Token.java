package br.ufscar.dc.compiladores.AnalisadorSintatico.lexico;

public class Token {
    private String type;
    private String value;

    private int line;

    public Token(String type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    @Override
    public String toString() {
        if (!type.equals(value)) {
            return "<'" + this.value + "'," + type + ">";
        } else {
            return "<'" + this.value + "','" + this.type + "'>";
        }
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }
}
