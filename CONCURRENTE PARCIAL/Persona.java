public class Persona {
    private int id;
    private int edad;
    private int sexo; //0:hombre,1:mujer
    private int peso;
    private int altura; //cm
    private int clasificación=-1;

    public Persona(int id, int edad, int sexo, int peso, int altura) {
        this.id = id;
        this.edad = edad;
        this.sexo = sexo;
        this.peso = peso;
        this.altura = altura;
    }

    public int getId() {
        return id;
    }

    public int getEdad() {
        return edad;
    }

    public int getSexo() {
        return sexo;
    }

    public int getPeso() {
        return peso;
    }

    public int getAltura() {
        return altura;
    }

    public int getClasificación() {
        return clasificación;
    }
    @Override
    public String toString(){
        return "(" + id + "," + edad + "," + sexo + "," + peso + "," + altura + ")";
    }
}
