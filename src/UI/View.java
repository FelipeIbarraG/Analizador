package UI;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane; 
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import Util.AnalizadorLexico;
import Util.AnalizadorSintactico;
import Util.Simbolo;
import Util.Token;

public class View extends JFrame implements ActionListener{

    private static final long serialVersionUID = 1L;

    private JPanel panelPrincipal;
    

    //MENU
    private JMenuBar JMMenu;
    private JMenu JMFile, JMError, JMTables;
    private JMenuItem JMIOpen, JMISave, JMISaveError, JMISaveLexemas, JMISaveSimbolos;

    // CONTENIDO SUPERIOR
    private JTextArea codigoArea;
    
    private JTextArea erroresArea;

    private JSplitPane splitSuperior;

    // CONTENIDO INFERIOR
    private JTable tablaLexemas; 

    private JTable tablaSimbolos;

    private JSplitPane splitTablas;

    // CONTENIDO SUR (BOTON)
    private JPanel panelBoton;
    private JButton btnAnalisisLexico;
    private JButton btnAnalisisSintactico;

    public View(String title) {
        super(title);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        InitComponents();
    }

    private void Menu() {
        JMMenu = new JMenuBar();

        // ------------------ ARCHIVO ------------------
        JMFile = new JMenu("Archivo");
        JMFile.setMnemonic('A');

        JMIOpen = new JMenuItem("Abrir código", 'A');
        JMIOpen.addActionListener(this);
        JMFile.add(JMIOpen);

        JMISave = new JMenuItem("Guardar código", 'G');
        JMISave.addActionListener(this);
        JMFile.add(JMISave);

        // ------------------ ERRORES ------------------
        JMError = new JMenu("Errores");
        JMError.setMnemonic('E');

        JMISaveError = new JMenuItem("Guardar errores", 'E');
        JMISaveError.addActionListener(this);
        JMError.add(JMISaveError);

        // ------------------ TABLAS ------------------
        JMTables = new JMenu("Tablas");
        JMTables.setMnemonic('T');

        JMISaveLexemas = new JMenuItem("Guardar tabla de lexemas", 'L');
        JMISaveLexemas.addActionListener(this);
        JMTables.add(JMISaveLexemas);

        JMISaveSimbolos = new JMenuItem("Guardar tabla de símbolos", 'S');
        JMISaveSimbolos.addActionListener(this);
        JMTables.add(JMISaveSimbolos);

        // ------------------ AGREGAR AL MENU PRINCIPAL ------------------
        JMMenu.add(JMFile);
        JMMenu.add(JMError);
        JMMenu.add(JMTables);

        setJMenuBar(JMMenu);
    }

    private void InitComponents() {
        
        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(panelPrincipal);

        // ---------- 0. Menu (cinta de opciones) ----------
        Menu();

        // ---------- 1. Contenido Superior ----------
        ContenidoSuperior();

        // ---------- 2. Contenido Inferior ----------
        ContenidoInferior();
        
        // Crear el JSplitPane que divida vertical el splitSuperior y el splitTablas
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitSuperior, splitTablas);
        splitPrincipal.setDividerLocation(0.55);
        splitPrincipal.setResizeWeight(0.55);
        splitPrincipal.setOneTouchExpandable(true);
        
        // Añadir el JSplitPane principal en el centro del panelPrincipal
        panelPrincipal.add(splitPrincipal, BorderLayout.CENTER);

