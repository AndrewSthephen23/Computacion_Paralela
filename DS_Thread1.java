package Serie;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;  
import javax.swing.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

// VERSION SERIAL
public class DS_Thread1 { 

private static JFrame WDW1 = new JFrame("Program 1");
private static JFrame WDW2 = new JFrame("Program 2");
private static JFrame WDW3 = new JFrame("Program 3");
private static JFrame WDW4 = new JFrame("Program 4");
private static JScrollPane SP1;
private static JScrollPane SP2;
private static JScrollPane SP3;
private static JScrollPane SP4;
private static JTextArea   TA1  = new JTextArea("");  
private static JTextArea   TA2  = new JTextArea("");  
private static JTextArea   TA3  = new JTextArea("");  
private static JTextArea   TA4  = new JTextArea("");  
private static Font fntLABEL = new Font("Arial",Font.BOLD,24);
private static Font fntTEXT  = new Font("Lucida Console",Font.BOLD,18);
private static JLabel LBL1Start  = new javax.swing.JLabel();
private static JLabel LBL1Finish = new javax.swing.JLabel();
private static JLabel LBL2Start  = new javax.swing.JLabel();
private static JLabel LBL2Finish = new javax.swing.JLabel();
private static JLabel LBL3Start  = new javax.swing.JLabel();
private static JLabel LBL3Finish = new javax.swing.JLabel();
private static JLabel LBL4Start  = new javax.swing.JLabel();
private static JLabel LBL4Finish = new javax.swing.JLabel();
private static final int N = 150000;
private static int[] V1 = new int[N];
private static int[] V2 = new int[N];
private static int[] V3 = new int[N];
private static int[] V4 = new int[N];


private static int[] ESTADO = new int[4 + 1];


