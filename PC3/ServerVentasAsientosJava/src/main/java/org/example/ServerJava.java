package org.example;

import java.sql.DriverManager; // Importación para manejar la conexión JDBC
import java.sql.SQLException; // Importación para manejar las excepciones de SQL
import java.sql.Connection; // Importación para representar una conexión a la base de datos

// Declaramos la clase ServerJava
public class ServerJava {
    // Variable para almacenar la conexión a la base de datos PostgreSQL
    private Connection postgresConnection;

    // Método para establecer una conexión a la base de datos PostgreSQL
    public void connectToPostgres() throws SQLException{

        // Establece la URL, el nombre de usuario y la contraseña
        // Establece la conexión a la base de datos PostgreSQL
        postgresConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/salesbd","postgres","admin");

    }

    // Método para obtener el id de la venta actual desde la base de datos
    private int getActualSale() throws SQLException {
        // Consulta SQL para obtener el id de venta más alto
        String query = "SELECT MAX(id_sales) FROM ventas";
        // Prepara la consulta SQL
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        // Ejecuta la consulta y obtiene el resultado
        java.sql.ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) { // Si hay un resultado
            // Retorna el id de venta más alto + 1
            return resultSet.getInt(1) + 1;
        }
        return 1; // Si no hay resultados, retorna 1
    }

    // Método para crear una venta en la base de datos
    private int createSale(Sales sale) throws SQLException {
        // Obtener el id de la venta actual
        int saleID = getActualSale();
        // Consulta SQL para insertar una nueva venta en la tabla ventas
        String query = "INSERT INTO ventas (id_sales, ruc, name, cost_total) VALUES (?, ?, ?, ?)";
        // Prepara la consulta SQL
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        // Establece los parámetros de la consulta SQL
        preparedStatement.setInt(1, saleID);
        preparedStatement.setString(2, sale.ruc);
        preparedStatement.setString(3, sale.name);
        preparedStatement.setDouble(4, sale.cost_total);
        // Ejecuta la consulta para insertar la nueva venta
        preparedStatement.executeUpdate();
        return saleID; // Retorna el ID de la venta creada
    }

    // Método para crear un nuevo detalle de venta en la base de datos
    private void createSalesDetail(SalesDetail detail) throws SQLException {
        // Consulta SQL para insertar un nuevo detalle de venta en la tabla detalles_ventas
        String query = "INSERT INTO detalles_ventas (id_sales, id_ruta, nombre, name_prod, descripcion_ruta, lugar_compra, asiento, cost, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Prepara la consulta SQL
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        // Establece los parámetros de la consulta SQL
        preparedStatement.setInt(1, detail.id_sales);
        preparedStatement.setInt(2, detail.id_ruta);
        preparedStatement.setString(3, detail.nombre);
        preparedStatement.setString(4, detail.name_prod);
        preparedStatement.setString(5, detail.descripcion_ruta);
        preparedStatement.setString(6, detail.lugar_compra);
        preparedStatement.setInt(7, detail.asiento);
        preparedStatement.setDouble(8, detail.cost);
        preparedStatement.setDouble(9, detail.total);
        // Ejecuta la consulta para insertar el nuevo detalle de venta
        preparedStatement.executeUpdate();
    }

    // Método para analizar y procesar los datos recibidos desde el servidor RabbitMQ
    public void parseData(String data) throws SQLException {
        // Ejemplo de datos recibidos: "1,10897645321,Fulano,70;23,103,Juan,pasaje,lima-chancay,lima,25,70,70"
        String[] parts = data.split(";");
        String[] salesData = parts[0].split(",");
        String[] salesDetailData = parts[1].split(",");

        Sales sale = new Sales();
        sale.id_sales = Integer.parseInt(salesData[0]);
        sale.ruc = salesData[1];
        sale.name = salesData[2];
        sale.cost_total = Double.parseDouble(salesData[3]);

        SalesDetail detail = new SalesDetail();
        detail.id_sales = Integer.parseInt(salesDetailData[0]);
        detail.id_ruta = Integer.parseInt(salesDetailData[1]);
        detail.nombre = salesDetailData[2];
        detail.name_prod = salesDetailData[3];
        detail.descripcion_ruta = salesDetailData[4];
        detail.lugar_compra = salesDetailData[5];
        detail.asiento = Integer.parseInt(salesDetailData[6]);
        detail.cost = Double.parseDouble(salesDetailData[7]);
        detail.total = Double.parseDouble(salesDetailData[8]);

        int actualSale = createSale(sale);
        createSalesDetail(detail);
    }

    // Métodos para iniciar el servidor Java
    public static void main(String[] argv) throws Exception {
        // Imprime mensaje de inicio
        System.out.println("Servidor Java iniciado");
        // Crea una instancia de ServerJava
        ServerJava serverJava = new ServerJava();
        // Establece la conexión a PostgreSQL
        serverJava.connectToPostgres();

        // Comprueba si la conexión a PostgreSQL ha sido establecida
        if (serverJava.postgresConnection != null) {
            System.out.println("Conexión a PostgreSQL establecida");
        } else {
            System.out.println("La conexión a PostgreSQL ha fallado");
            return; // Sale del método main si la conexión falla
        }

        // Crea una instancia de Rabbit
        Rabbit rabbit = new Rabbit();
        // Establece el servidor Java en Rabbit
        rabbit.setServerJava(serverJava);
        rabbit.run();
    }
}
