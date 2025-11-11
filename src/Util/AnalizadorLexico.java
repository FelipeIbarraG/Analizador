package Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Cosas por hacer
// Separar utilizando puntos, agregar validacion de cuando hay comentarios
// Encontre un pequeño error de cuando el usuario pode una sola palabra lo toma como identificador
// Personalizar los erroes

public class AnalizadorLexico {

    private List<Token> tokens;
    private List<String> errores;

    // Utilizar "HashSet" porque son palabras unicas e irrepetibles
    private static final Set<String> PALABRAS_CLAVE = new HashSet<>(Arrays.asList(
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","enum","extends","final",
        "finally","float","for","goto","if","implements","import","instanceof","int",
        "interface","long","native","new","package","private","protected","public",
        "return","short","static","strictfp","super","switch","synchronized","this",
        "throw","throws","transient","try","void","volatile","while","true","false","null",
        "System", "out", "print","return"
    ));

    // Separadores 
    private static final Set<String> SEPARADORES = new HashSet<>(Arrays.asList(
    "(", ")", "[", "]", "{", "}", ";", ",", ".", ":"
    ));

    // Operadores 
    private static final Set<String> OPERADORES = new HashSet<>(Arrays.asList(
        "+","-","*","=","/","%","++","--","==","!=",
        ">","<",">=","<=","&&","||","!","&","|","^","~",
        "<<",">>",">>>","+=","-=","*=","/=","%=",
        "&=","|=","^=","<<=",">>=",">>>=","->","::","@"
    ));


    public AnalizadorLexico() {
        tokens = new ArrayList<>();
        errores = new ArrayList<>();
    }

    // Logica que maneja el analizador por ahora 
    /*
        1. Si es una comilla (") maneja cadenas.
        2. Si estás dentro de una cadena, agrega todo directamente.
        3. Si es un espacio o tabulacoin (Character.isWhitespace(c)) o una ";", terminaa un token.
        4. Si no es nada de eso sigue construyendo el lexema con lexema.append(c).
    */

