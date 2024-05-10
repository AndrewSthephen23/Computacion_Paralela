import java.util.ArrayList;
import java.util.List;
/*
Implementamos una cola de mensajes 'messageQueue' que permite agregar mensajes,
obtener el proximo mensaje y obtener el tamaño actual de la cola de mensajes
 */

class MessageQueue { //Definimos la clase
    //declaramos una lista la cual almacenara los mensajes de la cola
    private final List<String> messages = new ArrayList<>();

    public void addMessage(String message) {//este metodo agrega mensajes a la cola
        //Sincronizamos el bloque de codigo para garantizar que solo un hilo
        //pueda acceder a la lista 'messages' a la vez y evitarnos problemas de concurrencia
        //cuando multiples hilos intentan agregar mensajes simultaneamente
        synchronized (messages) {
            messages.add(message);//agrega mensaje al final de la lista
        }
    }

    public String getNextMessage() {//este metodo obtiene y elimina el proximo mensaje de la cola
        synchronized (messages) { //sincronizamos el bloque de codigo para su acceso seguro a la lista
            if (!messages.isEmpty()) {//verifica si la lista 'messages' no esta vacia
                String message = messages.get(0);//Obtiene el primer mensaje de la lista
                messages.remove(0);//se elimina de la lista
                return message;//devolvemos el mensaje
            }
            return null;//si la lista esta vacia retorna null
        }
    }

    public int getSize(){//este metodo devuelve el tamaño actual de la cola
        return messages.size();//devuelve el # de elementos de la lista 'messages'.
    }
}

