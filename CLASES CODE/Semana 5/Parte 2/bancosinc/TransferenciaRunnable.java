/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package bancosinc;

/**
 *
 * @author Administrador
 */
public class TransferenciaRunnable implements Runnable{
    private Banco banco;
    private int deLaCuenta;
    private double cantidadMax;
    private int repetitions;
    private int RETARDO = 10;

    /*
     * Construye una trandferencia eecutable.
     * @param b el banco entre cuyas cuentas se transfiere el dinero
     * @param de la cuenta de la cual sale el dinero transferido
     * @param max cantidad máxima de dinero por transferencia
     */
    public TransferenciaRunnable(Banco b, int de, double max){
        banco = b;
        deLaCuenta = de;
        cantidadMax = max;
    }
    public void run(){
        try{
            while(true){
            int paraLaCuenta = (int)(banco.size()*Math.random());
            double cantidad = cantidadMax * Math.random();
            banco.transferencia(deLaCuenta, paraLaCuenta, cantidad);
            Thread.sleep((int)(RETARDO * Math.random()));
            }
        }catch(InterruptedException e){}
    }
}