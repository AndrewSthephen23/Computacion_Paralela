package org.example;

import java.sql.DriverManager; //Importacion para manejar la conexión JDBC
import java.sql.SQLException;// Importacion para manejar las excepciones de SQL
import java.sql.Connection; // Importacion para representar una conexion a la base de datos

// Declaramos la clase ServerJava
public class ServerJava {
    // Variable para almacenar la conexion a la base de datos PostgreSQL
    Connection postgresConnection;
    // Metodo para establecer una conexion a la base de datos PostgreSQL
    public void connectToPostgres(){
        try {
            // Carga el driver de PostgreSQL
            Class.forName("org.postgresql.Driver");
            // Establece la url,el nombre de usuario y la contrasena
            String url = "jdbc:postgresql://localhost:5432/salesdb";
            String username = "postgres";
            String password = "root";

            // Establece la conexion a la base de datos PostgreSQL
            postgresConnection = DriverManager.getConnection(url, username, password);
        // Captura las excepciones de clase no encontrada y SQL
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace(); // Imprime el error
        }
    }

    // Metodo para obtener el id de la venta actual desde la base de datos
    private int getActualSale() throws SQLException {
        // Consulta SQL para obtener el id de venta mas alto
        String query = "SELECT MAX(id_sales) FROM bill";
        // Prepara la consulta SQL
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        // Ejecuta la consulta y obtiene el resultado
        java.sql.ResultSet resultSet = preparedStatement.executeQuery();
        if(resultSet.next()){ // Si hay un resultado
            // Retorna el id de venta mas alto + 1
            return resultSet.getInt(1)+1;
        }
        return 0;// Si no hay resultados, retorna 0
    }

    // Metodo para crear una venta en la base de datos
    private int createSale(String name, String RUC, double total) throws SQLException {
        // Obtener el id de la venta actual
        int saleID = getActualSale();
        // Consulta SQL para insertar una nueva venta en la tabla bill
        String query = "INSERT INTO bill (id_sales,ruc, name,cost_total) VALUES (?, ?, ?, ?)";
        // Prepara la consulta SQL
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        // Establece los parametros de la consulta SQL
        preparedStatement.setInt(1, saleID);
        preparedStatement.setString(2, RUC);
        preparedStatement.setString(3, name);
        preparedStatement.setDouble(4, total);
        // Ejecuta la consulta para insertar la nueva venta
        preparedStatement.executeUpdate();
        return saleID++; // Retorna el ID de la venta creada
    }

    // Metodo para crear un nuevo producto vendido en la base de datos
    private void createProduct(int sale, Product product) throws SQLException {
        // Consulta SQL para insertar un nuevo producto vendido en la tabla sold_item
        String query = "INSERT INTO sold_item (id_sales,id_prod,name_prod,category,amount,cost,cost_total) VALUES (?, ?, ?, ?, ?, ?, ?)";
        // Prepara la consulta SQL
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        // Establece los parametros de la consulta SQL
        preparedStatement.setInt(1, sale);
        preparedStatement.setInt(2, product.ID);
        preparedStatement.setString(3, product.name);
        preparedStatement.setString(4, product.category);
        preparedStatement.setInt(5, product.amount);
        preparedStatement.setDouble(6, product.cost);
        preparedStatement.setDouble(7, product.total);
        // Ejecuta la consulta para insertar el nuevo producto vendido
        preparedStatement.executeUpdate();
    }

    // Metodo para analizar y procesar los datos recibidos desde el servidor RabbitMQ
    public void parseData(String data) throws SQLException {
        double total = 0;// Inicializa el total de venta en 0
        // Divide los datos recibidos en partes usando ';'
        String[] dataSplit = data.split(";");
        // Obtien el nombre del cliente desde los datos recibidos
        String name = dataSplit[0];
        // Obtiene el RUC del cliente desde los datos recibidos
        String RUC = dataSplit[1];
        // Crea un array para almacenar los productos vendidos
        Product[] products = new Product[dataSplit.length - 2];
        
        // Recorre los datos de los productos a partir del tercer elemento
        for (int i = 2; i < dataSplit.length; i++) {
            //Divide los datos de los productos en partes usando ','
            String[] productData = dataSplit[i].split(",");
            // Crea un nuevo objeto Product
            Product product = new Product();
            // Asiga el ID, nombre, categoria, cantidad, precio y total del producto desde los datos recibidos
            product.ID = Integer.parseInt(productData[0]);
            product.name = productData[1];
            product.category = productData[2];
            product.amount = Integer.parseInt(productData[3]);
            product.cost = Double.parseDouble(productData[4]);
            product.total = Double.parseDouble(productData[5]);
            // Incrementa el total de venta con el costo total del producto
            total += product.total;
            //Agrega el producto al array de productos vendidos
            products[i - 2] = product;
        }
        // Crea una nueva venta en la base de datos
        int actualSale = createSale(name,RUC,total);
        // Recorre los productos vendidos 
        for (Product product : products) {
            // crea un registro de venta en la base de datos para cada producto vendido
            createProduct(actualSale, product);
        }
    }

    // Metodos para iniciar el servidor Java
    public static void main(String[] argv) throws Exception {
        // Imprime mensaje de inicio
        System.out.println("Servidor Java iniciado");
        // Crea una instancia de ServerJava
        ServerJava serverJava = new ServerJava();
        // Establece la conexion a PostgreSQL
        serverJava.connectToPostgres();

        // Comprueba si la conexion a PostgreSQL ha sido establecida
        if (serverJava.postgresConnection != null) {
            System.out.println("Conexión a PostgreSQL establecida");

        } else {
            System.out.println("La conexión a PostgreSQL ha fallado");
            return;// Sale del metodo main si la conexion falla
        }
        // Crea una instancia de Rabbit
        Rabbit rabbit = new Rabbit();
        // Establece el servidor Java en Rabbit
        rabbit.setServerJava(serverJava);
        rabbit.run();
    }

}
