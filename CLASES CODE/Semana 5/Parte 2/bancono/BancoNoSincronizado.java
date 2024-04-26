package bancono;

public class BancoNoSincronizado {
    public static final int NUMCUENTAS = 100;
    public static final double SALDO_INICIAL = 1000;
    public static void main(String[] args){
        Banco b = new Banco(NUMCUENTAS , SALDO_INICIAL);
        for(int i = 0 ; i<NUMCUENTAS ; i++){
            TransferenciaRunnable r = new TransferenciaRunnable(b,i,SALDO_INICIAL);
            Thread t = new Thread(r);
            t.start();            
        }
    }
}
