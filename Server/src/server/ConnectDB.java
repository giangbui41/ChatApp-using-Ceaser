/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 *
 * @author LENOVO
 */
public class ConnectDB{

    public Connection getConnection() {
        try {
            // Load JDBC Driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String url = "jdbc:sqlserver://localhost:1433;Database=UDPchat2;user=sa;password=123;encrypt=false;";
            // Kết nối SQL Server
            Connection conn = DriverManager.getConnection(url);
            System.out.println("Connect Success!");
            return conn;
        } catch (ClassNotFoundException e) {
            System.out.println("Lỗi: Không tìm thấy Driver!");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối Database!");
            e.printStackTrace();
            return null;
        }
    }
    
}
