package pkg03serechil01;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ManejadorDeConHilos implements Runnable {
    private Socket entrante; // Socket que representa la conexión entrante
    private int contador; // Contador para identificar cada conexión

    // Constructor de la clase
    public ManejadorDeConHilos(Socket i, int c) {
        entrante = i;
        contador = c;
    }

    // Método run() que contiene la lógica de ejecución del hilo
    public void run() {
        try {
            try {
                // Obtener secuencias de entrada y salida del socket
                InputStream secuenciaDeEntrada = entrante.getInputStream();
                OutputStream secuenciaDeSalida = entrante.getOutputStream();

                // Crear objetos Scanner y PrintWriter para leer y escribir datos
                Scanner in = new Scanner(secuenciaDeEntrada);
                PrintWriter out = new PrintWriter(secuenciaDeSalida, true);

                // Enviar un mensaje de bienvenida al cliente
                out.println("¡Hola! Escriba ADIOS para salir");

                boolean terminado = false;
                // Leer los datos entrantes y responder con un eco
                while (!terminado && in.hasNextLine()) {
                    String linea = in.nextLine();
                    out.println("Eco" + linea); // Enviar eco al cliente
                    System.out.println("Eco de:" + contador + " dice:" + linea); // Imprimir eco en el servidor
                    if (linea.trim().equals("ADIOS")) {
                        terminado = true;
                    }
                }
            } finally {
                // Cerrar el socket cuando la conexión termina
                entrante.close();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Manejo de excepciones de E/S
        }
    }
}
