import threading
import pika
import tkinter as tk

def enviar_mensajes():
    origen = origen_var.get()
    destino = destino_var.get()
    cantidad = cantidad_var.get()

    mensaje = nombre + ";" + ruc + ";" + origen + ";" + destino + ";" + cantidad

    credentials = pika.PlainCredentials('admin', 'admin')
    connection = pika.BlockingConnection(pika.ConnectionParameters('localhost', 5672, 'viajes_host', credentials))
    channel = connection.channel()

    channel.queue_declare(queue='go-python-queue')
    channel.basic_publish(exchange='', routing_key='go-python-queue', body=mensaje)
    connection.close()

def recibir_mensajes():
    credentials = pika.PlainCredentials('admin', 'admin')
    connection = pika.BlockingConnection(pika.ConnectionParameters('localhost', 5672, 'viajes_host', credentials))
    channel = connection.channel()

    channel.queue_declare(queue='go-python-queue')

    def callback(ch, method, properties, body):
        if body.decode() == "0":
            resultado_label.config(text="Cantidad no disponible")
        elif body.decode() == "2":
            resultado_label.config(text="Compra exitosa")
        else:
            subcadenas = body.decode().split(";")
            resultado_label.config(text=f"Recibido: {subcadenas[0]} - {subcadenas[1]} - {subcadenas[2]} - {subcadenas[3]}")

    channel.basic_consume(queue='go-python-queue', on_message_callback=callback, auto_ack=True)

    print('Esperando mensajes...')
    channel.start_consuming()

def enviar_mensajes_thread():
    threading.Thread(target=enviar_mensajes).start()

def recibir_mensajes_thread():
    threading.Thread(target=recibir_mensajes).start()

if __name__ == "__main__":
    nombre = input("---------Ingresa tu nombre:  \n")
    ruc = input("---------Ingresa tu RUC:  \n")

    root = tk.Tk()
    root.title("Aplicación de Envío y Recepción")

    origen_label = tk.Label(root, text="Origen:")
    origen_label.grid(row=0, column=0)
    origen_var = tk.StringVar()
    origen_entry = tk.Entry(root, textvariable=origen_var)
    origen_entry.grid(row=0, column=1)

    destino_label = tk.Label(root, text="Destino:")
    destino_label.grid(row=1, column=0)
    destino_var = tk.StringVar()
    destino_entry = tk.Entry(root, textvariable=destino_var)
    destino_entry.grid(row=1, column=1)

    cantidad_label = tk.Label(root, text="Cantidad:")
    cantidad_label.grid(row=2, column=0)
    cantidad_var = tk.StringVar()
    cantidad_entry = tk.Entry(root, textvariable=cantidad_var)
    cantidad_entry.grid(row=2, column=1)

    enviar_button = tk.Button(root, text="Enviar", command=enviar_mensajes_thread)
    enviar_button.grid(row=3, column=0)

    recibir_button = tk.Button(root, text="Recibir", command=recibir_mensajes_thread)
    recibir_button.grid(row=3, column=1)

    resultado_label = tk.Label(root, text="")
    resultado_label.grid(row=4, columnspan=2)

    root.mainloop()
