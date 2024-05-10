/*
Importamos las clases necesarias para manejar entrada/salida,
conexiones de socket, escaneo de datos y uso de vectores(colecciones dinamicas)
 */
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

//Declaramos la clase cliente
public class Client {
    private static final int port = 2206;//Declaramos el puerto
    private static final String host = "localhost";//El nombre del host para la conexion al servidor
    private static Socket client;//Declaramos cliente de tipo socket para manejar la conexion cliente-servidor
    //Declaramos vectores para almacenar puntos(Point),centroides y asignanciones de clusteres
    private static final Vector<Point> points = new Vector<Point>();
    private static Vector<Point> centroids = new Vector<Point>();
    private static Vector<Integer> cluster = new Vector<Integer>();

    public static void main(String[] args){
        try {
            client = new Socket(host, port);//Intenta establecer una conexion "Socket" con el servidor especificando(host,puerto)
            System.out.println("Client has connected to server on port " + client.getPort());//muestra un mensaje que se realizo la conexion
            //creamos un hilo que se encargara de recibir data del server de forma asincrona
            Thread receiveDataThread = new Thread(() -> {
                //Utilizamos escaner para leer el flujo de entrada del Socket del cliente
                try (Scanner scanner = new Scanner(client.getInputStream())) {
                    boolean bDataReceived = false;//variable que comprueba si se recibio data
                    while (true) { //Se ejecuta continuamente para esperar y procesar el mensaje del servidor
                        if (!bDataReceived) { //si no se ha recibido ningun dato
                            if (scanner.hasNextLine()) {//verifica si hay otra linea en el scanner
                                String message = scanner.nextLine();//lee una linea de scanner
                                String[] data = message.split("%");//divide el mensaje en datos separados por '%'
                                //Los datos de puntos pointString y centroidsString se separan utilizando ';'.
                                String[] pointsString = data[0].split(";");
                                String[] centroidsString = data[1].split(";");
                                //parseData va a convertir y almacenar los puntos y centroides
                                //en los vectores correspondientes.
                                parseData(pointsString, points);
                                parseData(centroidsString, centroids);

                                bDataReceived = true;//marcamos que se han recibido datos
                                calculateKMeans();//iniciamos el calculo del algoritmo K-means
                            }
                        }else{//si se han recibido datos
                            //Se procesara el mensaje del scanner
                            if(scanner.hasNextLine()){//verifica si hay otra linea en el scanner
                                String message = scanner.nextLine();//lee una linea del scanner
                                //los datos del centroide se separan con ;.
                                String[] centroidsString = message.split(";");
                                parseData(centroidsString, centroids);//los centroides se almacenan en vectores
                                calculateKMeans();//inicia el calculo del algoritmo K-means.
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            receiveDataThread.start();//inicia el hilo para recibir y procesar datos
        } catch (IOException e) { //captura si hay excepsion en e/s
            throw new RuntimeException(e);//la reenvia como una excepcion en tiempo de ejecucion
        }
    }
    //definimos el metodo parseData que toma una cadena y lo combierte a vector
    private static void parseData(String[] pointsString, Vector<Point> points) {
        points.clear();//limpiar el vector de puntos
        for (String pointString : pointsString) {//iteramos sobre cada cadena dentro del arreglo
            String[] pointData = pointString.split(",");//dividimos cada cadena utilizando ',' para obtener las coordenadas x e y.
            //eliminamos los parentesis alrededor de las coordenadas.
            pointData[0] = pointData[0].substring(1);
            pointData[1] = pointData[1].substring(0, pointData[1].length() - 1);
            //comvertimos las coordenas en valores float y creamos un
            //nuevo objeto Point que se agrega al vertor 'points'
            points.add(new Point(Float.parseFloat(pointData[0]), Float.parseFloat(pointData[1])));
        }
    }
    //Definimos el metodo calculateDistance que calcula la distancia euclideana entre un punto y un centroide
    private static float calculateDistance(Point point, Point centroid) {
        return (float) Math.sqrt(Math.pow(point.getX() - centroid.getX(), 2) + Math.pow(point.getY() - centroid.getY(), 2));
    }
    //Definimos el metodo calculateKMeans que implementa el algoritmo K-Means
    private static void calculateKMeans() throws IOException {
        for (Point point : points) {//itera sobre cada punto del vector points
            float minDistance = Float.MAX_VALUE;
            int index = 0;
            //calcula la minima distancia entre el punto
            // y cada uno de los centroides en el vector centroids
            for (int i = 0; i < centroids.size(); i++) {
                float distance = calculateDistance(point, centroids.get(i));
                if (distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
            //asigna el punto al cluster correspondiente
            // representado por el indice del centroide mas cercano
            cluster.add(index+1);
        }
        //convertimos el vector de cluster a una cadena de texto agregando '\n' para
        // luego convertirlo a una matriz de bytes para enviar los datos a traves del OutputStream
        // Para eso se escribe los bytes en el 'OutputStream' del socket, enviandolos al servidor
        client.getOutputStream().write((cluster.toString() + "\n").getBytes());
        //limpiamos el bufer del Output para asegurar que los datos se envien de inmediato
        // y no se queden en el bufer del sistema
        client.getOutputStream().flush();
        cluster.clear();//eliminamos los elementos del cluster
        System.out.println("Data sent to server");//imprimos que los datos se enviaron
    }
}
