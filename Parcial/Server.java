import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.Scanner;
/*
Este clase implementa un servidor que utiliza el algoritmo de K-means para
procesar y manipular datos de puntos en un entorno de red
 */
public class Server {//declaramos la clase server
    //declaramos variables estaticas para el puerto y el serversocket
    private static final int port = 2206;
    private static ServerSocket server;
    //declaramos una lista de sockets de los clientes
    static final List<Socket> clients = new ArrayList<>();
    //declaramos variables para medir el tiempo de ejecucion del algoritmo
    static long startTime;
    static long endTime;
    static long totalTime;
    //Definimos ctes y vectores para almacenar puntos, centroides y datos de clusteres
    private static final int numberPoints = 10000;
    private static final int numberCentroids = 100;
    private static final Vector<Point> points = new Vector<Point>();
    private static Vector<Point> centroids = new Vector<Point>();
    private static Vector<Integer> oldCluster = new Vector<Integer>();;
    private static Vector<Integer> cluster = new Vector<Integer>();

    //declaramos la variable 'sendQueue' que es la cola para almacenar los
    // mensajes que seran enviados a los clientes
    private static MessageQueue sendQueue = new MessageQueue();
    //declaramos la variable que se utilizara para almacenar datos recibidos
    // por los clientes especificos en forma de cadena de texto
    static Vector<String> receiveData = new Vector<>();
    //declaramos la variable que se utilizara para almacenar los indices asociados a los
    // datos recibidos de los clientes, los indices identifican la procedencia de los
    // datos y facilitan su ordenamiento.
    static Vector<Integer> receiveIndex = new Vector<>();

