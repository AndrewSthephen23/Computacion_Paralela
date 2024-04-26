package pkg03serechil01;

import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

// Cliente que se conecta a un servidor remoto para obtener la hora

public class PruebaSocket {
    public static void main(String[] args) {
        try {
            // Establecer una conexión con el servidor remoto en el puerto 13 (servicio de tiempo)
            Socket s = new Socket("time-A.timefreq.bldrdoc.gov", 13);
            // Ejemplos alternativos para conectar a otros servidores:
            // Socket s = new Socket("132.163.96.1", 13); // IP del servidor de tiempo
            // Socket s = new Socket("127.0.0.1", 8189); // Servidor local en el puerto 8189

            try {
                // Obtener el flujo de entrada del socket para recibir datos del servidor
                InputStream secuenciaDeEntrada = s.getInputStream();
                Scanner in = new Scanner(secuenciaDeEntrada);

                // Leer y mostrar las líneas de respuesta recibidas del servidor
                while (in.hasNextLine()) {
                    String line = in.nextLine();
                    System.out.println(line); // Imprimir la línea recibida
                }
            } finally {
                s.close(); // Cerrar el socket al finalizar la conexión
            }
        } catch (Exception e) {
            e.printStackTrace(); // Manejar cualquier excepción que pueda ocurrir durante la conexión
        }
    }
}