        // ---------- 3. Zona inferior: Botón ----------
        ContenidoSur();
    }

    private void ContenidoSuperior() {
        // Zona de código
        codigoArea = new JTextArea(15,20); // Solo estetico tamaño del text area
        JScrollPane scrollCodigo = new JScrollPane(codigoArea);
        scrollCodigo.setBorder(BorderFactory.createTitledBorder("Editor de Código (Zona de Entrada)"));

        // Zona de errores
        // MOSTRAR DONDE ESTA EL ERROR, QUE LINEA FUE Y EL TOKEN DE ERROR
        erroresArea = new JTextArea();
        erroresArea.setEditable(false);
        JScrollPane scrollErrores = new JScrollPane(erroresArea);
        scrollErrores.setBorder(BorderFactory.createTitledBorder("Zona de Errores"));
        
        // JSplitPane Horizontal para Código y Errores
        splitSuperior = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollCodigo, scrollErrores);
        splitSuperior.setDividerLocation(0.5); 
        splitSuperior.setResizeWeight(0.5); 
        splitSuperior.setOneTouchExpandable(true);
    }

    private void ContenidoInferior() {
        // Tabla de lexemas
        String[] columnasLexemas = {"Lexema", "Componente Léxico"};
        DefaultTableModel modeloLexemas = new DefaultTableModel(columnasLexemas, 0);
        tablaLexemas = new JTable(modeloLexemas);
        JScrollPane scrollLexemas = new JScrollPane(tablaLexemas);
        scrollLexemas.setBorder(BorderFactory.createTitledBorder("Zona de Lexemas y Componentes Léxicos"));

        // Tabla de símbolos
        
        // Identificador: nombre
        // Tipo: int, boolean, String, etc.
        // Clase: clase contenedora
        // Valor: inicialización, si hay
        // Visibilidad: public, private, protected
        // Posición: línea/columna del Token
        // Rol: variable, clase, método, etc.

        String[] columnasSimbolos = {"Identificador", "Tipo", "Clase", "Valor", "Visibilidad", "Posición", "Rol"};
        DefaultTableModel modeloSimbolos = new DefaultTableModel(columnasSimbolos, 0);
        tablaSimbolos = new JTable(modeloSimbolos);
        JScrollPane scrollSimbolos = new JScrollPane(tablaSimbolos);
        scrollSimbolos.setBorder(BorderFactory.createTitledBorder("Tabla de Símbolos"));
        
        splitTablas = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollLexemas, scrollSimbolos);
        splitTablas.setDividerLocation(0.5); 
        splitTablas.setResizeWeight(0.5);
        splitTablas.setOneTouchExpandable(true);
    }

    private void ContenidoSur() {
        btnAnalisisLexico = new JButton("Análisis Léxico");
        btnAnalisisSintactico = new JButton("Análisis Sintáctico");
        
        panelBoton = new JPanel();
        panelBoton.add(btnAnalisisLexico);
         panelBoton.add(btnAnalisisSintactico);

        btnAnalisisLexico.addActionListener(this);
        btnAnalisisSintactico.addActionListener(this);

        panelPrincipal.add(panelBoton, BorderLayout.SOUTH);
    }

    public void OpenFile(){
        // CONFIRMAR EL CARGADO DEL ARCHIVO
        int resultado = JOptionPane.showConfirmDialog(
            this,
            "Al cargar un archivo se reemplazará el campo por completo",
            "Cargar un archivo",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (resultado == JOptionPane.CANCEL_OPTION) { return; }

        JFileChooser jfcAbrir = new JFileChooser();
        jfcAbrir.setDialogTitle("Cargar Archivo");
        jfcAbrir.setFileSelectionMode(JFileChooser.FILES_ONLY);

         // FILTRO
        FileNameExtensionFilter filtrotxt = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        jfcAbrir.addChoosableFileFilter(filtrotxt);
        jfcAbrir.setFileFilter(filtrotxt);

        if(jfcAbrir.showOpenDialog(this) != JFileChooser.CANCEL_OPTION){
            File archivo = jfcAbrir.getSelectedFile();

            // Verificar que el archivo no este vacio
            if (!archivo.exists() || archivo.length() == 0) {
                System.out.println("Archivo vacio!.");
                return;
            }

            // LIMPIAR ZONA DE CODIGO
            codigoArea.setText("");

            try(BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                StringBuilder contenido = new StringBuilder();
                String linea;

                while ((linea = br.readLine()) != null) {
                    contenido.append(linea).append("\n");
                }

                codigoArea.setText(contenido.toString());
                System.out.println("Archivo cargado correctamente!");
            } 
            catch (IOException e) 
            {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void SaveTextFile(String titulo, String contenido) {
        JFileChooser jfcGuardar = new JFileChooser();
        jfcGuardar.setDialogTitle("Guardar " + titulo + " como...");
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        jfcGuardar.setFileFilter(filtro);

        if (jfcGuardar.showSaveDialog(this) != JFileChooser.CANCEL_OPTION) {
            File archivo = jfcGuardar.getSelectedFile();
            String ruta = archivo.getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".txt")) {
                ruta += ".txt";
            }

            try (java.io.FileWriter fw = new java.io.FileWriter(ruta)) {
                fw.write(contenido);
                JOptionPane.showMessageDialog(this, titulo + " guardado correctamente.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar " + titulo + ": " + e.getMessage());
            }
        }
    }

    private void SaveTableAsText(JTable tabla, String titulo) {
        JFileChooser jfcGuardar = new JFileChooser();
        jfcGuardar.setDialogTitle("Guardar " + titulo + " como...");
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        jfcGuardar.setFileFilter(filtro);

        if (jfcGuardar.showSaveDialog(this) != JFileChooser.CANCEL_OPTION) {
            File archivo = jfcGuardar.getSelectedFile();
            String ruta = archivo.getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".txt")) {
                ruta += ".txt";
            }

            try (java.io.FileWriter fw = new java.io.FileWriter(ruta)) {
                DefaultTableModel model = (DefaultTableModel) tabla.getModel();

                // Escribir encabezados
                for (int i = 0; i < model.getColumnCount(); i++) {
                    fw.write(String.format("%-25s", model.getColumnName(i)));
                }
                fw.write("\n");

                fw.write("=".repeat(model.getColumnCount() * 25));
                fw.write("\n");

                // Escribir filas
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Object value = model.getValueAt(i, j);
                        fw.write(String.format("%-25s", value != null ? value.toString() : ""));
                    }
                    fw.write("\n");
                }

                JOptionPane.showMessageDialog(this, titulo + " guardada correctamente.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar " + titulo + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAnalisisLexico) {
            // 🔹 Crear y ejecutar el analizador léxico
            AnalizadorLexico analizador = new AnalizadorLexico();
            analizador.analizar(codigoArea.getText());

            // ---------- LIMPIAR ZONAS ----------
            erroresArea.setText("");
            DefaultTableModel modeloLexemas = (DefaultTableModel) tablaLexemas.getModel();
            modeloLexemas.setRowCount(0);

            // ---------- MOSTRAR TOKENS ----------
            for (Token t : analizador.getTokens()) {
                modeloLexemas.addRow(new Object[]{t.getLexema(), t.getTipo()});
            }

            // ---------- MOSTRAR ERRORES ----------
            if (analizador.getErrores().isEmpty()) {
                erroresArea.setText("Análisis léxico completado sin errores.\n");
            } else {
                for (String err : analizador.getErrores()) {
                    erroresArea.append(err + "\n");
                }
            }
        }

        if (e.getActionCommand().equals("Análisis Sintáctico")) {
            //Ejecutar primero el análisis léxico
            AnalizadorLexico analizadorLexico = new AnalizadorLexico();
            analizadorLexico.analizar(codigoArea.getText());

            

            // ---------- LIMPIAR ZONAS ----------
            erroresArea.setText("");
            DefaultTableModel modeloSimbolos = (DefaultTableModel) tablaSimbolos.getModel();
            modeloSimbolos.setRowCount(0);

            // ---------- VALIDAR TOKENS ----------
            if (analizadorLexico.getTokens().isEmpty()) {
                erroresArea.setText("No se encontraron tokens. Asegúrate de ejecutar el análisis léxico correctamente.\n");
                return;
            }

            // ---------- EJECUTAR ANÁLISIS SINTÁCTICO ----------
            AnalizadorSintactico analizadorSintactico = new AnalizadorSintactico();
            analizadorSintactico.analizar(analizadorLexico.getTokens());
            
            for (Simbolo s : analizadorSintactico.getTablaSimbolos()) {
                modeloSimbolos.addRow(new Object[]{
                    s.getNombre(), s.getTipo(), s.getClase(), s.getValor(),
                    s.getVisibilidad(), s.getPosicion(), s.getRol()
                });
            }


            // ---------- MOSTRAR ERRORES ----------
            if (analizadorSintactico.getErrores().isEmpty()) {
                erroresArea.setText("Análisis sintáctico completado sin errores.\n");
            } else {
                for (String err : analizadorSintactico.getErrores()) {
                    erroresArea.append(err + "\n");
                }
            }
        }


        if(e.getSource() == JMIOpen){ OpenFile(); }

        if (e.getSource() == JMISave) { 
            SaveTextFile("Código", codigoArea.getText()); 
        }

        if (e.getSource() == JMISaveError) { 
            SaveTextFile("Errores léxicos", erroresArea.getText()); 
        }

        if (e.getSource() == JMISaveLexemas) { 
            SaveTableAsText(tablaLexemas, "Tabla de Lexemas"); 
        }

        if (e.getSource() == JMISaveSimbolos) { 
            SaveTableAsText(tablaSimbolos, "Tabla de Símbolos"); 
        }
    }
}