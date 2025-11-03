package Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase base para el Analizador Sintáctico de MiniJava.
 * Implementa un parser descendente recursivo basado en la gramática BNF.
 * 
 * Cosas por hacer :
 * - Implementar los métodos recursivos: goal(), mainClass(), classDeclaration(), etc.
 * - Agregar manejo de errores con límite (2 errores máx).
 * - Enriquecer la tabla de símbolos conforme se identifiquen variables y clases.
 */

public class AnalizadorSintactico {

    private List<Token> tokens;
    private int indiceActual;
    private List<String> errores;

    public AnalizadorSintactico() {
        this.tokens = new ArrayList<>();
        this.indiceActual = 0;
        this.errores = new ArrayList<>();
    }

    /** Método principal que inicia el análisis sintáctico */
    public void analizar(List<Token> tokensEntrada) {
        this.tokens = tokensEntrada;
        this.indiceActual = 0;
        this.errores.clear();

        // Inicio
        goal();
    }

    private void goal() {
        // Goal ::= MainClass (ClassDeclaration)* <EOF>
        
        // Verificar que no este vacio
        if (tokens.isEmpty()) {
            errores.add("Error sintáctico: archivo vacío, se esperaba una clase principal.");
            return;
        }

        mainClass();

        // Más adelante aquí iría:
        // while (verificar("class")) { classDeclaration(); }
    }

    private void mainClass() {
        // Estructura esperada:
        // ( "public" )? "class" Identifier "{" 
        // "public" "static" "void" "main" "(" "String" "[" "]" Identifier ")" 
        // "{" ( VarDeclaration | Statement )* "}" "}"

        // ---------- 1. ( "public" )? ----------
        if (verificarLexema("public")) {
            match("public");
        }

        // ---------- 2. "class" ----------
        if (!match("class")) {
            // No hay punto en continuar si no hay clase principal
            return;
        }

        // ---------- 3. Identifier ----------
        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador (nombre de clase) después de 'class'.");
            return;
        } else {
            avanzar(); // consumimos el nombre de la clase
        }

        // ---------- 4. "{" ----------
        match("{");

        // ---------- 5. "public static void main ( String [ ] id )" ----------
        if (!match("public")) return;
        if (!match("static")) return;
        if (!match("void")) return;
        if (!match("main")) return;

        match("(");
        match("String");
        match("[");
        match("]");
        
        if (!verificarTipo("Identificador")) { // el args
            registrarError("Se esperaba un identificador como parámetro de 'main'.");
            return;
        } else {
            avanzar();
        }

        match(")");

        // ---------- 6. "{" ( VarDeclaration | Statement )* "}" ----------
        if (!match("{")) return;

        // LOGICA ( VarDeclaration | Statement )* 
        // ...

        match("}");

        // ---------- 7. Cierre de clase "}" ----------
        match("}");

    }

    // Avanza al siguiente token 
    private void avanzar() {
        if (indiceActual < tokens.size()) {
            indiceActual++;
        }
    }

    // Verifica si el token actual tiene el lexema especificado 
    private boolean verificarLexema(String esperado) {
        return indiceActual < tokens.size() &&
            tokens.get(indiceActual).getLexema().equals(esperado);
    }

    // Verifica si el token actual es de cierto tipo (ej. "Identificador")
    private boolean verificarTipo(String tipoEsperado) {
        return indiceActual < tokens.size() &&
            tokens.get(indiceActual).getTipo().equals(tipoEsperado);
    }

    // Verifica si el token actual coincide con el lexema esperado 
    private boolean match(String esperado) {
        if (indiceActual < tokens.size() && tokens.get(indiceActual).getLexema().equals(esperado)) {
            indiceActual++;
            return true;
        } else {
            if (indiceActual < tokens.size()) {
                Token t = tokens.get(indiceActual);
                errores.add(String.format(
                    "Error sintáctico en línea %d, columna %d: Se esperaba '%s' pero se encontró '%s'.",
                    t.getLinea(), t.getColumna(), esperado, t.getLexema()
                ));
            } else {
                errores.add("Error sintáctico: fin de archivo inesperado, se esperaba '" + esperado + "'.");
            }
            return false;
        }
    }

    // Registra un error sintáctico (al final necesite esta funcion sjjs)
    private void registrarError(String mensaje) {
        if (indiceActual < tokens.size()) {
            Token t = tokens.get(indiceActual);
            errores.add(String.format(
                "Error sintáctico en línea %d, columna %d: %s (token: '%s')",
                t.getLinea(), t.getColumna(), mensaje, t.getLexema()
            ));
        } else {
            errores.add("Error sintáctico: " + mensaje + " (fin de archivo).");
        }
    }

    // Devuelve la lista de errores sintácticos encontrados 
    public List<String> getErrores() {
        return errores;
    }
}
