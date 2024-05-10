/*
Este codigo define una clase Point que representa un punto en un plano
cartesiano, especificado por sus coordenadas 'x' y 'y'
 */
public class Point {//definimos la clase
    //declaramos 2 variables para representar las coordenas del punto
    private float x;
    private float y;

    public Point(float x, float y){//constructor de la clase
        this.x = x;
        this.y = y;
    }

    //metodos para obtener las coordenas
    public float getX(){return x;}
    public float getY(){return y;}

    //Metodo que actualiza las coordenas del punto con los nuevos valores especificados
    public void update(float x, float y){
        this.x = x;//actualiza valor de x
        this.y = y;//actualiza valor de y
    }

    @Override
    //sobreescribimos el metodo toString de la clase Object para devolver una
    //representacion en forma de cadena del punto
    public String toString(){
        return "(" + x + "," + y + ")";//formato (x,y)
    }
}
