package ejemplo01;
public class Sumafinita { // Esta línea declara una clase pública llamada "Sumafinita".
    double[] stotal; /* Esta línea declara un arreglo de números decimales
                        llamado "stotal" que se utilizará para almacenar resultados parciales.*/

    public void inicio(){ //Esta línea declara un método público llamado "inicio" que se encargará de inicializar y ejecutar el proceso de cálculo.
        int n = 10000,h = 12, seg = (int)(n/h) ,a ,b;  // Esta línea declara varias variables enteras

        trabajo[] htrab = new trabajo[90]; // Esta línea declara un arreglo de objetos "trabajo" llamado "htrab" con una longitud de 90 elementos.
        Thread[] t = new Thread[90];  // Esta línea declara un arreglo de objetos "Thread" llamado "t" con una longitud de 90 elementos
        stotal  = new double[90];  // Esta línea inicializa el arreglo "stotal" con una longitud de 90
        for (int i = 1; i <= h; i++) { // Esta línea inicia un bucle "for" que se ejecutará "h" veces (12 veces en este caso).
            a = (i-1)*seg+1;//Representa el inicio del rango de cálculo para este hilo.
            b = (i)*seg;//Representa el final del rango de cálculo para este hilo.

            // Creación de un objeto "trabajo" y un objeto "Thread" asociado.
            htrab[i] = new  trabajo(a,b,i);//crea un nuevo objeto de la clase interna trabajo
            t[i] = new Thread(htrab[i]);/* Aquí se crea un nuevo objeto Thread asociado al objeto trabajo
                                          Se utiliza el objeto trabajo como argumento del constructor de Thread.
                                          Esto vincula el hilo de ejecución (Thread) con la tarea específica (trabajo) que debe realizar.
                                          */
            t[i].start(); /*  Al llamar a start(), se le indica al sistema que comience a ejecutar
                                el método run() de la clase trabajo en un hilo separado.
                                El método run() es donde se define la lógica específica de la tarea
                                (trabajo) que se realizará en paralelo.
                            */
        }
        try {
            // Espera a que todos los hilos terminen su ejecución antes de continuar.
            for (int i = 1; i <= h; i++) {
                t[i].join();
            }
        } catch (Exception e) {}
        // Cálculo del total sumando los valores parciales almacenados en "stotal".
        double total=0;
        for (int i = 1; i <= h; i++) {
            total = total +  stotal[i];
        }
        System.out.println("TOTAL: "+total);
    }

    // Clase interna "trabajo" que extiende Thread para realizar trabajo concurrente.
    public class trabajo extends Thread{
        int ini,fin,tmp;
        double sum=0;

        // Constructor de la clase "trabajo".
        public trabajo(int ini_,int fin_,int tmp_){
            ini=ini_;fin=fin_;tmp=tmp_;
        }

        // Método que se ejecutará al iniciar el hilo.
        public void run(){

            // Bucle para realizar cálculos parciales.
            for (int i = ini; i <= fin; i++) {
                for (int j = 0; j < 1000000; j++) {
                    sum = sum + Math.sin(i);
                }
            }
            // Almacenar el resultado parcial en el arreglo "stotal".
            stotal[tmp]=sum;
            // Imprimir el resultado parcial.
            System.out.println("rpta :"+ sum);
        }
    }
    // Método principal que inicia la ejecución del programa.
    public static void main(String[] args){
        new Sumafinita().inicio(); // Crear una instancia de Sumafinita y llamar al método "inicio"
    }
}