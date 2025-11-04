package Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase base para el Analizador Sintáctico de MiniJava.
 * Implementa un parser descendente recursivo basado en la gramática BNF.
 * 
 */

public class AnalizadorSintactico {

    private List<Token> tokens;
    private int indiceActual;
    private List<String> errores;

    private static final int LIMITE_ERRORES = 5;

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

        // Fin 
        if (indiceActual < tokens.size()) {
            Token t = tokens.get(indiceActual);
            errores.add(String.format(
                "Error sintáctico: tokens inesperados después del final del programa. Ejemplo: '%s' en línea %d.",
                t.getLexema(), t.getLinea()
            ));
        }
    }

    private void goal() {
        if (tokens.isEmpty()) {
            errores.add("Error sintáctico: archivo vacío, se esperaba al menos una clase.");
            return;
        }

        // Siempre debe haber al menos una clase
        mainClass();

        // Cero o más ClassDeclaration
        while (verificarLexema("class") || (verificarLexema("public") && siguienteEs("class"))) {
            classDeclaration();
        }
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

        // cero o más VarDeclaration o Statement (Kleene)
        while (true) {
            // Fin del bloque
            if (verificarLexema("}")) break;

            // Si empieza con tipo es una declaración de variable
            if (esTipo()) {
                varDeclaration();
                continue;
            }

            // Si empieza con palabra reservada de sentencia if, while, System, etc.
            if (esInicioDeSentencia()) {
                statement();
                continue;
            }

            // Si no coincide con nada conocido, reporta error y avanza para evitar bucle infinito
            registrarError("Token inesperado dentro del cuerpo del main.");
            avanzar();
        }

        match("}");

        // ---------- 7. Cierre de clase "}" ----------
        match("}");

    }

    private void varDeclaration() {
        // VarDeclaration ::= Type Identifier ";"
        if (!esTipo()) {
            registrarError("Se esperaba un tipo de dato (int, boolean, String o clase definida).");
            return;
        }
        avanzar(); // consume el tipo
        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador después del tipo de dato.");
            return;
        }
        avanzar();
        match(";");
    }

    private void statement() {
        if (verificarLexema("{")) {
            match("{");
            while (esInicioDeSentencia()) {
                statement();
            }
            match("}");
            return;
        }

        if (verificarLexema("if")) {
            match("if");
            match("(");
            expression();
            match(")");
            statement();
            if (verificarLexema("else")) {
                match("else");
                statement();
            }
            return;
        }

        if (verificarLexema("while")) {
            match("while");
            match("(");
            expression();
            match(")");
            statement();
            return;
        }

        if (verificarLexema("System")) {
            match("System"); match("."); match("out"); match("."); match("println");
            match("("); expression(); match(")"); match(";");
            return;
        }

        // Asignaciones
        if (verificarTipo("Identificador")) {
            avanzar();
            if (verificarLexema("=")) {
                match("=");
                expression();
                match(";");
            } else if (verificarLexema("[")) {
                match("["); expression(); match("]"); match("="); expression(); match(";");
            } else {
                registrarError("Se esperaba '=' o '[' después del identificador.");
            }
            return;
        }

        registrarError("Sentencia no reconocida.");
        avanzar();

    }

    private void methodDeclaration() {
        // MethodDeclaration ::= "public" Type Identifier "(" ( Type Identifier ( "," Type Identifier )* )? ")"
        // "{" ( VarDeclaration )* ( Statement )* "return" Expression ";" "}"

        // ---------- 1. "public" ----------
        match("public");

        // ---------- 2. Type ----------
        if (!esTipo()) {
            registrarError("Se esperaba un tipo de retorno después de 'public'.");
            return;
        }
        avanzar();

        // ---------- 3. Identifier ----------
        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador (nombre del método) después del tipo.");
            return;
        }
        avanzar();

        // ---------- 4. "(" parámetros opcionales ")" ----------
        match("(");
        if (esTipo()) {
            // Primer parámetro
            avanzar();
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba un identificador de parámetro.");
            } else {
                avanzar();
            }

            // Parámetros adicionales separados por comas
            while (verificarLexema(",")) {
                match(",");
                if (!esTipo()) {
                    registrarError("Se esperaba un tipo de parámetro después de ','.");
                    break;
                }
                avanzar();
                if (!verificarTipo("Identificador")) {
                    registrarError("Se esperaba un identificador de parámetro.");
                    break;
                }
                avanzar();
            }
        }
        match(")");

        // ---------- 5. "{" cuerpo del método ----------
        match("{");

        // ( VarDeclaration )*
        while (esTipo()) {
            varDeclaration();
        }

        // ( Statement )*
        while (esInicioDeSentencia()) {
            statement();
        }

        // ---------- 6. "return" Expression ";" ----------
        if (!match("return")) {
            registrarError("Se esperaba la palabra clave 'return' antes de finalizar el método.");
        } else {
            expression();
            match(";");
        }

        // ---------- 7. "}" ----------
        match("}");
    }


    private void classDeclaration() {
        // ("public")? "class" Identifier ("extends" Identifier)? "{" (VarDeclaration)* (MethodDeclaration)* "}"

        if (verificarLexema("public")) match("public");
        match("class");

        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador como nombre de clase.");
            return;
        } else avanzar();

        // Herencia 
        if (verificarLexema("extends")) {
            match("extends");
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba un identificador después de 'extends'.");
            } else avanzar();
        }

        match("{");

        // Cero o más declaraciones de variables
        while (esTipo()) {
            varDeclaration();
        }

        // Cero o más métodos
        while (verificarLexema("public")) {
            methodDeclaration();
        }

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

    private boolean siguienteEs(String esperado) {
        int siguiente = indiceActual + 1;
        return siguiente < tokens.size() &&
            tokens.get(siguiente).getLexema().equals(esperado);
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

    private boolean esTipo() {
        if (indiceActual >= tokens.size()) return false;

        String lex = tokens.get(indiceActual).getLexema();
        String tipo = tokens.get(indiceActual).getTipo();

        // Por ahora solo registro los tipos mas basicos
        return lex.equals("int") 
        || lex.equals("boolean") 
        || lex.equals("String") 
        || tipo.equals("Identificador");
    }

    private boolean esInicioDeSentencia() {
        if (indiceActual >= tokens.size()) return false;
        String lex = tokens.get(indiceActual).getLexema();

        // Palabras clave que inician sentencias válidas
        return lex.equals("if") || lex.equals("while") || lex.equals("System")
            || lex.equals("{") || lex.equals("return")
            || verificarTipo("Identificador"); // asignaciones o llamadas
    }

    private void expression() {
        if (verificarTipo("Identificador") 
        || verificarTipo("Entero") 
        || verificarTipo("Decimal")) {
            avanzar();
        } else if (verificarLexema("true") 
        || verificarLexema("false") 
        || verificarLexema("this")) {
            avanzar();
        } else if (verificarLexema("(")) {
            match("(");
            expression();
            match(")");
        } else {
            registrarError("Expresión no reconocida.");
            avanzar();
        }
    }


    // Registra un error sintáctico (al final necesite esta funcion sjjs)
    private void registrarError(String mensaje) {
        if (errores.size() >= LIMITE_ERRORES) return; // detener registro

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
