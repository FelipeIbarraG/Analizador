# Analizador Léxico

## 📋 Descripción

Este programa implementa un **analizador léxico** (scanner) que simula el comportamiento del componente léxico de un compilador. El analizador toma código fuente y lo divide en tokens (unidades léxicas), identificando palabras clave, identificadores, operadores, números, cadenas, separadores y detectando errores léxicos.

## ✨ Características

- **Tokenización completa**: Identifica y clasifica todos los componentes léxicos del código fuente
- **Detección de errores**: Detecta y reporta errores léxicos con ubicación exacta (línea y columna)
- **Tabla de símbolos**: Construye y visualiza una tabla de símbolos con identificadores, tipos, valores y posiciones
- **Interfaz gráfica intuitiva**: Interfaz desarrollada con Java Swing que incluye:
  - Editor de código con resaltado
  - Visualización de tokens en tabla interactiva
  - Zona de errores léxicos
  - Tabla de símbolos
- **Gestión de archivos**: Permite abrir, guardar y analizar archivos desde la interfaz
- **Exportación de resultados**: Guarda resultados en formato de texto (código, errores, tablas)

## 🏗️ Estructura del Proyecto

```
AnalizadorLexico/
├── src/
│   ├── App.java                    # Punto de entrada de la aplicación
│   ├── UI/
│   │   └── View.java               # Interfaz gráfica (Swing)
│   └── Util/
│       ├── AnalizadorLexico.java   # Lógica del analizador léxico
│       └── Token.java              # Clase que representa un token
├── bin/                            # Archivos compilados (.class)
├── lib/                            # Bibliotecas externas
└── Analizador.jar                  # Ejecutable JAR

```

## 🔍 Componentes Léxicos Reconocidos

### Tipos de Tokens

1. **Palabras Clave**: Todas las palabras reservadas de Java (`class`, `public`, `static`, `void`, `int`, `boolean`, `if`, `else`, `while`, `return`, etc.)
2. **Identificadores**: Variables y nombres que siguen la expresión regular `[A-Za-z_$][A-Za-z0-9_$]*`
3. **Números Enteros**: Secuencias de dígitos `[0-9]+`
4. **Números Decimales**: Números con punto decimal `[0-9]+\.[0-9]+`
5. **Cadenas**: Texto entre comillas dobles `".*"`
6. **Caracteres**: Caracteres entre comillas simples `'(\\.|[^\\\\'])'`
7. **Separadores**: `(`, `)`, `[`, `]`, `{`, `}`, `;`, `,`, `.`, `:`
8. **Operadores**: Aritméticos, lógicos, relacionales y de asignación (`+`, `-`, `*`, `/`, `%`, `==`, `!=`, `&&`, `||`, `++`, `--`, etc.)

## 🚀 Flujo de Análisis Léxico

1. **Inicio**: El usuario carga código o lo escribe en el editor y presiona el botón "Análisis Léxico"
2. **Análisis**: El método `analizar()` del `AnalizadorLexico` procesa el código:
   - Divide el código en líneas
   - Recorre carácter por carácter
   - Construye lexemas hasta encontrar un delimitador
   - Clasifica cada lexema según su tipo
3. **Tokenización**: Cada lexema se envía a `clasificarToken()` que:
   - Compara con palabras clave, separadores y operadores
   - Valida mediante expresiones regulares (identificadores, números, cadenas)
   - Crea objetos `Token` con tipo, lexema, línea y columna
4. **Resultados**: Los tokens se muestran en:
   - **Tabla de Lexemas**: Lista todos los tokens encontrados (Lexema / Tipo)
   - **Tabla de Símbolos**: Muestra identificadores con su información
   - **Zona de Errores**: Lista errores léxicos detectados


### Funcionalidades de la Interfaz

- **Menú Archivo**:
  - `Abrir código`: Carga un archivo `.txt` con código fuente
  - `Guardar código`: Guarda el código del editor en un archivo
  
- **Menú Errores**:
  - `Guardar errores`: Exporta la lista de errores léxicos a un archivo de texto
  
- **Menú Tablas**:
  - `Guardar tabla de lexemas`: Exporta la tabla de tokens a un archivo
  - `Guardar tabla de símbolos`: Exporta la tabla de símbolos a un archivo

- **Botón Análisis Léxico**: Ejecuta el análisis sobre el código en el editor

## 🔧 Detalles Técnicos

### Manejo Especial de Casos

- **Cadenas**: Maneja correctamente cadenas entre comillas dobles, incluyendo espacios
- **Punto decimal**: Distingue entre el punto como separador (`.`) y como parte de números decimales (`3.14`)
- **Operadores compuestos**: Reconoce operadores de múltiples caracteres (`++`, `--`, `==`, `<=`, `>=`, `&&`, `||`, etc.)
- **Caracteres especiales**: Soporta caracteres especiales en identificadores (`$`, `_`)
- **Localización de errores**: Cada error incluye la línea y columna donde ocurre

### Clases Principales

- **`Token`**: Representa un token con tipo, lexema, línea y columna
- **`AnalizadorLexico`**: Contiene la lógica de análisis y clasificación de tokens
- **`View`**: Interfaz gráfica completa con editor, tablas y menús

## 📝 Ejemplo de Uso

```java
public class Ejemplo {
    public static void main(String[] args) {
        int x = 10;
        String mensaje = "Hola mundo";
        if (x > 5) {
            System.out.println(mensaje);
        }
    }
}
```

**Resultado del análisis**:
- Tokens identificados: `public`, `class`, `Ejemplo`, `{`, `public`, `static`, `void`, `main`, `(`, `String`, `[`, `]`, `args`, `)`, etc.
- Errores: Ninguno (código válido)


## 📌 Notas

- El analizador actualmente procesa código Java o MiniJava
- La tabla de símbolos se completa principalmente con información del análisis léxico básico
- Errores léxicos se detectan cuando un carácter o secuencia no coincide con ningún patrón reconocido

---
