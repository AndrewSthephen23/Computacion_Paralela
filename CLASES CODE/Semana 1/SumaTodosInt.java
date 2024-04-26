package ejemplo01;

public class SumaTodosInt {
    public int sum[] = new int[40]; // Arreglo para almacenar resultados parciales de la suma

    public static void main(String args[]) {
        new SumaTodosInt().inicio(); // Crea una instancia de SumaTodosInt y llama al método inicio()
    }

    void inicio() {
        int N = 10000; // Valor máximo hasta el cual sumar
        int H = 5; // Número de hilos a utilizar
        int d = (int) ((N) / H); // Rango de valores que cada hilo sumará
        Thread todos[] = new Thread[40]; // Arreglo de hilos (Threads)

        // Creación y ejecución de H - 1 hilos
        for (int i = 0; i < (H - 1); i++) {
            todos[i] = new tarea0101((i * d + 1), (i * d + d), i); // Crear un hilo tarea0101 con un rango específico
            todos[i].start(); // Iniciar la ejecución del hilo
        }

        // Creación y ejecución del último hilo
        todos[H - 1] = new tarea0101(((d * (H - 1)) + 1), N, H - 1); // Crear el último hilo con el rango restante
        todos[H - 1].start(); // Iniciar la ejecución del último hilo

        // Esperar a que todos los hilos terminen
        for (int i = 0; i < H; i++) {
            try {
                todos[i].join(); // Esperar a que el hilo termine su ejecución
            } catch (InterruptedException ex) {
                System.out.println("error" + ex); // Capturar y manejar una posible excepción de interrupción
            }
        }

        // Calcular la suma total sumando los resultados parciales
        int sumatotal = 0;
        for (int i = 0; i < H; i++) {
            sumatotal = sumatotal + sum[i]; // Sumar los resultados parciales
        }

        // Imprimir el resultado final
        System.out.println("suma total:" + sumatotal);
    }

    // Clase interna que representa el trabajo que realiza cada hilo
    public class tarea0101 extends Thread {
        public int max, min, id;

        // Constructor de la clase tarea0101
        tarea0101(int min_, int max_, int id_) {
            max = max_;
            min = min_;
            id = id_;
            System.out.println("id: " + id + " min: " + min_ + " max " + max_);
        }

        // Método run() que contiene la lógica de ejecución del hilo
        public void run() {
            int suma = 0;
            // Sumar los números en el rango [min, max]
            for (int i = min; i <= max; i++) {
                suma = suma + i;
//                try {
//                    sleep(1);
//                } catch (InterruptedException ex) {
//                    System.out.println("error " + ex);
//                }

            }
            // Almacenar el resultado parcial en el arreglo sum
            sum[id] = suma;
            System.out.println("id: " + id + " suma:" + suma); // Imprimir el resultado parcial
        }
    }
}