    public void analizar(String codigo) {
        tokens.clear();
        errores.clear();

        String[] lineas = codigo.split("\n"); // Dividir el codigo en lineas
        int numLinea = 1;

        for (String linea : lineas) {
            int columna = 1;
            StringBuilder lexema = new StringBuilder();
            boolean enCadena = false; // para ver si estamos dentro de comillas

            for (int i = 0; i < linea.length(); i++) {
                char c = linea.charAt(i);

                if (c == '"') {
                    lexema.append(c); // Agregar al stringBuilder

                    // Si hay comillas significa que se quiere hacer una cadena
                    if (enCadena) {
                        // cerramos cadena
                        Token token = clasificarToken(lexema.toString(), numLinea, columna);

                        if (token != null){
                            tokens.add(token);
                        } else {
                            errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocdo.");
                        }

                        lexema.setLength(0); // Limpiar el stringBuilder
                    }

                    // Cambiar estado del boleano para la siguiente 
                    enCadena = !enCadena;
                }
                else if (enCadena) {
                    // Si estan las comillas agregar todo
                    lexema.append(c); // Agregar al stringBuilder
                }

                // Si hay una espacio clasificar el lexema
                else if (Character.isWhitespace(c)) {
                    // Fin de un token
                    if (lexema.length() > 0) {
                        Token token = clasificarToken(lexema.toString(), numLinea, columna);
                        if (token != null) {
                            tokens.add(token);
                        }
                        else {
                            errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                        }
                        
                        lexema.setLength(0); // Limpiar el stringBuilder
                    }
                }
                // Manejar el punto "." de manera especial (puede ser decimal o separador)
                else if (c == '.') {
                    // Si el lexema actual es solo dígitos, podría ser un decimal
                    if (lexema.length() > 0 && lexema.toString().matches("[0-9]+")) {
                        // Verificar si el siguiente carácter es un dígito (decimal válido)
                        if (i + 1 < linea.length() && Character.isDigit(linea.charAt(i + 1))) {
                            // Continuar construyendo el decimal
                            lexema.append(c);
                        } else {
                            // No es un decimal, terminar el número y luego el punto como separador
                            Token token = clasificarToken(lexema.toString(), numLinea, columna);
                            if (token != null) {
                                tokens.add(token);
                            } else {
                                errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                            }
                            lexema.setLength(0);
                            
                            // Procesar el punto como separador
                            Token tokenPunto = clasificarToken(".", numLinea, columna);
                            if (tokenPunto != null) {
                                tokens.add(tokenPunto);
                            }
                        }
                    } else {
                        // No es parte de un decimal, procesar como separador
                        if (lexema.length() > 0) {
                            Token token = clasificarToken(lexema.toString(), numLinea, columna);
                            if (token != null) {
                                tokens.add(token);
                            } else {
                                errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                            }
                            lexema.setLength(0);
                        }
                        
                        Token tokenPunto = clasificarToken(".", numLinea, columna);
                        if (tokenPunto != null) {
                            tokens.add(tokenPunto);
                        }
                    }
                }
                // Detectar separadores y operadores individuales (excepto el punto que ya se maneja arriba)
                else if (esSeparadorOOperador(c)) {
                    // Primero, terminar el token actual si existe
                    if (lexema.length() > 0) {
                        Token token = clasificarToken(lexema.toString(), numLinea, columna);
                        if (token != null) {
                            tokens.add(token);
                        }
                        else {
                            errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                        }
                        
                        lexema.setLength(0); // Limpiar el stringBuilder
                    }
                    
                    // Manejar operadores de múltiples caracteres (ej: ++, --, ==, <=, >=, etc.)
                    StringBuilder posibleOperador = new StringBuilder();
                    posibleOperador.append(c);
                    
                    // Verificar si el siguiente carácter también es parte de un operador compuesto
                    if (i + 1 < linea.length()) {
                        char siguienteChar = linea.charAt(i + 1);
                        
                        // No procesar como operador compuesto si el siguiente es parte de un número decimal
                        if (c == '.' && Character.isDigit(siguienteChar)) {
                            // Ya se manejó arriba
                        } else {
                            String posibleOperadorCompuesto = posibleOperador.toString() + siguienteChar;
                            
                            // Verificar si es un operador de dos caracteres
                            if (OPERADORES.contains(posibleOperadorCompuesto)) {
                                Token tokenOp = clasificarToken(posibleOperadorCompuesto, numLinea, columna);
                                if (tokenOp != null) {
                                    tokens.add(tokenOp);
                                    i++; // Saltar el siguiente carácter ya que lo procesamos
                                    continue;
                                }
                            }
                            
                            // Verificar si es un operador de tres caracteres (ej: >>>, <<=)
                            if (i + 2 < linea.length()) {
                                char tercerChar = linea.charAt(i + 2);
                                String posibleOperadorTriple = posibleOperadorCompuesto + tercerChar;
                                if (OPERADORES.contains(posibleOperadorTriple)) {
                                    Token tokenOp = clasificarToken(posibleOperadorTriple, numLinea, columna);
                                    if (tokenOp != null) {
                                        tokens.add(tokenOp);
                                        i += 2; // Saltar los siguientes dos caracteres
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Si no es un operador compuesto, procesar el carácter individual
                    Token token = clasificarToken(String.valueOf(c), numLinea, columna);
                    if (token != null) {
                        tokens.add(token);
                    }
                    else {
                        errores.add("Error léxico en línea " + numLinea + ": '" + c + "' no reconocido.");
                    }
                }

                // Seguir construyendo el lexema
                else {
                    // Cualquier otro carácter
                    lexema.append(c);
                }
            }

            // Si no se cierra las comillas
            if (lexema.length() > 0) {
                Token token = clasificarToken(lexema.toString(), numLinea, columna);
                if (token != null) {
                    tokens.add(token);
                }
                else {
                    errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                }
            }

            numLinea++;
        }
    }


    // Método auxiliar para verificar si un carácter es separador o operador
    private boolean esSeparadorOOperador(char c) {
        if (c == '.') return false; // El punto se maneja de manera especial
        String str = String.valueOf(c);
        return SEPARADORES.contains(str) || OPERADORES.stream().anyMatch(op -> op.startsWith(str));
    }

    private Token clasificarToken(String lexema, int linea, int columna) {

        // --- 1. Palabras clave ---
        if (PALABRAS_CLAVE.contains(lexema))
            return new Token("Palabra Clave", lexema, linea, columna);

        // --- 2. Identificadores ---
        // Regla: comienzan con letra, _ o $, y pueden contener letras, dígitos, _ o $
        // esto lo agregeu porque hay variables que pueden empezar con 
        // $ConsultaSQL, o _Variable2 y asi...
        // ademas al final agregamos la cerradura de Kleene para decir que puede haber repeticiones infinitas
        if (lexema.matches("[A-Za-z_$][A-Za-z0-9_$]*"))
            return new Token("Identificador", lexema, linea, columna);

        // --- 3. Números enteros ---
        // Para numeros sin punto decimal
        // Y cerradura positiva que obliga a que exista una o mas repeticiones
        if (lexema.matches("[0-9]+"))
            return new Token("Entero", lexema, linea, columna);

        // --- 4. Números decimales ---
        // Números que contienen un punto y al menos un dígito después
        // esto lo tuve que investigar porque no sabia como poner un punto, resulta que
        // simplemente se agregan \\ para poner algo literal coomo el "."
        if (lexema.matches("[0-9]+\\.[0-9]+"))
            return new Token("Decimal", lexema, linea, columna);

        // --- 5. Cadenas ---
        // Para texto en comillas dobles como "Hola mundo"
        // las comillas tienen que ponerse como " \" "
        // Caracteres de cualquier tipo se pone como " .* "
        if (lexema.matches("\".*\""))
            return new Token("Cadena", lexema, linea, columna);

        // --- 6. Caracteres ---
        // Caracteres que estan en comillas simples
        // "\\\\." significa que tome cualquier caracter que contenga una \ (el punto es lo que sea) como \n, \t etc.
        // el simbolo "|" es un "or" y la ^ significa negacion osea que no puede haber otra \ o '
        if (lexema.matches("'(\\\\.|[^\\\\'])'"))
            return new Token("Carácter", lexema, linea, columna);

        // --- 7. Separadores ---
        if (SEPARADORES.contains(lexema))
            return new Token("Separador", lexema, linea, columna);

        // --- 8. Operadores ---
        if (OPERADORES.contains(lexema))
            return new Token("Operador", lexema, linea, columna);

        // --- 9. No reconocido ---
        return null;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<String> getErrores() {
        return errores;
    }
}
