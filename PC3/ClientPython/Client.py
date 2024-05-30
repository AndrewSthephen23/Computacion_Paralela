import random
import time
import pika
import uuid
import flet as ft

class System:
    username = ""
    ruc = ""
    cart = {}

    def __init__(self):
        credentials = pika.PlainCredentials('chan', 'chan')
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters('localhost', 5672, 'venta_host', credentials))

        self.channel = self.connection.channel()

        result = self.channel.queue_declare(queue='', exclusive=True)
        self.callback_queue = result.method.queue

        self.channel.basic_consume(
            queue=self.callback_queue,
            on_message_callback=self.on_response,
            auto_ack=True)

        self.response = None
        self.corr_id = None

    def edit_cart(self, id, type, tf):
        if id in self.cart:
            self.cart[id] += type
            if self.cart[id] == 0:
                del self.cart[id]
        else:
            self.cart[id] = 1
        tf.value = str(self.cart[id]) if id in self.cart else "0"
        tf.update()
    
    def clear_cart(self,tf_list):
        self.cart = {}
        for tf in tf_list:
            tf.value = "0"
            tf.update()

    def get_products(self):
        self.response = None
        self.corr_id = str(uuid.uuid4())
        self.channel.basic_publish(
            exchange='',
            routing_key='go-python-queue',
            properties=pika.BasicProperties(
                reply_to=self.callback_queue,
                correlation_id=self.corr_id,
            ),
            body="get_products")
        self.connection.process_data_events(time_limit=None)
        return str(self.response, "utf-8")

    def on_response(self, ch, method, props, body):
        if self.corr_id == props.correlation_id:
            self.response = body

    def generate_bill(self, mensaje):
        self.response = None
        self.corr_id = str(uuid.uuid4())
        self.channel.basic_publish(
            exchange='',
            routing_key='go-python-queue',
            properties=pika.BasicProperties(
                reply_to=self.callback_queue,
                correlation_id=self.corr_id,
            ),
            body=mensaje)
        self.connection.process_data_events(time_limit=None)
        return str(self.response, "utf-8")

system = System()

def initial_page(page: ft.Page):
    page.clean()
    system.username = ""
    system.ruc = ""
    system.cart = {}

    def login(username, ruc):
        system.username = username
        system.ruc = ruc
        main_page(page)

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

def show_products(page: ft.Page):
    prods = system.get_products()
    prods = prods[:-1]
    prods = prods.split(";")
    page.clean()
    rows_list = []
    tf_list = []
    for prod in prods:
        items = prod.split(",")
        id = int(items[0])
        tf = ft.TextField(
            value=str(system.cart[id]) if id in system.cart else "0",
            width=50,
            height=30,
            text_align=ft.TextAlign.CENTER,
            disabled=True,
        )
        tf_list.append(tf)
        def create_remove_func(id=id, tf=tf):
            return lambda e, item_id=id: system.edit_cart(item_id, -1, tf)

        def create_add_func(id=id, tf=tf):
            return lambda e, item_id=id: system.edit_cart(item_id, 1, tf)

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
                            on_click=create_remove_func()
                        ),
                        tf,
                        ft.IconButton(
                            icon=ft.icons.ADD,
                            icon_color="blue400",
                            icon_size=20,
                            tooltip="Agregar al carrito",
                            on_click=create_add_func()
                        ),
                    ],
                    alignment=ft.MainAxisAlignment.CENTER,
                    spacing=10,
                )
            )
        ]
        rows_list.append(temp)

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
                        on_click=lambda e: generate_bill(page)
                    ),
                    ft.Row(
                        controls=[
                            ft.ElevatedButton(
                                text="Borrar Carrito",
                                width=200,
                                height=40,
                                on_click=lambda e: system.clear_cart(tf_list)
                            ),
                            ft.ElevatedButton(
                                text="Volver",
                                width=200,
                                height=40,
                                on_click=lambda e: main_page(page),
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

def generate_bill(page: ft.Page):
    if len(system.cart) == 0:
        return
    def send_bill(e):
        mensaje = system.username + ";" + system.ruc + ";"
        for id in system.cart:
            mensaje += str(id) + "," + str(system.cart[id]) + "/"
        mensaje = mensaje[:-1]
        system.cart = {}
        status = system.generate_bill(mensaje)
        main_page(page,status)
    page.clean()
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
                        on_click=send_bill
                    ),
                    ft.ElevatedButton(
                        text="Volver",
                        width=200,
                        height=40,
                        on_click=lambda e: main_page(page),
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

def main_page(page: ft.Page, message=""):
    page.clean()
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
                        on_click=lambda e: generate_bill(page)
                    ),
                    ft.ElevatedButton(
                        text="Ver Productos",
                        width=200,
                        height=40,
                        on_click=lambda e: show_products(page)
                    ),
                    ft.ElevatedButton(
                        text="Salir",
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

def main_window(page: ft.Page):
    page.window_height = 750
    page.window_width = 1000
    page.window_resizable = False
    page.window_full_screen = False
    page.horizontal_alignment = ft.MainAxisAlignment.CENTER
    page.title = "Sistema Venta"
    page.horizontal_alignment = ft.CrossAxisAlignment.CENTER
    page.window_center()
    page.window_visible = True
    initial_page(page)


ft.app(target=main_window, view=ft.AppView.FLET_APP_HIDDEN)


