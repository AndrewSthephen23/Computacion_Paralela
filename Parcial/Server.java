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
    /*
    Declaramos el metodo para implementar el algoritmo del metodo burbuja para ordenar
    2 vectores (receiveIndex y receiveData) simultáneamente según los valores en receiveIndex
     */
    public static void sortData(){
        int n = receiveIndex.size();//n longitud del vector 'receiveIndex'
        //itera solbre elementos del vector
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                //si el elemento actual es mayor que el siguiente
                if (receiveIndex.get(j) > receiveIndex.get(j + 1)) {
                    //se realiza un intercambio entre los elementos en receiveIndex y receiveData
                    int temp = receiveIndex.get(j);
                    receiveIndex.set(j, receiveIndex.get(j + 1));
                    receiveIndex.set(j + 1, temp);
                    String tempString = receiveData.get(j);
                    receiveData.set(j, receiveData.get(j + 1));
                    receiveData.set(j + 1, tempString);
                    //despues del intercambio los elementos estan ordenados en orden ascendente
                }
            }
        }
        parseCluster();//los elementos estan listos para el siguiente paso
    }
    /*
    Declaramos el metodo que se encarga de procesar los datos de los clusteres despues de que
    se hayan sido ordenados y almacenados en los vectores oldCluster y cluster.
     */
    private static void parseCluster(){
        //se verifica si el vector oldCluster esta vacio
        if(oldCluster.isEmpty()){
            //si esta vacio se llama al metodo para analizar los datos de los cluteres y almacenarlos en oldCluster
            parseClusterData(oldCluster);
        }else{//si no esta vacio se procesan los datos de los cluteres anteriores almacenados en cluster
            parseClusterData(cluster);
            //si oldCluster es igual a cluster significa que no hubo cambios en la asignacion de los puntos
            // a los clusteres desde la ultima iteracion del algoritmo
            if(oldCluster.equals(cluster)){
                //se calcula el tiempo de ejecucion del algoritmo
                endTime = System.currentTimeMillis();
                totalTime = endTime - startTime;
                //se imprime en la consola junto como el # de puntos
                System.out.println("Total time: " + totalTime + "ms");
                System.out.println(totalTime + " , " + points.size());
                System.out.println("DONE");
                //se finaliza el programa
                System.exit(0);
            }else{//si oldCluster no es igual a cluster significa que hubo cambios en la asignacion de los
                // puntos a los clusteres
                //se actualiza oldCluster con los datos del cluster
                oldCluster.clear();
                oldCluster.addAll(cluster);
                cluster.clear();//se limpia para almacenar nuevos datos
            }
        }
        //se continua con el calculo de nuevos centroides
        calculateNewCentroids();
    }
    /*
    Declaramos el metodo que se encarga de procesar los datos de los clusteres
    recibidos desde multiples clientes y almacenados en el vector 'receiveData'
     */
    private static void parseClusterData(Vector<Integer> cluster) {
        //se itera sobre los clientes almacenados en clients
        for(int i = 0; i < clients.size(); i++){
            //para cada cliente se obtine la cadena de datos de clusteres correspondientes desde receiveData
            String data = receiveData.get(i);
            //se realiza un procesamiento de la cadena de datos para eliminar los '[]' que rodean a la cadena,
            //eliminar los espacios en blanco y dividir la cadena utilizando ';'como delimitador
            data = data.substring(1, data.length() - 1);
            data = data.replaceAll(" ", "");
            //la cadena se divide en un array de cadenas 'dataString' donde cada elemento representa
            //un valor de un punto asignado a un cluster.
            String[] dataString = data.split(",");
            // se itera sobre cada elemento
            for (String s : dataString) {
                //y se convierte en un entero, el entero resultante se agrega al vector 'cluster'
                // que almacena la asignacion de puntos a clusteres
                cluster.add(Integer.parseInt(s));
            }
        }
    }
    /*
    Declaramos el metodo que se encarga de calcular los nuevos centroides para los clusteres basados
    en la asignacion actualizada de los puntos a los clusteres y luego se envia estos nuevos a los
    clientes para su procesamiento.
     */
    private static void calculateNewCentroids(){
        //se inicializan 3 arreglos
        float[] sumPointsX = new float[centroids.size()];//arreglo para almacenar la suma de coordenas x de los puntos asignados a cada centroide
        float[] sumPointsY = new float[centroids.size()];//arreglo para almacenar la suma de coordenas y de los puntos asignados a cada centroide
        int[] count = new int[centroids.size()];//arreglo para almacenar el numero de puntos asignados a cada centroide
        //se itera sobre todos los puntos almacenados en 'points'
        for (int i = 0; i < numberPoints; i++) {
            //para cada punto se obtiene su asignacion de cluster
            Point point = points.get(i);
            //desde 'oldCluster'
            int cluster = oldCluster.get(i);
            //iteramos sobre todos los centroides
            for (int c = 1; c < centroids.size()+1; c++) {
                //si la asignacion de cluster del punto coindice con el indice del centroide actual
                if (cluster == c) {
                    //se actualizan las sumas de coordenadas 'x'e'y' en
                    sumPointsX[c-1] += point.getX();
                    sumPointsY[c-1] += point.getY();
                    //se incrementa el contador de puntos asignados a ese centroide.
                    count[c-1] += 1;
                }
            }
        }
        //iteramos sobre los centroides
        for (int c = 0; c < centroids.size(); c++) {
            //calculamos las nuevas coordenas 'x','y' para cada centroide dividiendo las sumas
            //acumuladas por el numero de puntos asignados a cada centroide
            centroids.get(c).update(sumPointsX[c] / count[c], sumPointsY[c] / count[c]);
        }
        //final mente se llama al metodo para enviar los nuevos centroides a los clientes conectados.
        sendNewCentroids();
    }
    /*
    Declaramos el metodo que se encarga de enviar los nuevos centroides calculados a todos los
    clientes conectados
     */
    private static void sendNewCentroids(){
        //limpiamos las listas de recepcion
        receiveIndex.clear();
        receiveData.clear();
        //se itera sobre cada cliente conectado al servidor
        for (Socket client : clients) {
            try {
                //para cada cliente se construye un mensaje que contiene los datos de los nuevos centroides
                StringBuilder message = new StringBuilder();
                //se itera el vector 'centroids'
                for(int i = 0; i < centroids.size(); i++){
                    //se agrega cada centroide al mensaje
                    message.append(centroids.get(i));
                    if(i != centroids.size() - 1){
                        //separamos los centroides con ';'
                        message.append(";");
                    }
                }
                //se agrega un salto de linea al final del mensaje para indicar el final de los datos
                message.append("\n");
                //se obtiene el flujo de salida del cliente y se escribe el mensaje convertido a bytes
                client.getOutputStream().write(message.toString().getBytes());
                //envia inmediatamente los mensajes
                client.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
/*
Definimos la clase que extiende 'Thread' la cual se utiliza para manejar la comunicacion
con un cliente especifico conectado al servidor
 */
class ClientHandler extends Thread{
    //declaramos variables
    private final Socket client;//socket del cliente con el que se estable la conexion
    private InputStream entry;//flujo de entrada desde el cliente
    private int index;//inidice del cliente dentro de la lista de clientes en el servidor
    //contructor que inicializa el 'ClientHandler' con el socket del cliente y su indice
    public ClientHandler(Socket client, int index){
        this.client = client;
        this.index = index;
    }
    //declaramos el siguiente metodo que es invocado cuando se inicia un hilo
    public void run(){
        try {
            entry = client.getInputStream();//se obtiene el flujo de entrada del socket del cliente
            Scanner scanner = new Scanner(entry);//Scanner para leer datos del flujo de entrada
            while (true) {
                //Verifica si hay una linea de datos disponible para ser leida del cliente
                if (scanner.hasNextLine()) {
                    System.out.println("Received data from client " + index);
                    //si hay datos disponibles se lee la linea de datos, que representa el mensaje enviado por el cliente
                    String message = scanner.nextLine();
                    //los datos recibidos message y el indice del cliente 'index' se agregan
                    //de forma sincronizada a las listas para asegurar que los recuros compartidos se
                    //manejes de manera segura entre varios hilos.
                    synchronized (Server.receiveIndex){
                        Server.receiveIndex.add(index);
                    }
                    synchronized (Server.receiveData){
                        Server.receiveData.add(message);
                    }
                    //verifica si se han recibido datos de todos los clientes
                    if (Server.receiveIndex.size() == Server.clients.size()) {
                        //si es asi llama al metodo para procesar los datos recibidos
                        Server.sortData();
                    }
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}