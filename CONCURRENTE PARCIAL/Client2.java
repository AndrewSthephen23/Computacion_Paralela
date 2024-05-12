import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

public class Client2 {
    private static final int port = 2206;
    private static final String host = "localhost";
    private static Socket client;

    private static final Vector<Persona> personas = new Vector<>();
    private static final MessageQueue resultQueue = new MessageQueue();

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
                                    String[] data = message.split(";");
                                    bDataReceived = true;
                                    parseData(data);           
                            }
                        }else{
                            if(scanner.hasNextLine()){
                                String message = scanner.nextLine();
                                String[] data = message.split(";");
                                parseData(data);                               
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
    
    private static void parseData(String[] data) {
        try{

            for (String datum : data) {
                String[] attributes = datum.split(",");
                int id = Integer.parseInt(attributes[0].substring(1));
                int edad = Integer.parseInt(attributes[1]);
                int sexo = Integer.parseInt(attributes[2]);
                int peso = Integer.parseInt(attributes[3]);
                int altura = Integer.parseInt(attributes[4].substring(0, attributes[4].length() - 1));
                //System.out.println(id+""+edad+""+sexo+""+peso+""+altura);
                personas.add(new Persona(id, edad, sexo, peso, altura));
            }

        } catch (Exception e){
            System.out.println(e);
        }
        System.out.println("PROCESS: ");
        processData();
    }

    private static void processData() {
        try {
            System.out.println("Processing data...");
            DecisionTree tree = new DecisionTree();
            for (Persona persona : personas) {
                int sexo = persona.getSexo();
                int edad = persona.getEdad();
                int peso = persona.getPeso();
                int altura = persona.getAltura();
                int mortality = tree.predict(sexo, edad, peso, altura);
                System.out.println("mortalidad: "+mortality);
                resultQueue.addMessage(String.valueOf(mortality));
            }
            System.out.println("Data processed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendResults();
    }

    private static void sendResults() {
        try {
            System.out.println("Sending results...");
            
            OutputStream outputStream = client.getOutputStream();
            while (!resultQueue.isEmpty()) {
                //System.out.println(resultQueue.getNextMessage());
                String result = resultQueue.getNextMessage() + "\n";
                outputStream.write(result.getBytes());
                outputStream.flush();
            }
            System.out.println("Results sent successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
