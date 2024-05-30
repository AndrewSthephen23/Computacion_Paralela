# Importamos las librerias
import random 
import time #Para manejar el tiempo y pausas
import pika #Para mejorar la comunicación con RabbitMQ
import uuid #Para generar un ID unico
import flet as ft #Para crear la interfaz grafica

#Define la clase System que maneja la logica principal del sistema
class System:
    # Variables para el usuario, el ruc y el carrito de compras
    username = ""
    ruc = ""
    cart = {}

    # Constructor de la clase
    def __init__(self):
        # Credenciales para conectarse a RabbitMQ
        credentials = pika.PlainCredentials('chan', 'chan')
        # Establece la coneccion con RabbitMQ
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters('localhost', 5672, 'venta_host', credentials))
        
        # Crea un canal para la comunicacion 
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

    # Metodo para editar el carrito de compras
    def edit_cart(self, id, type, tf):
        # Si el ID del producto ya esta en el carrito, actualiza la cantidad
        if id in self.cart:
            self.cart[id] += type
            # Si la cantidad es 0, elimina el ID del producto del carrito
            if self.cart[id] == 0:
                del self.cart[id]
        else:
            # Si el ID del producto no esta en el carrito, lo agrega con cantidad 1
            self.cart[id] = 1
        # Actualiza el valor de la caja de texto con la cantidad actual del producto    
        tf.value = str(self.cart[id]) if id in self.cart else "0"
        tf.update()
    
    # Metodo para limpiar el carrito de compras
    def clear_cart(self,tf_list):
        # Limpia el carrito de compras
        self.cart = {}
        # Actualiza todos los campos de texto a "0"        
        for tf in tf_list:
            tf.value = "0"
            tf.update()

    # Metodo para obtener la lista de productos desde el servidor
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
            body="get_products")
        
        # Espera la respuesta del servidor
        self.connection.process_data_events(time_limit=None)
        # Devuelve la respuesta recibida
        return str(self.response, "utf-8")

    # Callback para manejar la respuesta del servidor
    def on_response(self, ch, method, props, body):
        # Si el ID de la respuesta coincide con el ID de la petición, guarda la respuesta
        if self.corr_id == props.correlation_id:
            self.response = body

    # Metodo para generar una factura
    def generate_bill(self, mensaje):
        self.response = None
        self.corr_id = str(uuid.uuid4())
        # Publica la solicitud de generacion de factura en la cola
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

# Crea un objeto de la clase System
system = System()

# Define la funcion para la pagina inicial de la aplicacion
def initial_page(page: ft.Page):
    # Limpia la pagina y reinicia las variables del sistema
    page.clean()
    system.username = ""
    system.ruc = ""
    system.cart = {}

    # Funcion para manejar el inicio de session
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

    # Agrega los elementos a la pagina
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

# Define la funcion para mostrar los productos
def show_products(page: ft.Page):
    prods = system.get_products() # Obtiene los productos del servidor
    prods = prods[:-1] # Elimina el ";" al final
    prods = prods.split(";") # Separa la cadena en una lista de productos
    page.clean() # Limpia la pagina 
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
                            icon_color="blue400",
                            icon_size=20,
                            tooltip="Remover del carrito",
                            on_click=create_remove_func() # Llama a la función para remover del carrito
                        ),
                        tf,
                        ft.IconButton(
                            icon=ft.icons.ADD,
                            icon_color="blue400",
                            icon_size=20,
                            tooltip="Agregar al carrito",
                            on_click=create_add_func() # Llama a la funcion para añadir al carrito
                        ),
                    ],
                    alignment=ft.MainAxisAlignment.CENTER,
                    spacing=10,
                )
            )
        ]
        rows_list.append(temp) # Añade la fila a la lista

    # Añade los elementos a la pagina
    page.add(ft.SafeArea(
        content=ft.Container(
            content=ft.Column(
                controls=[
                    ft.Column(
                        controls=[
                            ft.Text(
                                value="Productos",
                                font_family="Arial",
                                color="blue",
                                size=40,
                            ),
                            ft.DataTable(
                                columns=[
                                    ft.DataColumn(ft.Text(col_name)) for col_name in ["ID", "Nombre", "Precio", "Agregar"]
                                ],
                                rows=[
                                    ft.DataRow(
                                        cells=cell
                                    ) for cell in rows_list
                                ],
                                width=1000,
                                height=4900,
                            ),
                        ],
                        scroll=True,
                        width=1000,
                        height=500,
                        horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                        alignment=ft.MainAxisAlignment.CENTER,
                        spacing=30
                    ),
                    ft.ElevatedButton(
                        text="Generar Factura",
                        width=200,
                        height=40,
                        on_click=lambda e: generate_bill(page) # Llama a la funcion para generar la factura
                    ),
                    ft.Row(
                        controls=[
                            ft.ElevatedButton(
                                text="Borrar Carrito",
                                width=200,
                                height=40,
                                on_click=lambda e: system.clear_cart(tf_list) # Llama a la funcion para borrar el carrito
                            ),
                            ft.ElevatedButton(
                                text="Volver",
                                width=200,
                                height=40,
                                on_click=lambda e: main_page(page),# Llama a la funcion para volver a la pagina principal
                            )
                        ],
                        alignment=ft.MainAxisAlignment.CENTER,
                        spacing=30
                    )],
                horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                width=1000,
                height=700,
                spacing=40
            ),
        )))

