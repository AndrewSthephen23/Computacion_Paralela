package pkg03serechil01;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServidorDeEco {
    public static void main(String[] args) {
        try {
            // Crear un servidor de socket en el puerto 8189
            ServerSocket s = new ServerSocket(8189);

            // Esperar y aceptar una conexión entrante
            Socket entrante = s.accept();

            try {
                // Obtener secuencias de entrada y salida del socket
                InputStream secuenciaDeEntrada = entrante.getInputStream();
                OutputStream secuenciaDeSalida = entrante.getOutputStream();

                // Crear objetos Scanner y PrintWriter para leer y escribir datos
                Scanner in = new Scanner(secuenciaDeEntrada);
                PrintWriter out = new PrintWriter(secuenciaDeSalida, true);

                // Enviar un mensaje de bienvenida al cliente
                out.println("¡Hola! Escriba ADIOS para salir.");

                boolean terminado = false;
                Scanner sc = new Scanner(System.in);

                // Leer y procesar los datos recibidos del cliente y desde la consola
                while (!terminado && (in.hasNextLine() || sc.hasNextLine())) {
                    if (in.hasNextLine()) {
                        // Leer la línea enviada por el cliente
                        String linea = in.nextLine();
                        // Enviar un eco de vuelta al cliente
                        out.println("Eco:" + linea);
                        // Verificar si el cliente quiere terminar la conexión
                        if (linea.trim().equals("ADIOS"))
                            terminado = true;
                    }
                    if (sc.hasNextLine()) {
                        // Leer la línea ingresada desde la consola del servidor
                        String lineaout = sc.nextLine();
                        System.out.println("li:" + lineaout); // Imprimir la línea en la consola del servidor
                        // Enviar un eco de vuelta al cliente
                        out.println("Eco:" + lineaout);
                    }
                }
            } finally {
                // Cerrar el socket entrante al finalizar la conexión
                entrante.close();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Manejar cualquier excepción que pueda ocurrir durante la conexión
        }
    }
}

//panel programas /activar o desactivar las caracteristicas de windows /cliente telnet /aceptar
//telnet 127.0.0.1 8189