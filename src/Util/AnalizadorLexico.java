package Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalizadorLexico {

    private List<Token> tokens;
    private List<String> errores;

    private static final Set<String> PALABRAS_CLAVE = new HashSet<>(Arrays.asList(
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","enum","extends","final",
        "finally","float","for","goto","if","implements","import","instanceof","int",
        "interface","long","native","new","package","private","protected","public",
        "return","short","static","strictfp","super","switch","synchronized","this",
        "throw","throws","transient","try","void","volatile","while","true","false","null",
        "System", "out", "print","println"
    ));

    private static final Set<String> SEPARADORES = new HashSet<>(Arrays.asList(
    "(", ")", "[", "]", "{", "}", ";", ",", ".", ":"
    ));

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

    public void analizar(String codigo) {
        tokens.clear();
        errores.clear();

        String[] lineas = codigo.split("\n");
        int numLinea = 1;

        for (String linea : lineas) {
            int columna = 1;
            StringBuilder lexema = new StringBuilder();
            boolean enCadena = false;
            boolean enComentarioLinea = false;

            for (int i = 0; i < linea.length(); i++) {
                char c = linea.charAt(i);

                // Detectar comentarios de línea //
                if (!enCadena && i + 1 < linea.length() && c == '/' && linea.charAt(i + 1) == '/') {
                    enComentarioLinea = true;
                    // Terminar token actual si existe
                    if (lexema.length() > 0) {
                        Token token = clasificarToken(lexema.toString(), numLinea, columna);
                        if (token != null) {
                            tokens.add(token);
                        } else {
                            errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                        }
                        lexema.setLength(0);
                    }
                    break; // Saltar el resto de la línea
                }

                // Si estamos en comentario de línea, ignorar todo
                if (enComentarioLinea) {
                    continue;
                }

                if (c == '"') {
                    lexema.append(c);

                    if (enCadena) {
                        Token token = clasificarToken(lexema.toString(), numLinea, columna);

                        if (token != null){
                            tokens.add(token);
                        } else {
                            errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                        }

                        lexema.setLength(0);
                    }

                    enCadena = !enCadena;
                }
                else if (enCadena) {
                    lexema.append(c);
                }
                else if (Character.isWhitespace(c)) {
                    if (lexema.length() > 0) {
                        Token token = clasificarToken(lexema.toString(), numLinea, columna);
                        if (token != null) {
                            tokens.add(token);
                        }
                        else {
                            errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                        }
                        
                        lexema.setLength(0);
                    }
                }
                else if (c == '.') {
                    if (lexema.length() > 0 && lexema.toString().matches("[0-9]+")) {
                        if (i + 1 < linea.length() && Character.isDigit(linea.charAt(i + 1))) {
                            lexema.append(c);
                        } else {
                            Token token = clasificarToken(lexema.toString(), numLinea, columna);
                            if (token != null) {
                                tokens.add(token);
                            } else {
                                errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                            }
                            lexema.setLength(0);
                            
                            Token tokenPunto = clasificarToken(".", numLinea, columna);
                            if (tokenPunto != null) {
                                tokens.add(tokenPunto);
                            }
                        }
                    } else {
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
                else if (esSeparadorOOperador(c)) {
                    if (lexema.length() > 0) {
                        Token token = clasificarToken(lexema.toString(), numLinea, columna);
                        if (token != null) {
                            tokens.add(token);
                        }
                        else {
                            errores.add("Error léxico en línea " + numLinea + ": '" + lexema + "' no reconocido.");
                        }
                        
                        lexema.setLength(0);
                    }
                    
                    StringBuilder posibleOperador = new StringBuilder();
                    posibleOperador.append(c);
                    
                    if (i + 1 < linea.length()) {
                        char siguienteChar = linea.charAt(i + 1);
                        
                        if (c == '.' && Character.isDigit(siguienteChar)) {
                            // Ya se manejó arriba
                        } else {
                            String posibleOperadorCompuesto = posibleOperador.toString() + siguienteChar;
                            
                            if (OPERADORES.contains(posibleOperadorCompuesto)) {
                                Token tokenOp = clasificarToken(posibleOperadorCompuesto, numLinea, columna);
                                if (tokenOp != null) {
                                    tokens.add(tokenOp);
                                    i++;
                                    continue;
                                }
                            }
                            
                            if (i + 2 < linea.length()) {
                                char tercerChar = linea.charAt(i + 2);
                                String posibleOperadorTriple = posibleOperadorCompuesto + tercerChar;
                                if (OPERADORES.contains(posibleOperadorTriple)) {
                                    Token tokenOp = clasificarToken(posibleOperadorTriple, numLinea, columna);
                                    if (tokenOp != null) {
                                        tokens.add(tokenOp);
                                        i += 2;
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                    
                    Token token = clasificarToken(String.valueOf(c), numLinea, columna);
                    if (token != null) {
                        tokens.add(token);
                    }
                    else {
                        errores.add("Error léxico en línea " + numLinea + ": '" + c + "' no reconocido.");
                    }
                }
                else {
                    lexema.append(c);
                }
            }

            if (lexema.length() > 0 && !enComentarioLinea) {
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

    private boolean esSeparadorOOperador(char c) {
        if (c == '.') return false;
        String str = String.valueOf(c);
        return SEPARADORES.contains(str) || OPERADORES.stream().anyMatch(op -> op.startsWith(str));
    }

    private Token clasificarToken(String lexema, int linea, int columna) {

        if (PALABRAS_CLAVE.contains(lexema))
            return new Token("Palabra Clave", lexema, linea, columna);

        if (lexema.matches("[A-Za-z_$][A-Za-z0-9_$]*"))
            return new Token("Identificador", lexema, linea, columna);

        if (lexema.matches("[0-9]+"))
            return new Token("Entero", lexema, linea, columna);

        if (lexema.matches("[0-9]+\\.[0-9]+"))
            return new Token("Decimal", lexema, linea, columna);

        if (lexema.matches("\".*\""))
            return new Token("Cadena", lexema, linea, columna);

        if (lexema.matches("'(\\\\.|[^\\\\'])'"))
            return new Token("Carácter", lexema, linea, columna);

        if (SEPARADORES.contains(lexema))
            return new Token("Separador", lexema, linea, columna);

        if (OPERADORES.contains(lexema))
            return new Token("Operador", lexema, linea, columna);

        return null;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<String> getErrores() {
        return errores;
    }
}