# Funcion para generar la factura
def generate_bill(page: ft.Page):
    if len(system.cart) == 0:# Si el carrito esta vacio, no hace nada
        return
    
    # Funcion para enviar la factura
    def send_bill(e):
        mensaje = system.username + ";" + system.ruc + ";"
        for id in system.cart:# Añade los productos del carrito  al mensaje
            mensaje += str(id) + "," + str(system.cart[id]) + "/"
        mensaje = mensaje[:-1] # Elimina el ultimo caracter del mensaje
        system.cart = {} # Vacia el carrito
        status = system.generate_bill(mensaje) # Genera la factura
        main_page(page,status) # Navega a la pagina principal con el status de la factura
    page.clean() # Limpia la pagina
    page.add(ft.SafeArea(
        content=ft.Container(
            content=ft.Column(
                controls=[
                    ft.Text(
                        value="Factura",
                        font_family="Arial",
                        color="blue",
                        size=40,
                    ),
                    ft.DataTable(
                        columns=[
                            ft.DataColumn(ft.Text(col_name)) for col_name in ["ID", "Cantidad"]
                        ],
                        rows=[
                            ft.DataRow(
                                cells=[
                                    ft.DataCell(
                                        content=ft.Text(items[0])),
                                    ft.DataCell(
                                        content=ft.Text(items[1])),
                                ]
                            ) for items in system.cart.items()
                        ],
                        width=1000,
                    ),
                    ft.ElevatedButton(
                        text="Generar Factura",
                        width=200,
                        height=40,
                        on_click=send_bill # Llama a la funcion para enviar la factura
                    ),
                    ft.ElevatedButton(
                        text="Volver",
                        width=200,
                        height=40,
                        on_click=lambda e: main_page(page), # Llama a la funcion para volver a la pagina principal
                    )
                ],
                horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                alignment=ft.MainAxisAlignment.CENTER,
                spacing=30,
                scroll=True
            )
        ),
        minimum=30
    ))

# Funcion para mostrar la pagina principal
def main_page(page: ft.Page, message=""):
    page.clean() # Limpia la pagina
    user_text = ft.Text(
        value="Bienvenido " + system.username + " con RUC: " + system.ruc,
        font_family="Arial",
        color="white",
        size=20,
    )
    user_message = ft.Text(
        value=message,
        font_family="Arial",
        color="white",
        size=20,
    )
    page.add(ft.SafeArea(
        content=ft.Container(
            content=ft.Column(
                controls=[
                    user_text,
                    user_message,
                    ft.ElevatedButton(
                        text="Generar Factura",
                        width=200,
                        height=40,
                        on_click=lambda e: generate_bill(page) # Llama a la funcion para generar la factura
                    ),
                    ft.ElevatedButton(
                        text="Ver Productos",
                        width=200,
                        height=40,
                        on_click=lambda e: show_products(page) # Llama a la funcion para mostrar los productos
                    ),
                    ft.ElevatedButton(
                        text="Salir",
                        width=200,
                        height=40,
                        on_click=lambda e: initial_page(page) # Llama a la funcion para cerrar la sesion
                    )
                ],
                horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                alignment=ft.MainAxisAlignment.CENTER,
                spacing=30
            )
        ),
        minimum=30
    ))

# Funcion principal para configurar la ventana
def main_window(page: ft.Page):
    page.window_height = 750 # Establece la altura de la ventana
    page.window_width = 1000 # Establece el ancho de la ventana
    page.window_resizable = False # La ventana no es redimensionable
    page.window_full_screen = False # La ventana no es de pantalla completa
    page.horizontal_alignment = ft.MainAxisAlignment.CENTER # Alineacion horizontal centrada
    page.title = "Sistema Venta" # Titulo de la ventana
    page.horizontal_alignment = ft.CrossAxisAlignment.CENTER # Alineacion horizontal centrada
    page.window_center() # Centra la ventana
    page.window_visible = True # Hace visible la ventana
    initial_page(page) # Muestra la pagina inicial

# Inicia la aplicacion
ft.app(target=main_window, view=ft.AppView.FLET_APP_HIDDEN)


