package pkg03serechil01;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorDeEcoConHilos {
    public static void main(String[] args) {
        try {
            int i = 1;
            ServerSocket s = new ServerSocket(8189);
            while (true) {
                // Esperar y aceptar una conexión entrante
                Socket entrante = s.accept();
                System.out.println("Engendrado " + i);

                // Crear un objeto ManejadorDeConHilos para manejar la conexión entrante
                Runnable r = new ManejadorDeConHilos(entrante, i);

                // Crear un nuevo hilo para manejar la conexión
                Thread t = new Thread(r);

                // Iniciar el hilo para manejar la conexión
                t.start();

                // Incrementar el contador de conexiones
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace(); // Manejar cualquier excepción que pueda ocurrir durante la ejecución del servidor
        }
    }
}
//telnet 127.0.0.1 8189