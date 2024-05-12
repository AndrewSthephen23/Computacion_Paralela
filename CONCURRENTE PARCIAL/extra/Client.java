import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

public class Client {
    private static final int port = 2205;
    private static final String host = "localhost";
    private static Socket client;

    private static final Vector<Point> points = new Vector<Point>();
    private static Vector<Point> centroids = new Vector<Point>();
    private static Vector<Integer> cluster = new Vector<Integer>();

    public static void main(String[] args){
        try {
            client = new Socket(host, port);
            System.out.println("Client has connected to server on port " + client.getPort());
            Thread receiveDataThread = new Thread(() -> {
                try (Scanner scanner = new Scanner(client.getInputStream())) {
                    boolean bDataReceived = false;
                    while (true) {
                        if (!bDataReceived) {
                            if (scanner.hasNextLine()) {
                                String message = scanner.nextLine();
                                System.out.println(message);
                                String[] data = message.split("%");
                                String[] pointsString = data[0].split(";");
                                String[] centroidsString = data[1].split(";");
                                parseData(pointsString, points);
                                parseData(centroidsString, centroids);
                                bDataReceived = true;
                                calculateKMeans();
                            }
                        }else{
                            if(scanner.hasNextLine()){
                                String message = scanner.nextLine();
                                String[] centroidsString = message.split(";");
                                parseData(centroidsString, centroids);
                                calculateKMeans();
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            receiveDataThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseData(String[] pointsString, Vector<Point> points) {
        points.clear();
        for (String pointString : pointsString) {
            String[] pointData = pointString.split(",");
            pointData[0] = pointData[0].substring(1);
            pointData[1] = pointData[1].substring(0, pointData[1].length() - 1);
            points.add(new Point(Float.parseFloat(pointData[0]), Float.parseFloat(pointData[1])));
        }
    }

    private static float calculateDistance(Point point, Point centroid) {
        return (float) Math.sqrt(Math.pow(point.getX() - centroid.getX(), 2) + Math.pow(point.getY() - centroid.getY(), 2));
    }

    private static void calculateKMeans() throws IOException {
        for (Point point : points) {
            float minDistance = Float.MAX_VALUE;
            int index = 0;
            for (int i = 0; i < centroids.size(); i++) {
                float distance = calculateDistance(point, centroids.get(i));
                if (distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
            cluster.add(index+1);
        }
        client.getOutputStream().write((cluster.toString() + "\n").getBytes());
        client.getOutputStream().flush();
        cluster.clear();
        System.out.println("Data sent to server");
    }
}
