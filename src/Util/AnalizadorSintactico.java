package Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador Sintáctico de MiniJava.
 * Parser descendente recursivo que usa los tokens generados por AnalizadorLexico.
 */
public class AnalizadorSintactico {

    private List<Token> tokens;
    private int indiceActual;
    private List<String> errores;
    private List<Simbolo> tablaSimbolos = new ArrayList<>();

    private String claseActual = "";
    private String visibilidadActual = "public";

    private static final int LIMITE_ERRORES = 5;

    public AnalizadorSintactico() {
        this.tokens = new ArrayList<>();
        this.indiceActual = 0;
        this.errores = new ArrayList<>();
    }

    /** Inicia el análisis sintáctico con una lista de tokens */
    public void analizar(List<Token> tokensEntrada) {
        this.tokens = tokensEntrada;
        this.indiceActual = 0;
        this.errores.clear();
        this.tablaSimbolos.clear(); // limpiar tabla previa

        goal();

        // Si quedan tokens sin consumir
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

        mainClass();

        while (verificarLexema("class") || (verificarLexema("public") && siguienteEs("class"))) {
            classDeclaration();
        }
    }

    private void mainClass() {
        if (verificarLexema("public")) match("public");
        if (!match("class")) return;

        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador después de 'class'.");
            return;
        }

        Token t = tokens.get(indiceActual);
        claseActual = t.getLexema();
        tablaSimbolos.add(new Simbolo(claseActual, "-", "-", "-", "public",
            "Línea " + t.getLinea() + ", Columna " + t.getColumna(), "Clase"));
        avanzar();

        match("{");

