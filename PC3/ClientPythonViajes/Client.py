# Importamos las librerías
import random
import time #Para manejar el tiempo y pausas
import pika #Para mejorar la comunicación con RabbitMQ
import uuid #Para generar un ID único
import flet as ft #Para crear la interfaz gráfica
import asyncio # Para manejar el bucle de eventos

# Define la clase System que maneja la lógica principal del sistema
class System:
    # Variables para el usuario, el ruc y el carrito de compras
    username = ""
    ruc = ""
    cart = {}

    # Constructor de la clase
    def __init__(self):
        # Credenciales para conectarse a RabbitMQ
        credentials = pika.PlainCredentials('admin', 'admin')
        # Establece la conexión con RabbitMQ
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters('localhost', 5672, 'viajes_host', credentials))

        # Crea un canal para la comunicación 
        self.channel = self.connection.channel()

        # Declara una cola exclusiva para las respuestas
        result = self.channel.queue_declare(queue='', exclusive=True)
        self.callback_queue = result.method.queue

        # Configura el consumidor para recibir mensajes de la cola de respuestas
        self.channel.basic_consume(
            queue=self.callback_queue,
            on_message_callback=self.on_response,
            auto_ack=True)

        # Inicializa las variables de respuesta y correlación
        self.response = None
        self.corr_id = None

    # Método para editar el carrito de compras
    def edit_cart(self, id, type, tf):
        # Si el ID del producto ya está en el carrito, actualiza la cantidad
        if id in self.cart:
            self.cart[id] += type
            # Si la cantidad es 0, elimina el ID del producto del carrito
            if self.cart[id] == 0:
                del self.cart[id]
        else:
            # Si el ID del producto no está en el carrito, lo agrega con cantidad 1
            self.cart[id] = 1
        # Actualiza el valor de la caja de texto con la cantidad actual del producto    
        tf.value = str(self.cart[id]) if id in self.cart else "0"
        tf.update()

    # Método para limpiar el carrito de compras
    def clear_cart(self, tf_list):
        # Limpia el carrito de compras
        self.cart = {}
        # Actualiza todos los campos de texto a "0"        
        for tf in tf_list:
            tf.value = "0"
            tf.update()

    # Método para obtener la lista de productos desde el servidor
    def get_products(self):
        self.response = None
        self.corr_id = str(uuid.uuid4())

        # Publica una solicitud de productos en la cola de peticiones
        self.channel.basic_publish(
            exchange='',
            routing_key='go-python-queue',
            properties=pika.BasicProperties(
                reply_to=self.callback_queue,
                correlation_id=self.corr_id,
            ),
            body="get_rutas")

        # Espera la respuesta del servidor
        self.connection.process_data_events(time_limit=None)
        # Devuelve la respuesta recibida
        return str(self.response, "utf-8")

    # Callback para manejar la respuesta del servidor
    def on_response(self, ch, method, props, body):
        # Si el ID de la respuesta coincide con el ID de la petición, guarda la respuesta
        if self.corr_id == props.correlation_id:
            self.response = body

    # Método para verificar la disponibilidad de asientos
    def check_seat_availability(self):
        self.response = None
        self.corr_id = str(uuid.uuid4())

        # Crea el mensaje para verificar la disponibilidad de asientos
        mensaje = f"{self.username};{self.ruc};"
        for route_id, quantity in self.cart.items():
            mensaje += f"{route_id},{quantity}/"
        mensaje = mensaje[:-1]  # Elimina el último '/'

        # Publica la solicitud de verificación en la cola de peticiones
        self.channel.basic_publish(
            exchange='',
            routing_key='go-python-queue',
            properties=pika.BasicProperties(
                reply_to=self.callback_queue,
                correlation_id=self.corr_id,
            ),
            body=mensaje)

        # Espera la respuesta del servidor
        self.connection.process_data_events(time_limit=None)
        # Procesa la respuesta recibida
        response_str = str(self.response, "utf-8")
        available = response_str == "Venta Realizada"
        return available

    # Método para generar una factura
    def generate_bill(self, mensaje):
        self.response = None
        self.corr_id = str(uuid.uuid4())
        # Publica la solicitud de generación de factura en la cola
        self.channel.basic_publish(
            exchange='',
            routing_key='go-python-queue',
            properties=pika.BasicProperties(
                reply_to=self.callback_queue,
                correlation_id=self.corr_id,
            ),
            body=mensaje)
        # Espera la respuesta del servidor
        self.connection.process_data_events(time_limit=None)
        # Devuelve la respuesta recibida
        return str(self.response, "utf-8")

    # Método para realizar la venta
    def perform_sale(self):
        if self.check_seat_availability():
            mensaje = f"{self.username};{self.ruc};"
            for route_id, quantity in self.cart.items():
                mensaje += f"{route_id},{quantity}/"
            mensaje = mensaje[:-1]  # Elimina el último '/'
            return self.generate_bill(mensaje)
        else:
            return "No se pudo realizar la venta"

# Crea un objeto de la clase System
system = System()

