package Util;

public class Token {
    private String tipo;    // "Identificador", "Palabra Clave", "Operador" ect
    private String lexema;  // Puede ser "A", "B" "C", "X", "Y" etc
    private int linea;      // Línea donde se encontró
    private int columna;    // Columna donde inicia

    public Token(String tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public String getTipo() { return tipo; }
    public String getLexema() { return lexema; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }

    @Override
    public String toString() {
        return String.format(
            "Token[tipo=%s, lexema='%s', linea=%d, columna=%d]", 
            tipo, lexema, linea, columna
        );
    }
}
