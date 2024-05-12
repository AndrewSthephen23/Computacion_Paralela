import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.Scanner;

public class Server {
    
    private static final int port = 2205;
    private static ServerSocket server;
    static final List<Socket> clients = new ArrayList<>();
    static long startTime;
    static long endTime;
    static long totalTime;

    private static final int numberPoints = 10000;
    private static final int numberCentroids = 100;
    private static final Vector<Point> points = new Vector<Point>();
    private static Vector<Point> centroids = new Vector<Point>();
    private static Vector<Integer> oldCluster = new Vector<Integer>();;
    private static Vector<Integer> cluster = new Vector<Integer>();

    private static MessageQueue sendQueue = new MessageQueue();
    static Vector<String> receiveData = new Vector<>();
    static Vector<Integer> receiveIndex = new Vector<>();

    public static void main(String[] args){
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
                    new ClientHandler(client, clients.size()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String message = scanner.nextLine();
                if(message.equals("SEND")){
                    startTime = System.currentTimeMillis();
                    sendData();
                }
            }
        }).start();
    }

    static void sendData() {
        int size = points.size() / clients.size();
        int offset = points.size() % clients.size();
        for(int i = 0; i < clients.size(); i++){
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
    }

    private static String getDataToSend(int i, int size, int offset) {
        int start = i * size;
        int end = start + size;
        if(i == clients.size() - 1){
            end += offset;
        }
        String message = "";
        for(int j = start; j < end; j++){
            message += points.get(j);
            if(j != end - 1){
                message += ";";
            }
        }
        message += "%";
        for(int j = 0; j < centroids.size(); j++){
            message += centroids.get(j);
            if(j != centroids.size() - 1){
                message += ";";
            }
        }
        return message;
    }

    static void prepareData(){
        Random rand = new Random();
        for(int i = 0; i < numberPoints; i++){
            int x = rand.nextInt(100);
            int y = rand.nextInt(100);
            points.add(new Point(x,y));
        }
        Vector<Integer> used = new Vector<Integer>();
        for(int i = 0; i < numberCentroids; i++){
            int n = rand.nextInt(numberPoints);
            while(used.contains(n)){
                n = rand.nextInt(numberPoints);
            }
            used.add(n);
            float x = points.get(n).getX();
            float y = points.get(n).getY();
            centroids.add(new Point(x,y));
        }
       // System.out.println("Number of points: " + numberPoints);
        //System.out.println("Number of centroids: " + numberCentroids);
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