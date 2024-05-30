package org.example;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public class Rabbit {
    private final static String QUEUE_NAME = "go-java-queue";
    private ServerJava serverJava;
    public void setServerJava(ServerJava serverJava) {
        this.serverJava = serverJava;
    }
    public void run() throws IOException, TimeoutException {
        System.out.println("Esperando mensajes desde el canal '" + QUEUE_NAME + "'");
        initChannel(QUEUE_NAME);
    }

    void initChannel(String queueName) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setVirtualHost("venta_host");
        factory.setUsername("chan");
        factory.setPassword("chan");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws UnsupportedEncodingException {
                String message = new String(body, "UTF-8");
                System.out.println("Mensaje recibido desde el canal '" + QUEUE_NAME + "': '" + message + "'");
                try {
                    serverJava.parseData(message);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}


