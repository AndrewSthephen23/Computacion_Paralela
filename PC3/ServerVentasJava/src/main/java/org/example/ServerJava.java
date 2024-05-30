package org.example;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

public class ServerJava {
    Connection postgresConnection;
    public void connectToPostgres(){
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/salesdb";
            String username = "postgres";
            String password = "root";

            postgresConnection = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private int getActualSale() throws SQLException {
        String query = "SELECT MAX(id_sales) FROM bill";
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        java.sql.ResultSet resultSet = preparedStatement.executeQuery();
        if(resultSet.next()){
            return resultSet.getInt(1)+1;
        }
        return 0;
    }

    private int createSale(String name, String RUC, double total) throws SQLException {
        int saleID = getActualSale();
        String query = "INSERT INTO bill (id_sales,ruc, name,cost_total) VALUES (?, ?, ?, ?)";
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        preparedStatement.setInt(1, saleID);
        preparedStatement.setString(2, RUC);
        preparedStatement.setString(3, name);
        preparedStatement.setDouble(4, total);
        preparedStatement.executeUpdate();
        return saleID++;
    }

    private void createProduct(int sale, Product product) throws SQLException {
        String query = "INSERT INTO sold_item (id_sales,id_prod,name_prod,category,amount,cost,cost_total) VALUES (?, ?, ?, ?, ?, ?, ?)";
        java.sql.PreparedStatement preparedStatement = postgresConnection.prepareStatement(query);
        preparedStatement.setInt(1, sale);
        preparedStatement.setInt(2, product.ID);
        preparedStatement.setString(3, product.name);
        preparedStatement.setString(4, product.category);
        preparedStatement.setInt(5, product.amount);
        preparedStatement.setDouble(6, product.cost);
        preparedStatement.setDouble(7, product.total);
        preparedStatement.executeUpdate();
    }

    public void parseData(String data) throws SQLException {
        double total = 0;
        String[] dataSplit = data.split(";");
        String name = dataSplit[0];
        String RUC = dataSplit[1];
        Product[] products = new Product[dataSplit.length - 2];
        for (int i = 2; i < dataSplit.length; i++) {
            String[] productData = dataSplit[i].split(",");
            Product product = new Product();
            product.ID = Integer.parseInt(productData[0]);
            product.name = productData[1];
            product.category = productData[2];
            product.amount = Integer.parseInt(productData[3]);
            product.cost = Double.parseDouble(productData[4]);
            product.total = Double.parseDouble(productData[5]);
            total += product.total;
            products[i - 2] = product;
        }
        int actualSale = createSale(name,RUC,total);
        for (Product product : products) {
            createProduct(actualSale, product);
        }
    }

    public static void main(String[] argv) throws Exception {
        System.out.println("Servidor Java iniciado");
        ServerJava serverJava = new ServerJava();
        serverJava.connectToPostgres();

        if (serverJava.postgresConnection != null) {
            System.out.println("Conexión a PostgreSQL establecida");

        } else {
            System.out.println("La conexión a PostgreSQL ha fallado");
            return;
        }

        Rabbit rabbit = new Rabbit();
        rabbit.setServerJava(serverJava);
        rabbit.run();
    }

}
