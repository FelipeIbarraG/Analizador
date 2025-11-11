package Util;

public class Simbolo {
    private String nombre;
    private String tipo;
    private String clase;
    private String valor;
    private String visibilidad;
    private String posicion;
    private String rol;

    public Simbolo(String nombre, String tipo, String clase, String valor,
                   String visibilidad, String posicion, String rol) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.clase = clase;
        this.valor = valor;
        this.visibilidad = visibilidad;
        this.posicion = posicion;
        this.rol = rol;
    }

    // Getters
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getClase() { return clase; }
    public String getValor() { return valor; }
    public String getVisibilidad() { return visibilidad; }
    public String getPosicion() { return posicion; }
    public String getRol() { return rol; }
}

