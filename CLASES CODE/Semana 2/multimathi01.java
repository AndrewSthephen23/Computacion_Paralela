package ejemplo;

public class multimathi01 {
    int N = 4; // Tamaño de las matrices cuadradas (N x N)
    public int A[][], B[][], C[][]; // Declaración de matrices A, B y C

    public static void main(String args[]) {
        new multimathi01().inicio(); // Crea una instancia de multimathi01 y llama al método inicio()
    }

    void inicio() {
        int H = 2; // Número de hilos a utilizar
        int d = (int)(N/H); // Rango de filas que cada hilo procesará

        // Inicialización de las matrices A, B y C con dimensiones N x N
        A = new int[N][N];
        B = new int[N][N];
        C = new int[N][N];

        // Llenar las matrices A y B con valores aleatorios
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A.length; j++) {
                A[i][j] = (int)(Math.random()*10) + 1;
                B[i][j] = (int)(Math.random()*10) + 1;
            }
        }

        // Imprimir la matriz A
        System.out.println("----------------");
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A.length; j++) {
                System.out.print(A[i][j] + " ");
            }
            System.out.println("");
        }
        System.out.println("----------------");

        // Imprimir la matriz B
        for (int i = 0; i < B.length; i++) {
            for (int j = 0; j < B.length; j++) {
                System.out.print(B[i][j] + " ");
            }
            System.out.println("");
        }
        System.out.println("****************");

        // Crear y ejecutar H - 1 hilos para realizar la multiplicación parcial
        Thread hilos[] = new Thread[40];
        for (int i = 0; i < (H - 1); i++) {
            hilos[i] = new sumintermat(i * d + 1, i * d + d);
            hilos[i].start();
        }

        // Crear y ejecutar el último hilo para procesar las filas restantes
        hilos[H - 1] = new sumintermat(((d * (H - 1)) + 1), N);
        hilos[H - 1].start();

        // Esperar a que todos los hilos terminen su ejecución
        for (int i = 0; i < H; i++) {
            try {
                hilos[i].join();
            } catch (InterruptedException ex) {
                System.out.println("error: " + ex);
            }
        }

        // Imprimir la matriz resultante C (resultado de la multiplicación)
        for (int i = 0; i < C.length; i++) {
            for (int j = 0; j < C.length; j++) {
                System.out.print(C[i][j] + " ");
            }
            System.out.println("");
        }
    }

    // Clase interna que representa el trabajo que realiza cada hilo
    public class sumintermat extends Thread {
        public int min, max;

        // Constructor de la clase sumintermat
        sumintermat(int min_, int max_) {
            min = min_ - 1;
            max = max_ - 1;
            System.out.println("min: " + min + " max: " + max);
        }

        // Método run() que contiene la lógica de ejecución del hilo
        public void run() {
            for (int k = min; k <= max; k++) {
                for (int j = 0; j < N; j++) {
                    for (int i = 0; i < N; i++) {
                        C[k][j] += A[k][i] * B[i][j]; // Realizar la multiplicación de matrices y acumular en C
                    }
                }
            }
        }
    }
}
