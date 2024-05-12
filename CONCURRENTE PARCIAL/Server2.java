import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server2 {
    static long startTime;
    static long endTime;
    static long totalTime;
    private static Vector<Integer> oldCluster = new Vector<Integer>();;
    private static Vector<Integer> cluster = new Vector<Integer>();
    private static final int port = 2206;
    private static ServerSocket server;
    static final List<Socket> clients = new ArrayList<>();

    private static final int numberPersons = 300;
    private static final Vector<Persona> personas = new Vector<>();
    private static MessageQueue sendQueue = new MessageQueue();
    static Vector<String> receiveData = new Vector<>();
    static Vector<Integer> receiveIndex = new Vector<>();

    public static void main(String[] args) {
        prepareData();
        try {
            server = new ServerSocket(port);
            System.out.println("Server is running on port " + server.getLocalPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            while (true) {
                try {
                    Socket client = server.accept();
                    clients.add(client);
                    System.out.println("New client connected from " + client.getInetAddress().getHostAddress());
                    new ClientHandler2(client, clients.size()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String message = scanner.nextLine();
                if (message.equals("SEND")) {
                    sendData();
                }
            }
        }).start();
    }

    //ENVIO
    static void sendData() {
        System.out.println("Sending data...");
        int size = personas.size() / clients.size();
        int offset = personas.size() % clients.size();
        for (int i = 0; i < clients.size(); i++) {
            String message = getDataToSend(i, size, offset);
            message += "\n";
            sendQueue.addMessage(message);
        }
        for (Socket client : clients) {
            try {
                client.getOutputStream().write(sendQueue.getNextMessage().getBytes());
                client.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Data sent successfully.");
    }

    // TO STRING PREPARACION
    private static String getDataToSend(int i, int size, int offset) {
        int start = i * size;
        int end = start + size;
        if (i == clients.size() - 1) {
            end += offset;
        }
        StringBuilder message = new StringBuilder();
        for (int j = start; j < end; j++) {
            Persona persona = personas.get(j);
            message.append(persona.toString());
            if (j != end - 1) {
                message.append(";");
            }
        }
        return message.toString();
    }

    //GENERACION DE PERSONAS
    static void prepareData() {
        System.out.println("Generating data...");
        Random rand = new Random();
        for (int i = 0; i < numberPersons; i++) {
            int edad = rand.nextInt(51)+20;
            int sexo = rand.nextInt(2); // 0 para hombre, 1 para mujer
            int peso = rand.nextInt(150) + 50; // Entre 50 y 200 kg
            int altura = rand.nextInt(40) + 140; // Entre 100 y 200 cm
            personas.add(new Persona(i, edad, sexo, peso, altura));
        }
        //System.out.println("Data generated: " + personas);
    }

    // Ordenar los datos una vez que llegan todos los mensajes
    public static void sortData() {
        System.out.println("Sorting data...");
        int n = receiveIndex.size();
        //System.out.println("Tamaño de recibo: "+ n);
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

    // Comparar resultados anteriores
    private static void parseCluster(){
        System.out.println("Parsing cluster data...");
        if(oldCluster.isEmpty()){
            //System.out.println("empty");
            parseClusterData(oldCluster);
        }else{
            //System.out.println("Not empty");
            parseClusterData(cluster);
            if(oldCluster.equals(cluster)){
                endTime = System.currentTimeMillis();
                totalTime = endTime - startTime;
                System.out.println("Total time: " + totalTime + "ms");
                System.out.println(totalTime + " , " + personas.size());
                System.out.println("DONE");
                System.exit(0);
            }else{
                oldCluster.clear();
                oldCluster.addAll(cluster);
                cluster.clear();
            }
        }
        sendNewInfo();
    }

    // Parsear los datos del cluster
    private static void parseClusterData(Vector<Integer> cluster) {
        for(int i = 0; i < clients.size(); i++){
            String data = receiveData.get(i);
            //System.out.println(data);
            //data = data.substring(1, data.length() - 1);
            //data = data.replaceAll(" ", "");
            //String[] dataString = data.split(",");
            cluster.add(Integer.parseInt(data));
            //for (String s : dataString) {
            //}
        }
    }

    // Enviar nueva información a los clientes
    private static void sendNewInfo(){
        System.out.println("Sending new information...");
        receiveIndex.clear();
        receiveData.clear();
        for (Socket client : clients) {
            try {
                StringBuilder message = new StringBuilder();
                for(int i = 0; i < personas.size(); i++){
                    message.append(personas.get(i));
                    if(i != personas.size() - 1){
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
        System.out.println("New information sent successfully.");
    }
}

class ClientHandler2 extends Thread {
    private final Socket client;
    private InputStream entry;
    private int index;

    public ClientHandler2(Socket client, int index) {
        this.client = client;
        this.index = index;
    }

    public void run() {
        try {
            entry = client.getInputStream();
            Scanner scanner = new Scanner(entry);
            while (true) {
                if (scanner.hasNextLine()) {
                    String message = scanner.nextLine();
                    synchronized (Server2.receiveIndex) {
                        Server2.receiveIndex.add(index);
                    }
                    synchronized (Server2.receiveData) {
                        Server2.receiveData.add(message);
                    }
                    if (Server2.receiveIndex.size() == Server2.clients.size()) {
                        Server2.sortData();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