# Define la función para la página inicial de la aplicación
def initial_page(page: ft.Page):
    # Limpia la página y reinicia las variables del sistema
    page.clean()
    system.username = ""
    system.ruc = ""
    system.cart = {}

    # Función para manejar el inicio de sesión
    def login(username, ruc):
        system.username = username
        system.ruc = ruc
        main_page(page)

    # Define los campos de texto para el nombre y el RUC
    username = ft.TextField(
        value="",
        border_color="white",
        width=200
    )
    ruc = ft.TextField(
        value="",
        border_color="white",
        width=200
    )

    # Agrega los elementos a la página
    page.add(ft.SafeArea(
        content=ft.Container(
            content=ft.Column(
                controls=[
                    ft.Text(
                        value="Sistema Venta",
                        font_family="Arial",
                        color="blue",
                        size=40,
                    ),
                    ft.Text(
                        value="Ingrese su nombre",
                        font_family="Arial",
                        color="white",
                        size=20,
                    ),
                    username,
                    ft.Text(
                        value="Ingrese su RUC",
                        font_family="Arial",
                        color="white",
                        size=20,
                    ),
                    ruc,
                    ft.ElevatedButton(
                        text="Ingresar",
                        width=200,
                        height=40,
                        on_click=lambda e: login(username.value, ruc.value)
                    )
                ],
                horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                alignment=ft.MainAxisAlignment.CENTER,
                spacing=30
            )
        ),
        minimum=30
    ))

# Define la función para mostrar los productos
def show_products(page: ft.Page):
    prods = system.get_products() # Obtiene los productos del servidor
    prods = prods[:-1] # Elimina el ";" al final
    prods = prods.split(";") # Separa la cadena en una lista de productos
    page.clean() # Limpia la página 
    rows_list = [] # Lista para almacenar las filas de la tabla
    tf_list = [] # Lista para almacenar los campos de texto
    for prod in prods:
        items = prod.split(",") # Separa los detalles del producto
        id = int(items[0]) # Obtiene el ID del producto
        tf = ft.TextField(
            value=str(system.cart[id]) if id in system.cart else "0", # Muestra la cantidad en el carrito
            width=50,
            height=30,
            text_align=ft.TextAlign.CENTER,
            disabled=True,
        )
        tf_list.append(tf) # Añade el campo de texto a la lista

        # Funciones para añadir o quitar productos del carrito
        def create_remove_func(id=id, tf=tf):
            return lambda e, item_id=id: system.edit_cart(item_id, -1, tf)
        def create_add_func(id=id, tf=tf):
            return lambda e, item_id=id: system.edit_cart(item_id, 1, tf)

        # Crea las filas de la tabla
        temp = [
            ft.DataCell(
                content=ft.Text(items[0])),
            ft.DataCell(
                ft.Text(items[1])),
            ft.DataCell(
                ft.Text(items[2])),
            ft.DataCell(
                ft.Row(
                    controls=[
                        ft.IconButton(
                            icon=ft.icons.REMOVE,
                            icon_size=15,
                            on_click=create_remove_func()
                        ),
                        tf,
                        ft.IconButton(
                            icon=ft.icons.ADD,
                            icon_size=15,
                            on_click=create_add_func()
                        )
                    ],
                    alignment=ft.MainAxisAlignment.CENTER,
                    spacing=0
                )
            )
        ]
        rows_list.append(temp) # Añade la fila a la lista

    # Define los encabezados de la tabla
    col_names = ["ID", "Nombre", "Precio", "Acciones"]

    # Añade los elementos a la página
    page.add(ft.SafeArea(
        content=ft.Container(
            content=ft.Column(
                controls=[
                    ft.Row(
                        controls=[
                            ft.ElevatedButton(
                                text="Regresar",
                                on_click=lambda e: main_page(page)
                            ),
                            ft.ElevatedButton(
                                text="Limpiar Carrito",
                                on_click=lambda e: system.clear_cart(tf_list)
                            )
                        ]
                    ),
                    ft.Text(
                        value="Lista de Productos",
                        font_family="Arial",
                        color="white",
                        size=30,
                    ),
                    ft.DataTable(
                        columns=[
                            ft.DataColumn(
                                ft.Text(col_names[0])
                            ),
                            ft.DataColumn(
                                ft.Text(col_names[1])
                            ),
                            ft.DataColumn(
                                ft.Text(col_names[2])
                            ),
                            ft.DataColumn(
                                ft.Text(col_names[3])
                            )
                        ],
                        rows=[ft.DataRow(temp) for temp in rows_list]
                    ),
                    ft.ElevatedButton(
                        text="Comprar",
                        on_click=lambda e: purchase(page)
                    ),
                ],
                horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                alignment=ft.MainAxisAlignment.CENTER,
                spacing=20
            )
        ),
        minimum=20
    ))

# Define la función para la página principal
def main_page(page: ft.Page):
    page.clean() # Limpia la página
    # Añade los elementos a la página
    page.add(ft.SafeArea(
        content=ft.Container(
            content=ft.Column(
                controls=[
                    ft.Text(
                        value="Bienvenido al Sistema de Venta de Boletos",
                        font_family="Arial",
                        color="blue",
                        size=30,
                    ),
                    ft.ElevatedButton(
                        text="Mostrar Productos",
                        width=200,
                        height=40,
                        on_click=lambda e: show_products(page)
                    ),
                    ft.ElevatedButton(
                        text="Cerrar Sesión",
                        width=200,
                        height=40,
                        on_click=lambda e: initial_page(page)
                    )
                ],
                horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                alignment=ft.MainAxisAlignment.CENTER,
                spacing=30
            )
        ),
        minimum=30
    ))

# Define la función para manejar la compra
def purchase(page: ft.Page):
    sale_result = system.perform_sale()
    page.clean()
    # Muestra el resultado de la compra
    page.add(ft.SafeArea(
        content=ft.Container(
            content=ft.Column(
                controls=[
                    ft.Text(
                        value=sale_result,
                        font_family="Arial",
                        color="blue",
                        size=30,
                    ),
                    ft.ElevatedButton(
                        text="Regresar",
                        width=200,
                        height=40,
                        on_click=lambda e: main_page(page)
                    )
                ],
                horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                alignment=ft.MainAxisAlignment.CENTER,
                spacing=30
            )
        ),
        minimum=30
    ))

# Ejecuta la aplicación con la página inicial
ft.app(target=initial_page)