    public static void main(String[] args){//ejecuta el servidor
        prepareData();//llama al metodo para generar puntos aleatorios y centroides iniciales
        try {
            server = new ServerSocket(port);//creamos un socket especificando el puerto
            System.out.println("Server is running on port " + server.getLocalPort());//mensaje que muestra que esta en ejecucion
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //GESTION DE CLIENTES:
        //iniciamos un hilo de forma continua
        new Thread(() -> {
            while (true) {
                try {
                    Socket client = server.accept();//acepta conexiones de clientes
                    clients.add(client);//registra nuevos clientes en la lista
                    System.out.println("New client connected from " + client.getInetAddress().getHostAddress());
                    //inicia un nuevo hilo de 'ClienteHandler' para manejar cada conexion de cliente
                    new ClientHandler(client, clients.size()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        //INTERACCION DESDE CONSOLA
        //iniciamos otro hilo para permitir la interaccion desde la consola
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String message = scanner.nextLine();
                //si se introduce "SEND" en la consola, inicia el envio de datos a los clientes
                if(message.equals("SEND")){
                    startTime = System.currentTimeMillis();
                    sendData();
                }
            }
        }).start();
    }
    //declaramos el metodo que se encarga de enviar los datos de puentos y centroides al cliente
    static void sendData() {
        //calcula el tamaño de lote de puentos por cliente
        int size = points.size() / clients.size();
        //calcula el residuo de la division debido a la division no exacta de punto entre clientes
        int offset = points.size() % clients.size();

        for(int i = 0; i < clients.size(); i++){
            //llama al metodo 'getDataToSend' para obtener los datos a enviar a cada cliente
            String message = getDataToSend(i, size, offset);
            message += "\n";
            sendQueue.addMessage(message);//agrega los datos a la cola de mensajes
        }

        for (Socket client : clients) {
            try {
                //envia los datos a cada cliente a traves de su 'OutputStream'
                // extrayendo los mensajes de la cola de mensajes.
                client.getOutputStream().write(sendQueue.getNextMessage().getBytes());
                client.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //declaramos el metodo responsable de construir y formatear un mensaje de datos
    // para ser enviados a un cliente especifico en el servidor.
    private static String getDataToSend(int i, int size, int offset) {
        //calcula el indice inicial de donde se tomaran los puntos para este cliente
        int start = i * size;
        //calcula el indice final hasta donde se tomaran los puntos para este cliente
        int end = start + size;
        //si i es igual al ultimo cliente
        if(i == clients.size() - 1){
            //se añade offset al valor de end para asegurar que todos los puntos
            //se distributan entre los clientes, incluso si hay un residuo.
            end += offset;
        }
        //construimos el mensaje de puntos
        String message = "";
        for(int j = start; j < end; j++){
            //para cada punto se agrega al mensaje
            message += points.get(j);
            //verificamos si no es el ultimo punto
            if(j != end - 1){
                //si no es el ultimo punto se le agrega ';' para separar los puntos
                message += ";";
            }
        }
        //añadimos el separador de centroides al mensaje para separar la seccion de puntos de
        // la seccioon de centroides en el mensaje final
        message += "%";
        //agregamos los centroides al mensaje
        for(int j = 0; j < centroids.size(); j++){//itera a traves de los centroides almacenados
            message += centroids.get(j);//el centroide se agrega al mensaje usando get()
            //verificamos si no es el ultimo centroide
            if(j != centroids.size() - 1){
                //si no lo es se agrega un ';' para separar los centroides
                message += ";";
            }
        }
        //finalmente se retorna el mensaje completo para enviarlo al cliente
        return message;
    }
    //Declaramos el metodo que se encarga de inicializar y preparar los datos necesarios para
    // ejecutar el algoritmo de agrupamiento implementado en el servidor.
    static void prepareData(){
        //GENERACION DE PUNTOS ALEATORIOS
        Random rand = new Random();
        for(int i = 0; i < numberPoints; i++){
            //generamos coordenadas 'x' y 'y' entre 0 y 99
            int x = rand.nextInt(100);
            int y = rand.nextInt(100);
            //creamos un nuevo objeto point con las coordenadas generadas y se agrega a lista
            points.add(new Point(x,y));
        }
        //SELECCION ALEATORIA DE CENTROIDES
        //se crea un vector para mantener un registro de los indices de los puntos que se seleccionaron como centroides
        Vector<Integer> used = new Vector<Integer>();
        //se itera la cantidad de centroides para seleccionar centroides aleatorios
        for(int i = 0; i < numberCentroids; i++){
            int n = rand.nextInt(numberPoints);//se genera un indice aleatorio entre 0 y numberPoints-1
            //se asegura que el indice 'n' no haya sido utilizado antes como un centroide
            // verificando si esta contenido en el vector 'used'.
            while(used.contains(n)){
                n = rand.nextInt(numberPoints);
            }
            used.add(n);//si se encuentra un indice no autorizado, se agrega a used para evitar que se seleccione nuevamente.
            //se obtinen las coordenas del punto en el indice 'n'
            float x = points.get(n).getX();
            float y = points.get(n).getY();
            //se crea un nuevo objeto Point y se agrega a la lista centroids
            centroids.add(new Point(x,y));
        }
        //Se imprime en consola la cantidad de puntos generados y la cantidad de centroides seleccionados
        System.out.println("Number of points: " + numberPoints);
        System.out.println("Number of centroids: " + numberCentroids);
    }
    public static void sortData(){
        int n = receiveIndex.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (receiveIndex.get(j) > receiveIndex.get(j + 1)) {
                    int temp = receiveIndex.get(j);
                    receiveIndex.set(j, receiveIndex.get(j + 1));
                    receiveIndex.set(j + 1, temp);
                    String tempString = receiveData.get(j);
                    receiveData.set(j, receiveData.get(j + 1));
                    receiveData.set(j + 1, tempString);
                }
            }
        }
        parseCluster();
    }
    private static void parseCluster(){
        if(oldCluster.isEmpty()){
            parseClusterData(oldCluster);
        }else{
            parseClusterData(cluster);
            if(oldCluster.equals(cluster)){
                endTime = System.currentTimeMillis();
                totalTime = endTime - startTime;
                System.out.println("Total time: " + totalTime + "ms");
                System.out.println(totalTime + " , " + points.size());
                System.out.println("DONE");
                System.exit(0);
            }else{
                oldCluster.clear();
                oldCluster.addAll(cluster);
                cluster.clear();
            }
        }
        calculateNewCentroids();
    }

    private static void parseClusterData(Vector<Integer> cluster) {
        for(int i = 0; i < clients.size(); i++){
            String data = receiveData.get(i);
            data = data.substring(1, data.length() - 1);
            data = data.replaceAll(" ", "");
            String[] dataString = data.split(",");
            for (String s : dataString) {
                cluster.add(Integer.parseInt(s));
            }
        }
    }

    private static void calculateNewCentroids(){
        float[] sumPointsX = new float[centroids.size()];
        float[] sumPointsY = new float[centroids.size()];
        int[] count = new int[centroids.size()];

        for (int i = 0; i < numberPoints; i++) {
            Point point = points.get(i);
            int cluster = oldCluster.get(i);

            for (int c = 1; c < centroids.size()+1; c++) {
                if (cluster == c) {
                    sumPointsX[c-1] += point.getX();
                    sumPointsY[c-1] += point.getY();
                    count[c-1] += 1;
                }
            }
        }

        for (int c = 0; c < centroids.size(); c++) {
            centroids.get(c).update(sumPointsX[c] / count[c], sumPointsY[c] / count[c]);
        }
        sendNewCentroids();
    }

    private static void sendNewCentroids(){
        receiveIndex.clear();
        receiveData.clear();
        for (Socket client : clients) {
            try {
                StringBuilder message = new StringBuilder();
                for(int i = 0; i < centroids.size(); i++){
                    message.append(centroids.get(i));
                    if(i != centroids.size() - 1){
                        message.append(";");
                    }
                }
                message.append("\n");
                client.getOutputStream().write(message.toString().getBytes());
                client.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

class ClientHandler extends Thread{
    private final Socket client;
    private InputStream entry;
    private int index;
    public ClientHandler(Socket client, int index){
        this.client = client;
        this.index = index;
    }
    public void run(){
        try {
            entry = client.getInputStream();
            Scanner scanner = new Scanner(entry);
            while (true) {
                if (scanner.hasNextLine()) {
                    System.out.println("Received data from client " + index);
                    String message = scanner.nextLine();
                    synchronized (Server.receiveIndex){
                        Server.receiveIndex.add(index);
                    }
                    synchronized (Server.receiveData){
                        Server.receiveData.add(message);
                    }
                    if (Server.receiveIndex.size() == Server.clients.size()) {
                        Server.sortData();
                    }
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}