        match("public"); match("static"); match("void"); match("main");
        match("("); match("String"); match("["); match("]");

        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador como parámetro de 'main'.");
        } else avanzar();

        match(")"); match("{");

        while (true) {
            if (verificarLexema("}")) break;

            if (esTipo() && siguienteEsIdentificador() && siguienteDespuesDeIdentificadorEsPuntoYComa()) {
                varDeclaration();
                continue;
            }


            if (esInicioDeSentencia()) {
                statement();
                continue;
            }

            registrarError("Token inesperado dentro del cuerpo del main.");
            avanzar();
        }

        match("}"); match("}");
    }

    // Sobrecarga para compatibilidad con llamadas sin parametros
    private void varDeclaration() {
        varDeclaration(claseActual, visibilidadActual);
    }

    private void varDeclaration(String claseContenedora, String visibilidad) {
        if (!esTipo()) {
            registrarError("Se esperaba un tipo de dato (int, boolean, String o clase definida).");
            return;
        }

        String tipo = tokens.get(indiceActual).getLexema();
        avanzar();

        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador después del tipo de dato.");
            return;
        }

        Token tokenVar = tokens.get(indiceActual);
        String nombre = tokenVar.getLexema();
        avanzar();
        match(";");

        tablaSimbolos.add(new Simbolo(nombre, tipo, claseContenedora, "-", visibilidad,
            "Línea " + tokenVar.getLinea() + ", Columna " + tokenVar.getColumna(), "Variable"));
    }

    
    private void statement() {
        if (verificarLexema("{")) {
            match("{");
            while (esInicioDeSentencia()) statement();
            match("}");
            return;
        }

        if (verificarLexema("if")) {
            match("if"); match("("); expression(); match(")"); statement();
            if (verificarLexema("else")) { match("else"); statement(); }
            return;
        }

        if (verificarLexema("while")) {
            match("while"); match("("); expression(); match(")"); statement();
            return;
        }

        if (verificarLexema("System")) {
            match("System"); match("."); match("out"); match("."); match("println");
            match("("); expression(); match(")"); match(";");
            return;
        }

        if (verificarLexema("return")) {
            match("return");

            // Soporta tanto "return;" como "return <expr>;"
            if (!verificarLexema(";")) {
                expression();
            }
            match(";");
            return;
        }

        // Asignaciones o llamadas
        if (verificarTipo("Identificador")) {
            avanzar(); // identificador

            if (verificarLexema("=")) {
                match("="); expression(); match(";");
            } else if (verificarLexema("[")) {
                match("["); expression(); match("]"); match("="); expression(); match(";");
            } else if (verificarLexema("(")) {
                match("(");
                if (!verificarLexema(")")) {
                    while (true) {
                        expression();
                        if (verificarLexema(",")) match(",");
                        else break;
                    }
                }
                match(")");
                match(";");
            } else {
                registrarError("Se esperaba '=' o '[' o '(' después del identificador.");
            }
            return;
        }

        registrarError("Sentencia no reconocida.");
        avanzar();
    }

    private void methodDeclaration(String claseContenedora) {
        match("public");

        String visibilidad = "public";
        String tipoRetorno = "";
        String nombreMetodo = "";
        Token tokenMetodo;

        // --- Tipo de retorno ---
        if (!esTipo()) {
            registrarError("Se esperaba un tipo de retorno después de 'public'.");
            return;
        }
        tipoRetorno = tokens.get(indiceActual).getLexema();
        avanzar();

        // --- Nombre del método ---
        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador como nombre de método.");
            return;
        }

        tokenMetodo = tokens.get(indiceActual);
        nombreMetodo = tokenMetodo.getLexema();
        avanzar();

        // Registrar método en la tabla de símbolos
        tablaSimbolos.add(new Simbolo(
            nombreMetodo, tipoRetorno, claseContenedora, "-", visibilidad,
            "Línea " + tokenMetodo.getLinea() + ", Columna " + tokenMetodo.getColumna(), "Método"
        ));

        // --- Parámetros ---
        match("(");

        if (esTipo()) {
            String tipoParam = tokens.get(indiceActual).getLexema();
            avanzar();

            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba un identificador de parámetro.");
            } else {
                Token tParam = tokens.get(indiceActual);
                tablaSimbolos.add(new Simbolo(
                    tParam.getLexema(), tipoParam,
                    claseContenedora + "." + nombreMetodo, "-", "local",
                    "Línea " + tParam.getLinea() + ", Columna " + tParam.getColumna(), "Parámetro"
                ));
                avanzar();
            }

            while (verificarLexema(",")) {
                match(",");
                if (!esTipo()) {
                    registrarError("Se esperaba un tipo de parámetro.");
                    break;
                }
                tipoParam = tokens.get(indiceActual).getLexema();
                avanzar();
                if (!verificarTipo("Identificador")) {
                    registrarError("Se esperaba identificador de parámetro.");
                    break;
                }
                Token tParam = tokens.get(indiceActual);
                tablaSimbolos.add(new Simbolo(
                    tParam.getLexema(), tipoParam,
                    claseContenedora + "." + nombreMetodo, "-", "local",
                    "Línea " + tParam.getLinea() + ", Columna " + tParam.getColumna(), "Parámetro"
                ));
                avanzar();
            }
        }

        match(")");

        // --- Cuerpo del método ---
        if (!match("{")) return;

        while (!verificarLexema("}") && indiceActual < tokens.size()) {

            // Si parece declaración de variable local (tipo + identificador + ';')
            if (esTipo() && siguienteEsIdentificador() && siguienteDespuesDeIdentificadorEsPuntoYComa()) {
                varDeclaration(nombreMetodo, "local");
            }

            // Si es sentencia (asignación, llamada, return, etc.)
            else if (esInicioDeSentencia()) {
                statement();
            }

            // Cualquier otro token inesperado
            else {
                registrarError("Token inesperado dentro del cuerpo del método.");
                avanzar();
            }
        }

        match("}"); // Fin del método
    }
    
    private void classDeclaration() {
        String visibilidad = "default";
        String nombreClase = "";
        String clasePadre = null;
        Token tokenClase;

        if (verificarLexema("public")) { visibilidad = "public"; match("public"); }
        if (!match("class")) return;

        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador como nombre de clase.");
            return;
        }

        tokenClase = tokens.get(indiceActual);
        nombreClase = tokenClase.getLexema();
        avanzar();

        if (verificarLexema("extends")) {
            match("extends");
            if (!verificarTipo("Identificador")) registrarError("Se esperaba identificador después de 'extends'.");
            else { clasePadre = tokens.get(indiceActual).getLexema(); avanzar(); }
        }

        tablaSimbolos.add(new Simbolo(nombreClase, "class", clasePadre, "-", visibilidad,
            "Línea " + tokenClase.getLinea() + ", Columna " + tokenClase.getColumna(), "Clase"));

        if (!match("{")) return;
        while (esTipo()) varDeclaration(nombreClase, visibilidad);
        while (verificarLexema("public") && (siguienteEs("int") || siguienteEs("boolean") || siguienteEs("String") || siguienteEs("void") || siguienteEsTipoIdentificador())) {
            methodDeclaration(nombreClase);
        }

        match("}");
    }

    // ------------------ MÉTODOS AUXILIARES ------------------

    private boolean siguienteEsTipoIdentificador() {
        int sig = indiceActual + 1;
        return sig < tokens.size() && tokens.get(sig).getTipo().equals("Identificador");
    }

    private void avanzar() { if (indiceActual < tokens.size()) indiceActual++; }

    private boolean verificarLexema(String esperado) {
        return indiceActual < tokens.size() && tokens.get(indiceActual).getLexema().equals(esperado);
    }

    private boolean verificarTipo(String tipoEsperado) {
        return indiceActual < tokens.size() && tokens.get(indiceActual).getTipo().equals(tipoEsperado);
    }

    private boolean siguienteEs(String esperado) {
        int sig = indiceActual + 1;
        return sig < tokens.size() && tokens.get(sig).getLexema().equals(esperado);
    }

    private boolean match(String esperado) {
        if (indiceActual < tokens.size() && tokens.get(indiceActual).getLexema().equals(esperado)) {
            indiceActual++;
            return true;
        } else {
            if (indiceActual < tokens.size()) {
                Token t = tokens.get(indiceActual);
                errores.add(String.format(
                    "Error sintáctico en línea %d, columna %d: Se esperaba '%s' pero se encontró '%s'.",
                    t.getLinea(), t.getColumna(), esperado, t.getLexema()));
            } else errores.add("Error sintáctico: fin de archivo inesperado, se esperaba '" + esperado + "'.");
            return false;
        }
    }

    private boolean esTipo() {
        if (indiceActual >= tokens.size()) return false;
        String lex = tokens.get(indiceActual).getLexema();
        String tipo = tokens.get(indiceActual).getTipo();
        return lex.equals("int") || lex.equals("boolean") || lex.equals("String") || lex.equals("void") || tipo.equals("Identificador");
    }


    private boolean esInicioDeSentencia() {
        if (indiceActual >= tokens.size()) return false;
        String lex = tokens.get(indiceActual).getLexema();
        return lex.equals("if") || lex.equals("while") || lex.equals("System")
            || lex.equals("{") || lex.equals("return") || verificarTipo("Identificador");
    }

    /**
     * Analiza expresiones generales incluyendo operadores relacionales y aritméticos,
     * y construcciones 'new Clase(...)'.
     */
    private void expression() {
        simpleExpression();
        while (indiceActual < tokens.size()) {
            Token t = tokens.get(indiceActual);
            if (t.getTipo().equals("Operador") && esOperadorValido(t.getLexema())) {
                avanzar();
                simpleExpression();
            } else break;
        }
    }

    private void simpleExpression() {
        if (verificarTipo("Identificador") || verificarTipo("Entero") || verificarTipo("Decimal")) {
            avanzar();
        } else if (verificarLexema("true") || verificarLexema("false") || verificarLexema("this")) {
            avanzar();
        } else if (verificarLexema("(")) {
            match("("); expression(); match(")");
        } else if (verificarLexema("new")) {
            match("new");
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba un identificador después de 'new'.");
            } else {
                // tipo de la clase
                avanzar();
            }
            // paréntesis de constructor
            match("(");
            // argumentos del constructor (opcionales)
            if (!verificarLexema(")")) {
                while (true) {
                    expression();
                    if (verificarLexema(",")) match(",");
                    else break;
                }
            }
            match(")");
        } else {
            registrarError("Expresión no reconocida.");
            avanzar();
        }
    }

    private boolean esOperadorValido(String op) {
        return op.matches("==|!=|>|<|>=|<=|\\+|-|\\*|/|%");
    }

    private void registrarError(String mensaje) {
        if (errores.size() >= LIMITE_ERRORES) return;
        if (indiceActual < tokens.size()) {
            Token t = tokens.get(indiceActual);
            errores.add(String.format(
                "Error sintáctico en línea %d, columna %d: %s (token: '%s')",
                t.getLinea(), t.getColumna(), mensaje, t.getLexema()));
        } else errores.add("Error sintáctico: " + mensaje + " (fin de archivo).");
    }
    
    private boolean siguienteEsIdentificador() {
        int sig = indiceActual + 1;
        return sig < tokens.size() && tokens.get(sig).getTipo().equals("Identificador");
    }

    private boolean siguienteDespuesDeIdentificadorEsPuntoYComa() {
        int sig = indiceActual + 2;
        return sig < tokens.size() && tokens.get(sig).getLexema().equals(";");
    }

    public List<String> getErrores() { return errores; }
    public List<Simbolo> getTablaSimbolos() { return tablaSimbolos; }
}
