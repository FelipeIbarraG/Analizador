# Analizador Sintáctico

## 📋 Descripción

Este programa implementa un **analizador sintáctico** (parser) que simula el comportamiento del componente sintáctico de un compilador. El analizador toma los tokens generados por el analizador léxico y verifica que cumplan con las reglas gramaticales definidas en la gramática BNF de MiniJava, detectando y reportando errores sintácticos con ubicación exacta.

## ✨ Características

- **Análisis sintáctico completo**: Verifica que la secuencia de tokens cumpla con las reglas gramaticales del BNF
- **Método recursivo descendente**: Implementa un parser descendente recursivo para validar la estructura del código
- **Detección de errores**: Detecta y reporta errores sintácticos con ubicación exacta (línea y columna del token)
- **Límite de errores**: Se detiene después de detectar 2 errores sintácticos para facilitar la depuración
- **Enriquecimiento de tabla de símbolos**: Completa la tabla de símbolos con información semántica (clase, visibilidad, rol)
- **Integración con interfaz**: Utiliza los tokens del analizador léxico para realizar el análisis sintáctico
- **Exportación de resultados**: Permite guardar errores sintácticos en archivos de texto

## 🏗️ Estructura del Proyecto

```
Analizador/
├── src/
│   ├── App.java                       # Punto de entrada de la aplicación
│   ├── UI/
│   │   └── View.java                  # Interfaz gráfica (Swing)
│   └── Util/
│       ├── AnalizadorLexico.java      # Lógica del analizador léxico
│       ├── AnalizadorSintactico.java  # Lógica del analizador sintáctico
│       └── Token.java                 # Clase que representa un token
├── bin/                               # Archivos compilados (.class)
├── lib/                               # Bibliotecas externas
└── Analizador.jar                     # Ejecutable JAR
```

## 📚 Gramática BNF (MiniJava)

El analizador sintáctico implementa la siguiente gramática:

### Reglas No Terminales

1. **Goal**: Punto de entrada del programa
   - `MainClass ( ClassDeclaration )* <EOF>`

2. **MainClass**: Clase principal con método main
   - `( "public" )? "class" Identifier "{" "public" "static" "void" "main" "(" "String" "[" "]" Identifier ")" "{" ( VarDeclaration | Statement )* "}" "}"`

3. **ClassDeclaration**: Declaración de clases
   - `"class" Identifier ( "extends" Identifier )? "{" ( VarDeclaration )* ( MethodDeclaration )* "}"`

4. **VarDeclaration**: Declaración de variables
   - `Type Identifier ";"`

5. **MethodDeclaration**: Declaración de métodos
   - `"public" Type Identifier "(" ( Type Identifier ( "," Type Identifier )* )? ")" "{" ( VarDeclaration )* ( Statement )* "return" Expression ";" "}"`

6. **Type**: Tipos de datos
   - `"int" "[" "]" | "boolean" | "int" | Identifier`

7. **Statement**: Sentencias del lenguaje
   - Bloques `{ Statement* }`
   - Condicionales `if ( Expression ) Statement else Statement`
   - Bucles `while ( Expression ) Statement`
   - Impresión `System.out.println ( Expression ) ;`
   - Asignaciones `Identifier = Expression ;` o `Identifier [ Expression ] = Expression ;`

8. **Expression**: Expresiones
   - Operadores binarios: `Expression ( "&&" | "<" | "+" | "-" | "*" ) Expression`
   - Acceso a arrays: `Expression [ Expression ]`
   - Métodos y propiedades: `Expression . "length"` y `Expression . Identifier ( ( Expression ( "," Expression )* )? )`
   - Literales: Enteros, cadenas, booleanos (`true`, `false`)
   - Identificadores, `this`, `new`, operador `!`, expresiones entre paréntesis

## 🚀 Flujo de Análisis Sintáctico

