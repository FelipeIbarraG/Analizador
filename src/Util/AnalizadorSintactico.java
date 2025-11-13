package Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador Sintáctico de MiniJava mejorado.
 * Parser descendente recursivo que usa los tokens generados por AnalizadorLexico.
 */
public class AnalizadorSintactico {

    private List<Token> tokens;
    private int indiceActual;
    private List<String> errores;
    private List<Simbolo> tablaSimbolos = new ArrayList<>();

    private String claseActual = "";
    private String visibilidadActual = "default";

    private static final int LIMITE_ERRORES = 100;

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
        this.tablaSimbolos.clear();

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

        // Procesar todas las clases (pueden ser múltiples clases públicas o no públicas)
        while (indiceActual < tokens.size()) {
            if (verificarLexema("public") || verificarLexema("class")) {
                classDeclaration();
            } else {
                registrarError("Se esperaba una declaración de clase.");
                avanzar();
            }
            
            if (errores.size() >= LIMITE_ERRORES) break;
        }
    }

    private void classDeclaration() {
        String visibilidad = "default";
        String nombreClase = "";
        String clasePadre = null;
        Token tokenClase;

        // Modificador de acceso opcional
        if (verificarLexema("public")) { 
            visibilidad = "public"; 
            match("public"); 
        } else if (verificarLexema("private")) {
            visibilidad = "private";
            match("private");
        } else if (verificarLexema("protected")) {
            visibilidad = "protected";
            match("protected");
        }

        if (!match("class")) return;

        // Nombre de la clase
        if (!verificarTipo("Identificador")) {
            registrarError("Se esperaba un identificador como nombre de clase.");
            return;
        }

        tokenClase = tokens.get(indiceActual);
        nombreClase = tokenClase.getLexema();
        claseActual = nombreClase;
        avanzar();

        // Herencia (extends)
        if (verificarLexema("extends")) {
            match("extends");
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba identificador después de 'extends'.");
            } else { 
                clasePadre = tokens.get(indiceActual).getLexema(); 
                avanzar(); 
            }
        }

        // Implementación (implements)
        if (verificarLexema("implements")) {
            match("implements");
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba identificador después de 'implements'.");
            } else {
                avanzar();
                // Puede implementar múltiples interfaces
                while (verificarLexema(",")) {
                    match(",");
                    if (!verificarTipo("Identificador")) {
                        registrarError("Se esperaba identificador después de ','.");
                    } else {
                        avanzar();
                    }
                }
            }
        }

        // Registrar clase en tabla de símbolos
        tablaSimbolos.add(new Simbolo(nombreClase, "class", clasePadre != null ? clasePadre : "-", "-", visibilidad,
            "Línea " + tokenClase.getLinea() + ", Columna " + tokenClase.getColumna(), "Clase"));

        if (!match("{")) return;

        // Cuerpo de la clase: variables de instancia y métodos
        while (indiceActual < tokens.size() && !verificarLexema("}")) {
            
            // Verificar si es el método main
            if (esMetodoMain()) {
                mainMethod();
            }
            // Verificar si es una declaración de método
            else if (esDeclaracionMetodo()) {
                methodDeclaration(nombreClase);
            }
            // Verificar si es una declaración de variable
            else if (esDeclaracionVariable()) {
                String visVar = obtenerVisibilidad();
                varDeclaration(nombreClase, visVar);
            }
            // Bloque estático o inicializador
            else if (verificarLexema("static") && siguienteEs("{")) {
                match("static");
                match("{");
                while (!verificarLexema("}") && indiceActual < tokens.size()) {
                    if (esInicioDeSentencia()) {
                        statement();
                    } else {
                        avanzar();
                    }
                }
                match("}");
            }
            else {
                registrarError("Declaración no reconocida dentro de la clase.");
                avanzar();
            }

            if (errores.size() >= LIMITE_ERRORES) break;
        }

        match("}");
        claseActual = "";
    }

    private boolean esMetodoMain() {
        int i = indiceActual;
        
        // public static void main
        if (i < tokens.size() && tokens.get(i).getLexema().equals("public")) i++;
        else return false;
        
        if (i < tokens.size() && tokens.get(i).getLexema().equals("static")) i++;
        else return false;
        
        if (i < tokens.size() && tokens.get(i).getLexema().equals("void")) i++;
        else return false;
        
        if (i < tokens.size() && tokens.get(i).getLexema().equals("main")) return true;
        
        return false;
    }

    private void mainMethod() {
        match("public");
        match("static");
        match("void");
        match("main");
        match("(");
        
        // Parámetro: String[] args
        if (verificarLexema("String")) {
            match("String");
            match("[");
            match("]");
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba un identificador como parámetro de 'main'.");
            } else {
                avanzar();
            }
        }
        
        match(")");
        
        // Cuerpo del main
        if (!match("{")) return;

        while (indiceActual < tokens.size() && !verificarLexema("}")) {
            if (esDeclaracionVariable()) {
                varDeclaration(claseActual + ".main", "local");
            } else if (esInicioDeSentencia()) {
                statement();
            } else {
                registrarError("Token inesperado dentro del cuerpo del main.");
                avanzar();
            }
            
            if (errores.size() >= LIMITE_ERRORES) break;
        }

        match("}");
    }

    private boolean esDeclaracionMetodo() {
        int i = indiceActual;
        
        // Puede tener modificadores: public, private, protected, static, final, abstract
        while (i < tokens.size()) {
            String lex = tokens.get(i).getLexema();
            if (lex.equals("public") || lex.equals("private") || lex.equals("protected") ||
                lex.equals("static") || lex.equals("final") || lex.equals("abstract")) {
                i++;
            } else {
                break;
            }
        }
        
        // Debe tener un tipo de retorno
        if (i >= tokens.size()) return false;
        String tipoRetorno = tokens.get(i).getLexema();
        if (!esTipoValido(tipoRetorno) && !tokens.get(i).getTipo().equals("Identificador")) {
            return false;
        }
        i++;
        
        // Puede tener [] para arrays
        while (i + 1 < tokens.size() && tokens.get(i).getLexema().equals("[") && 
               tokens.get(i + 1).getLexema().equals("]")) {
            i += 2;
        }
        
        // Debe tener un identificador (nombre del método)
        if (i >= tokens.size() || !tokens.get(i).getTipo().equals("Identificador")) {
            return false;
        }
        i++;
        
        // Debe tener paréntesis de apertura
        if (i >= tokens.size() || !tokens.get(i).getLexema().equals("(")) {
            return false;
        }
        
        return true;
    }

    private boolean esTipoValido(String tipo) {
        return tipo.equals("int") || tipo.equals("boolean") || tipo.equals("String") || 
               tipo.equals("void") || tipo.equals("double") || tipo.equals("float") ||
               tipo.equals("char") || tipo.equals("byte") || tipo.equals("short") ||
               tipo.equals("long");
    }

    /**
     * Verifica si el siguiente conjunto de tokens forma una declaración de variable.
     */
    private boolean esDeclaracionVariable() {
        int i = indiceActual;
        
        // Puede tener modificadores
        while (i < tokens.size()) {
            String lex = tokens.get(i).getLexema();
            if (lex.equals("public") || lex.equals("private") || lex.equals("protected") ||
                lex.equals("static") || lex.equals("final")) {
                i++;
            } else {
                break;
            }
        }
        
        // Debe tener un tipo
        if (i >= tokens.size()) return false;
        String tipo = tokens.get(i).getLexema();
        if (!esTipoValido(tipo) && !tokens.get(i).getTipo().equals("Identificador")) {
            return false;
        }
        i++;
        
        // Puede tener [] para arrays
        while (i + 1 < tokens.size() && tokens.get(i).getLexema().equals("[") && 
               tokens.get(i + 1).getLexema().equals("]")) {
            i += 2;
        }
        
        // Debe tener un identificador
        if (i >= tokens.size() || !tokens.get(i).getTipo().equals("Identificador")) {
            return false;
        }
        i++;
        
        // Puede tener [] después del nombre (int arr[])
        while (i + 1 < tokens.size() && tokens.get(i).getLexema().equals("[") && 
               tokens.get(i + 1).getLexema().equals("]")) {
            i += 2;
        }
        
        // Debe terminar con ; o = (asignación)
        if (i >= tokens.size()) return false;
        String siguiente = tokens.get(i).getLexema();
        
        return siguiente.equals(";") || siguiente.equals("=") || siguiente.equals(",");
    }

    private String obtenerVisibilidad() {
        String vis = "default";
        
        if (verificarLexema("public")) {
            vis = "public";
            match("public");
        } else if (verificarLexema("private")) {
            vis = "private";
            match("private");
        } else if (verificarLexema("protected")) {
            vis = "protected";
            match("protected");
        }
        
        // Modificadores adicionales
        if (verificarLexema("static")) match("static");
        if (verificarLexema("final")) match("final");
        
        return vis;
    }

    private void varDeclaration(String claseContenedora, String visibilidad) {
        if (!esTipo()) {
            registrarError("Se esperaba un tipo de dato.");
            return;
        }

        String tipo = tokens.get(indiceActual).getLexema();
        avanzar();
        
        // Manejo de arrays: int[] o int []
        while (verificarLexema("[")) {
            match("[");
            match("]");
            tipo += "[]";
        }

        // Puede declarar múltiples variables: int a, b, c;
        do {
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba un identificador después del tipo de dato.");
                return;
            }

            Token tokenVar = tokens.get(indiceActual);
            String nombre = tokenVar.getLexema();
            avanzar();
            
            // Arrays estilo C: int arr[]
            String tipoFinal = tipo;
            while (verificarLexema("[")) {
                match("[");
                match("]");
                tipoFinal += "[]";
            }

            // Manejo de inicialización opcional
            String valor = "-";
            if (verificarLexema("=")) {
                match("=");
                
                // Capturar valor simple para la tabla
                if (verificarTipo("Entero") || verificarTipo("Decimal") || 
                    verificarTipo("Cadena") || verificarLexema("true") || 
                    verificarLexema("false") || verificarLexema("null")) {
                    valor = tokens.get(indiceActual).getLexema();
                }
                
                expression();
            }

            tablaSimbolos.add(new Simbolo(nombre, tipoFinal, claseContenedora, valor, visibilidad,
                "Línea " + tokenVar.getLinea() + ", Columna " + tokenVar.getColumna(), "Variable"));
            
            // Siguiente variable en la misma declaración
            if (verificarLexema(",")) {
                match(",");
            } else {
                break;
            }
        } while (true);

        match(";");
    }

    private void methodDeclaration(String claseContenedora) {
        String visibilidad = obtenerVisibilidad();
        String tipoRetorno = "";
        String nombreMetodo = "";
        Token tokenMetodo;

        // Tipo de retorno
        if (!esTipo()) {
            registrarError("Se esperaba un tipo de retorno.");
            return;
        }
        tipoRetorno = tokens.get(indiceActual).getLexema();
        avanzar();
        
        // Arrays en tipo de retorno
        while (verificarLexema("[")) {
            match("[");
            match("]");
            tipoRetorno += "[]";
        }

        // Nombre del método
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

        // Parámetros
        match("(");

        if (esTipo()) {
            String tipoParam = tokens.get(indiceActual).getLexema();
            avanzar();
            
            // Arrays en parámetros
            while (verificarLexema("[")) {
                match("[");
                match("]");
                tipoParam += "[]";
            }

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
                
                while (verificarLexema("[")) {
                    match("[");
                    match("]");
                    tipoParam += "[]";
                }
                
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

        // Cuerpo del método
        if (!match("{")) return;

        while (!verificarLexema("}") && indiceActual < tokens.size()) {
            if (esDeclaracionVariable()) {
                varDeclaration(claseContenedora + "." + nombreMetodo, "local");
            } else if (esInicioDeSentencia()) {
                statement();
            } else {
                registrarError("Token inesperado dentro del cuerpo del método.");
                avanzar();
            }
            
            if (errores.size() >= LIMITE_ERRORES) break;
        }

        match("}");
    }

    private void statement() {
        // Bloque de sentencias
        if (verificarLexema("{")) {
            match("{");
            while (indiceActual < tokens.size() && !verificarLexema("}")) {
                if (esDeclaracionVariable()) {
                    varDeclaration(claseActual, "local");
                } else if (esInicioDeSentencia()) {
                    statement();
                } else {
                    registrarError("Token inesperado en bloque.");
                    avanzar();
                }
                if (errores.size() >= LIMITE_ERRORES) break;
            }
            match("}");
            return;
        }

        // Sentencia if
        if (verificarLexema("if")) {
            match("if"); match("("); expression(); match(")"); statement();
            if (verificarLexema("else")) { match("else"); statement(); }
            return;
        }

        // Sentencia while
        if (verificarLexema("while")) {
            match("while"); match("("); expression(); match(")"); statement();
            return;
        }

        // Sentencia for
        if (verificarLexema("for")) {
            match("for"); match("(");
            
            if (esDeclaracionVariable()) {
                varDeclaration(claseActual, "local");
            } else if (!verificarLexema(";")) {
                expression();
                match(";");
            } else {
                match(";");
            }
            
            if (!verificarLexema(";")) {
                expression();
            }
            match(";");
            
            if (!verificarLexema(")")) {
                expression();
            }
            
            match(")");
            statement();
            return;
        }

        // do-while
        if (verificarLexema("do")) {
            match("do");
            statement();
            match("while");
            match("(");
            expression();
            match(")");
            match(";");
            return;
        }

        // switch
        if (verificarLexema("switch")) {
            match("switch");
            match("(");
            expression();
            match(")");
            match("{");
            
            while (verificarLexema("case") || verificarLexema("default")) {
                if (verificarLexema("case")) {
                    match("case");
                    expression();
                    match(":");
                } else {
                    match("default");
                    match(":");
                }
                
                while (!verificarLexema("case") && !verificarLexema("default") && 
                       !verificarLexema("}") && indiceActual < tokens.size()) {
                    if (verificarLexema("break")) {
                        match("break");
                        match(";");
                        break;
                    }
                    statement();
                }
            }
            
            match("}");
            return;
        }

        // try-catch
        if (verificarLexema("try")) {
            match("try");
            statement();
            
            while (verificarLexema("catch")) {
                match("catch");
                match("(");
                if (esTipo()) {
                    avanzar();
                    if (verificarTipo("Identificador")) {
                        avanzar();
                    }
                }
                match(")");
                statement();
            }
            
            if (verificarLexema("finally")) {
                match("finally");
                statement();
            }
            return;
        }

        // System.out.println() o System.out.print()
        if (verificarLexema("System")) {
            match("System"); match("."); match("out"); match(".");
            
            if (verificarLexema("println") || verificarLexema("print")) {
                avanzar();
            } else {
                registrarError("Se esperaba 'println' o 'print' después de 'System.out.'");
            }
            
            match("(");
            if (!verificarLexema(")")) {
                expression();
            }
            match(")"); 
            match(";");
            return;
        }

        // Sentencia return
        if (verificarLexema("return")) {
            match("return");
            if (!verificarLexema(";")) {
                expression();
            }
            match(";");
            return;
        }

        // break, continue
        if (verificarLexema("break") || verificarLexema("continue")) {
            avanzar();
            match(";");
            return;
        }

        // throw
        if (verificarLexema("throw")) {
            match("throw");
            expression();
            match(";");
            return;
        }

        // Asignaciones, llamadas a métodos o expresiones
        if (verificarTipo("Identificador")) {
            avanzar();

            // Operadores de incremento/decremento postfijos
            if (verificarLexema("++") || verificarLexema("--")) {
                avanzar();
                match(";");
                return;
            }

            // Acceso a miembros o métodos
            while (verificarLexema(".")) {
                match(".");
                if (!verificarTipo("Identificador")) {
                    registrarError("Se esperaba un identificador después de '.'");
                    break;
                }
                avanzar();
                
                if (verificarLexema("(")) {
                    match("(");
                    if (!verificarLexema(")")) {
                        while (true) {
                            expression();
                            if (verificarLexema(",")) match(",");
                            else break;
                        }
                    }
                    match(")");
                }
            }

            // Asignación simple
            if (verificarLexema("=")) {
                match("="); expression(); match(";");
            }
            // Operadores de asignación compuesta
            else if (verificarLexema("+=") || verificarLexema("-=") || verificarLexema("*=") || 
                     verificarLexema("/=") || verificarLexema("%=") || verificarLexema("&=") || 
                     verificarLexema("|=") || verificarLexema("^=") || verificarLexema("<<=") || 
                     verificarLexema(">>=") || verificarLexema(">>>=")) {
                avanzar();
                expression(); 
                match(";");
            }
            // Acceso/asignación a array
            else if (verificarLexema("[")) {
                match("["); expression(); match("]"); 
                
                if (verificarLexema("=")) {
                    match("="); expression();
                }
                match(";");
            } 
            // Llamada a método
            else if (verificarLexema("(")) {
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
            } 
            else {
                match(";");
            }
            return;
        }

        registrarError("Sentencia no reconocida.");
        avanzar();
    }

    private void expression() {
        simpleExpression();
        while (indiceActual < tokens.size()) {
            Token t = tokens.get(indiceActual);
            if (t.getTipo().equals("Operador") && esOperadorBinario(t.getLexema())) {
                avanzar();
                simpleExpression();
            } else break;
        }
    }

    private void simpleExpression() {
        // Operadores unarios prefijos
        if (verificarLexema("!") || verificarLexema("-") || verificarLexema("+") || 
            verificarLexema("~") || verificarLexema("++") || verificarLexema("--")) {
            avanzar();
        }

        if (verificarTipo("Identificador")) {
            avanzar();
            
            // Operadores postfijos
            if (verificarLexema("++") || verificarLexema("--")) {
                avanzar();
            }
            
            // Acceso a miembros
            while (verificarLexema(".")) {
                match(".");
                if (verificarLexema("length")) {
                    avanzar();
                } else if (verificarTipo("Identificador")) {
                    avanzar();
                    if (verificarLexema("(")) {
                        match("(");
                        if (!verificarLexema(")")) {
                            while (true) {
                                expression();
                                if (verificarLexema(",")) match(",");
                                else break;
                            }
                        }
                        match(")");
                    }
                }
            }
            
            // Acceso a arrays
            while (verificarLexema("[")) {
                match("[");
                expression();
                match("]");
            }
            
            // Llamada a método
            if (verificarLexema("(")) {
                match("(");
                if (!verificarLexema(")")) {
                    while (true) {
                        expression();
                        if (verificarLexema(",")) match(",");
                        else break;
                    }
                }
                match(")");
            }
        } 
        else if (verificarTipo("Entero") || verificarTipo("Decimal")) {
            avanzar();
        } 
        else if (verificarTipo("Cadena")) {
            avanzar();
        }
        else if (verificarTipo("Carácter")) {
            avanzar();
        }
        else if (verificarLexema("true") || verificarLexema("false") || 
                 verificarLexema("this") || verificarLexema("null")) {
            avanzar();
        } 
        else if (verificarLexema("(")) {
            match("("); 
            expression(); 
            match(")");
        } 
        else if (verificarLexema("new")) {
            match("new");
            if (!verificarTipo("Identificador")) {
                registrarError("Se esperaba un identificador después de 'new'.");
            } else {
                avanzar();
            }
            
            // Array o constructor
            if (verificarLexema("[")) {
                match("[");
                expression();
                match("]");
                
                // Arrays multidimensionales vacíos: new int[5][]
                while (verificarLexema("[")) {
                    match("[");
                    if (!verificarLexema("]")) {
                        expression();
                    }
                    match("]");
                }
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
            }
        } 
        else {
            registrarError("Expresión no reconocida.");
            if (indiceActual < tokens.size()) {
                avanzar();
            }
        }
    }

    private boolean esOperadorBinario(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%") ||
               op.equals("==") || op.equals("!=") || op.equals(">") || op.equals("<") || 
               op.equals(">=") || op.equals("<=") || 
               op.equals("&&") || op.equals("||") || 
               op.equals("&") || op.equals("|") || op.equals("^") ||
               op.equals("<<") || op.equals(">>") || op.equals(">>>") ||
               op.equals("instanceof");
    }

    // ------------------ MÉTODOS AUXILIARES ------------------

    private void avanzar() { 
        if (indiceActual < tokens.size()) indiceActual++; 
    }

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
        return esTipoValido(lex) || tipo.equals("Identificador");
    }

    private boolean esInicioDeSentencia() {
        if (indiceActual >= tokens.size()) return false;
        String lex = tokens.get(indiceActual).getLexema();
        return lex.equals("if") || lex.equals("while") || lex.equals("for") || 
               lex.equals("do") || lex.equals("switch") || lex.equals("try") ||
               lex.equals("System") || lex.equals("{") || lex.equals("return") || 
               lex.equals("break") || lex.equals("continue") || lex.equals("throw") ||
               verificarTipo("Identificador");
    }

    private void registrarError(String mensaje) {
        if (errores.size() >= LIMITE_ERRORES) return;
        if (indiceActual < tokens.size()) {
            Token t = tokens.get(indiceActual);
            errores.add(String.format(
                "Error sintáctico en línea %d, columna %d: %s (token: '%s')",
                t.getLinea(), t.getColumna(), mensaje, t.getLexema()));
        } else {
            errores.add("Error sintáctico: " + mensaje + " (fin de archivo).");
        }
    }

    public List<String> getErrores() { 
        return errores; 
    }
    
    public List<Simbolo> getTablaSimbolos() { 
        return tablaSimbolos; 
    }
}