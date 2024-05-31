package org.example;
// Importamos las clases necesarias de la biblioteca RabbitMQ
import com.rabbitmq.client.*;

import java.io.IOException;// Para menejar las excepciones de entrada/salida
import java.io.UnsupportedEncodingException;// Para manejar las excepciones de codificación de caracteres
import java.sql.SQLException; // Para manejar las excepciones de SQL
import java.util.concurrent.TimeoutException;// Para manejar las excepciones de tiempo de espera

// Declaramos la clase Rabbit
public class Rabbit {
    // Declaramos el nombre de la cola
    private final static String QUEUE_NAME = "go-java-queue";
    // Declaramos la variable para almacenar una referencia a ServerJava
    private ServerJava serverJava;
    
    // Metodo para establecer una referencia a ServerJava
    public void setServerJava(ServerJava serverJava) {
        this.serverJava = serverJava;
    }

    // Metodo para iniciar la escucha de mensajes en la cola
    public void run() throws IOException, TimeoutException {
        System.out.println("Esperando mensajes desde el canal '" + QUEUE_NAME + "'");
        // Inicializa el canal para escuchar en la cola especificada
        initChannel(QUEUE_NAME);
    }

    // Metodo para inicializar el canal
    void initChannel(String queueName) throws IOException, TimeoutException {
        // Crea una instancia de la clase ConnectionFactory para configurar la conexión
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Establece la IP del servidor RabbitMQ
        factory.setPort(5672);// Establece el puerto del servidor RabbitMQ
        factory.setVirtualHost("venta_host");// Establece el nombre del host virtual de RabbitMQ
        factory.setUsername("chan");// Establece el nombre de usuario de RabbitMQ
        factory.setPassword("chan"); // Establece la contraseña de RabbitMQ

        // Crea una nueva conexion y un canal para la comunicacion
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Declara la cola para asegurar de que exista
        channel.queueDeclare(queueName, false, false, false, null);

        // Crea un consumidor para manejar la recepcion de mensajes
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws UnsupportedEncodingException {
                // Convierte el cuerpo del mensaje a una cadena utilizando la codificación UTF-8
                String message = new String(body, "UTF-8");
                // Imprime el mensaje recibido
                System.out.println("Mensaje recibido desde el canal '" + QUEUE_NAME + "': '" + message + "'");
                try {
                    // LLama al metodo parseData de serverJava para procesar el mensaje recibido
                    serverJava.parseData(message);
                } catch (SQLException e) {
                    // Si occurre una excepción de SQL, lanza una RuntimeException
                    throw new RuntimeException(e);
                }
            }
        };
        // Inicia el consumo de mensajes en la cola con auto-acknowledge activado
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}