1. **Inicio**: El usuario carga código y presiona el botón "Análisis Sintáctico"
2. **Análisis léxico previo**: Se ejecuta el analizador léxico para obtener tokens
3. **Análisis sintáctico**: El método `analizar()` del `AnalizadorSintactico` procesa los tokens:
   - Inicia desde el no terminal `Goal`
   - Recorre los tokens de izquierda a derecha
   - Para cada regla gramatical, verifica que los tokens coincidan
   - Implementa métodos recursivos descendentes para cada no terminal
4. **Detección de errores**: Cuando un token no coincide con lo esperado:
   - Se registra un error sintáctico con línea y columna
   - Se intenta recuperación de errores
   - Se detiene después de 2 errores detectados
5. **Resultados**: Los errores se muestran en la zona de errores sintácticos

## 🔧 Detalles Técnicos

### Métodos Principales

- **`analizar(List<Token> tokens)`**: Método principal que inicia el análisis desde Goal
- **`goal()`**: Valida la estructura principal del programa (MainClass + ClassDeclaration*)
- **`mainClass()`**: Verifica la clase principal con el método main
- **`classDeclaration()`**: Valida declaraciones de clases opcionales
- **`varDeclaration()`**: Verifica declaraciones de variables
- **`methodDeclaration()`**: Valida declaraciones de métodos
- **`type()`**: Determina el tipo de dato
- **`statement()`**: Analiza diferentes tipos de sentencias
- **`expression()`**: Evalúa expresiones recursivamente
- **`match(String tipoEsperado)`**: Verifica que el token actual coincida con el esperado
- **`reportarError(String mensaje)`**: Registra errores sintácticos encontrados

### Manejo de Errores

- **Recuperación de errores**: El analizador intenta continuar después de detectar un error
- **Información detallada**: Cada error incluye el tipo de error, ubicación y token encontrado vs esperado
- **Límite de errores**: Se detiene después de 2 errores para facilitar la depuración
- **Tokens perdidos**: Si falta un token, se reporta el error y se intenta continuar

## 📝 Ejemplo de Uso

### Código Válido
El programa debe de ser capaz de analizar el codigo siguiente sin ningun problema.

```java
public class Ejemplo {
    public static void main(String[] args) {
        int x = 10;
        if (x > 5) {
            System.out.println("Hola");
        }
    }
}
```

**Resultado del análisis**: Sin errores sintácticos

### Código con Error Sintáctico

```java
public class Ejemplo {
    public static void main(String[] args) {
        int x = 10  // Falta punto y coma
        if (x > 5 {
            System.out.println("Hola");
        }
    }
}
```

**Resultado del análisis**:
- Error 1: Línea 3, Columna 12 - Se esperaba `;` pero se encontró `if`
- Error 2: Línea 4, Columna 14 - Se esperaba `)` pero se encontró `{`

## 🔍 Componentes Sintácticos Validados

### Estructuras
- Clases y herencia
- Métodos con parámetros y retorno
- Variables locales y globales
- Bloques de código

### Sentencias
- Declaraciones de variables
- Asignaciones simples y a arrays
- Condicionales if-else
- Bucles while
- Llamadas a System.out.println

### Expresiones
- Operadores aritméticos, lógicos y relacionales
- Acceso a arrays y propiedades
- Llamadas a métodos
- Literales (enteros, cadenas, booleanos)
- Expresiones entre paréntesis

## 🔗 Integración con Analizador Léxico

El analizador sintáctico:
- Recibe la lista de tokens generados por `AnalizadorLexico`
- Utiliza la información de línea y columna de cada token para reportar errores
- No modifica los tokens, solo los consume en orden
- Requiere análisis léxico exitoso para funcionar correctamente

## 📌 Notas

- El analizador implementa la gramática completa de MiniJava según el BNF proporcionado
- Se requieren tokens válidos del analizador léxico para el análisis sintáctico
- La recuperación de errores es limitada y puede generar errores en cascada
- El límite de 2 errores se puede configurar según las necesidades del proyecto