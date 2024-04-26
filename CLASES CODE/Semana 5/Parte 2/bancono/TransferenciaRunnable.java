
package bancono;

public class TransferenciaRunnable implements Runnable{
    private Banco banco;
    private int deLaCuenta;
    private double cantidadMax;
    private int RETARDO = 10;

    public TransferenciaRunnable(Banco b, int de, double max){
        banco = b;
        deLaCuenta = de;
        cantidadMax = max;
    }
    public void run(){
        try{
            while(true){
            int paraLaCuenta = (int) (banco.size()*Math.random());
            double cantidad = cantidadMax * Math.random();
            banco.transferencia(deLaCuenta, paraLaCuenta, cantidad);
            Thread.sleep((int)(RETARDO * Math.random()));
            }
        }catch(InterruptedException e){}
    }
}