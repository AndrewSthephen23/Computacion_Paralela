
package bancono;

class Banco {

    private final double[] cuentas;
    public Banco(int n, double saldoInicial){
    cuentas = new double[n];
    for(int i = 0; i< cuentas.length;i++)
        cuentas[i] = saldoInicial;
    }
    public void transferencia(int de, int para, double cantidad){
        if(cuentas[de]<cantidad) return;
        System.out.print(Thread.currentThread());
        cuentas[de] -= cantidad;
        System.out.printf(" %10.2f de %d para %d",cantidad, de , para);
        cuentas[para] += cantidad;
        System.out.printf(" Saldo total %10.2f%n",getSaldoTotal());
    }
    public double getSaldoTotal(){
        double sum = 0;
        for(double a :cuentas)
            sum +=a;
        return sum;
    }

    public int size(){
        return cuentas.length;
    }
}
