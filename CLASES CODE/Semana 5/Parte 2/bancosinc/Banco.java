
package bancosinc;

import java.util.concurrent.locks.*;

public class Banco{
    private final double[] cuentas;
    private Lock cierreBanco;
    private Condition fondosSuficientes;
    public Banco(int n, double saldoInicial){
    cuentas = new double[n];
    for(int i = 0; i< cuentas.length;i++)
        cuentas[i] = saldoInicial;
        cierreBanco = new ReentrantLock();
        fondosSuficientes = cierreBanco.newCondition();
    }
    public void transferencia(int de, int para, double cantidad)
    throws InterruptedException{
        cierreBanco.lock();//cierre
        try{
            while(cuentas[de]<cantidad)
                fondosSuficientes.await();
            System.out.print(Thread.currentThread());
            cuentas[de] -= cantidad;
            System.out.printf(" %10.2f de %d para %d",cantidad, de , para);
            cuentas[para] += cantidad;
            System.out.printf(" Saldo total %10.2f%n",getSaldoTotal());
            fondosSuficientes.signalAll();
        }finally
        {
            cierreBanco.unlock();
        }
    }
    public double getSaldoTotal(){
        cierreBanco.lock();
        try{
            double sum = 0;
            for(double a :cuentas)
                sum +=a;
            return sum;
        }finally{
            cierreBanco.unlock();
        }
    }
    public int size(){
        return cuentas.length;
    }
}