    //==============================================================================
    public static void ConfigurarControles(JFrame WDW, 
                                           int WW,
                                           int HH,
                                           int LEFT,
                                           int TOP,
                                           JScrollPane SP,
                                           JTextArea   TA,
                                           JLabel      LBLStart,
                                           JLabel      LBLFinish
                                           )  {

        WDW.setSize(WW, HH);
        WDW.setLocation(LEFT,TOP);
        WDW.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        WDW.setVisible(true);

        LBLStart.setBounds(25, 20, 300, 40);
        LBLFinish.setBounds(5, 10, 300, 40);

        LBLStart.setFont(fntLABEL);
        LBLFinish.setFont(fntLABEL);

      //LBLStart.setBounds(spLeft,10,lblWidth,lblHeight);
      //LBLFinish.setBounds(spLeft,10+spHeight+10,lblWidth,lblHeight);

        TA.setEditable(false);
        TA.setBounds(25,60,300,500);
        TA.setBackground(Color.WHITE);
        TA.setFont(fntTEXT);
        TA.setForeground(Color.GREEN);
        TA.setBackground(Color.BLACK);

        SP = new JScrollPane(TA);
        SP.setBounds(25,50,300,500);

        WDW.add(LBLStart);
        WDW.add(SP);
        WDW.add(LBLFinish);
        WDW.setVisible(true);

    }
    //==============================================================================
    private static void LoadVector() {

        ESTADO[1] = 0; //Libre
        ESTADO[2] = 0;
        ESTADO[3] = 0;
        ESTADO[4] = 0;

        Random r = new Random();   
        for(int i = 0; i<N;i++){
            V1[i] = r.nextInt(100000);
            V2[i] = r.nextInt(100000);
            V3[i] = r.nextInt(100000);
            V4[i] = r.nextInt(100000);
        }
    }
    //==============================================================================
    public static void main(String[] args) throws InterruptedException {
        AtomicInteger AI1 = new AtomicInteger(1);
        AtomicInteger AI2 = new AtomicInteger(1);
        AtomicInteger AI3 = new AtomicInteger(1); //AI1.get()==1
        AtomicInteger AI4 = new AtomicInteger(1); //AI1.get()==1
        LoadVector();
        ConfigurarControles(WDW1,500,800,100,40,SP1,TA1,LBL1Start,LBL1Finish);
        ConfigurarControles(WDW2,500,800,500,40,SP2,TA2,LBL2Start,LBL2Finish);
        ConfigurarControles(WDW3,500,800,700,40,SP3,TA3,LBL3Start,LBL3Finish);
        ConfigurarControles(WDW4,500,800,950,40,SP4,TA4,LBL4Start,LBL4Finish);
        //-------------------------------------------------
        Thread thread1 = new Thread(new Runnable() {  //Thread.sleep(1000);
            public void run() {
                long inicio = System.currentTimeMillis();
                LBL1Start.setText("Time Execution: " + inicio / 1000 + " segundos");
                Impares(V1, TA1);
                AI1.set(0);
                long fin = System.currentTimeMillis() - inicio;
                LBL1Finish.setText("Time Execution: " + fin / 1000 + " segundos");
                System.out.println("\n");
            }
        });
        //-------------------------------------------------
        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                try {
                    //Esperar a que el hilo 1 termine antes de comenzar
                    thread1.join();
                    long inicio = System.currentTimeMillis();
                    LBL2Start.setText("Time Execution: " + inicio / 1000 + " segundos");
                    Pares(V2, TA2);
                    AI2.set(0);
                    long fin = System.currentTimeMillis() - inicio;
                    LBL2Finish.setText("Time Execution: " + fin / 1000 + " segundos");
                    System.out.println("\n");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        //-------------------------------------------------
        Thread thread3 = new Thread(new Runnable() {
            public void run() {
                try {
                    //Esperar a que el hilo 2 termine antes de comenzar
                    thread2.join();
                    long inicio = System.currentTimeMillis();
                    LBL3Start.setText("Time Execution: " + inicio / 1000 + " segundos");
                    BubbleSort2(V3,TA3);
                    AI3.set(0);
                    long fin = System.currentTimeMillis() - inicio;
                    LBL3Finish.setText("Time Execution: " + fin / 1000 + " segundos");
                    System.out.println("\n");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        //-------------------------------------------------
        Thread thread4 = new Thread(new Runnable() {
            public void run() {
                try {
                    //Esperar a que el hilo 3 termine antes de comenzar
                    thread3.join();
                    long inicio = System.currentTimeMillis();
                    LBL4Start.setText("Time Execution: " + inicio / 1000 + " segundos");
                    QuickSort(V4,1,N,TA4);
                    AI4.set(0);
                    long fin = System.currentTimeMillis() - inicio;
                    LBL4Finish.setText("Time Execution: " + fin / 1000 + " segundos");
                    System.out.println("\n");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }   
        });
        //-------------------------------------------------
        //Iniciar los hilos en serie
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
    }
    //==============================================================================
    public static int Partition(int V[],int A,int B)    {
    int E,P,TMP;
        P = A;
        E = V[B];
        for(int i = A;i<B;i++) {
            if(V[i]<=E) {
               TMP = V[i];
               V[i] = V[P];
               V[P] = TMP;
               P++;
            }
        }
        TMP = V[B];
        V[B] = V[P];
        V[P] = TMP;
        return P;
    }
    //==============================================================================
    public static void QuickSort(int V[],int A,int B, JTextArea TA) {
    int E;
        if(A<B) {
           TA.append("Partition [" + A + "," +  B + "]\n");
           E = Partition(V,A,B);
           QuickSort(V,A,E-1,TA);
           QuickSort(V,E+1,B,TA);
        }
    }
    //==============================================================================
    public static void BubbleSort(int V[], JTextArea TA) {
        int n,tmp;
            n = V.length;
            TA.setText("");
            for (int k=1;k<=n-1;k++) {
                TA.append("Cycle " + k + "\n");
                for(int i=0;i<=n-k-1;i++) {
                    if (V[i]>V[i+1]) {
                        tmp = V[i];
                        V[i] = V[i+1];
                        V[i+1] = tmp;
                    }
                }
            }  
    }
    //==============================================================================
    public static void BubbleSort2(int V[], JTextArea TA) {
    boolean Sw;
    int k,n,tmp;
        n = V.length;
        TA.setText("");
        Sw = true;
        k=1;
        while((k<=n-1)&&(Sw==true)) {
            TA.append("Cycle " + k + "\n");
            Sw = false;
            for(int i=0;i<=n-k-1;i++) {
                if (V[i]>V[i+1]) {
                    Sw = true;
                    tmp = V[i];
                    V[i] = V[i+1];
                    V[i+1] = tmp;
                }
            }
            k++;
        }  
    }
    //==============================================================================
    public long FibR(int n) {
        if(n==1) {
          return 0;
        }
       else if(n==2) {
        return 1;
       }
        else {
          return FibR(n-2) + FibR(n-1);
        }
     }
     public void ImprimirSecuenciaFibonacci(int N) {
        for(int i=1;i<=N;i++) {
            System.out.println(i + "\n"+ FibR(i));
        }
     }
    //===========================Parte de mi 
    public static void Pares(int V[], JTextArea TA){
        int n;
        n=V.length;
        TA.setText("");
        for (int i = 0; i < n; i++) {
            if(i%2==0){
                TA.append("Numero:"+i+"\n");
            }
        }
    }
    //=========================================
    public static void Impares(int V[], JTextArea TA){
        int n;
        n=V.length;
        TA.setText("");
        for (int i = 0; i < n; i++) {
            if (i%2!=0) {
                TA.append("Numero:"+i+"\n");
            }
        }
    }
}
