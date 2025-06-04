package server;

import java.sql.*;
import java.util.*;

public class DBAccess {
    private Connection con;

    public DBAccess() {
        try {
            ConnectDB dbConnection = new ConnectDB();
            this.con = dbConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Thêm người dùng mới vào bảng 'users'
    public boolean saveUser(String username,String password, String ipAddress, int port) {
        String sql = "INSERT INTO users (username,password, ip_address, port) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, ipAddress);
            pstmt.setInt(4, port);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu user: " + e.getMessage());
            return false;
        }
    }

    // Lưu tin nhắn vào bảng 'chat_logs' và trả về ID của bản ghi vừa tạo
    public int saveMessage(String encryptedText, String decryptedText, int keyValue, String clientIP, String messageType) {
        String sql = "INSERT INTO chat_logs (encrypted_text, decrypted_text, key_value, client_ip, message_type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, encryptedText);
            pstmt.setString(2, decryptedText);
            pstmt.setInt(3, keyValue);
            pstmt.setString(4, clientIP);
            pstmt.setString(5, messageType);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu tin nhắn: " + e.getMessage());
            return -1;
        }
    }

    // Lưu broadcast message vào bảng 'broadcast_messages' và trả về ID
    public int saveBroadcastMessage(int baseMessageId) {
        String sql = "INSERT INTO broadcast_messages (base_message_id) VALUES (?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, baseMessageId);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu broadcast message: " + e.getMessage());
            return -1;
        }
    }

    // Lưu thông tin người nhận broadcast
    public boolean saveBroadcastReceiver(int broadcastId, int receiverId, Timestamp receivedTime) {
        String sql = "INSERT INTO broadcast_receivers (broadcast_id, receiver_id, received_time) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, broadcastId);
            pstmt.setInt(2, receiverId);
            pstmt.setTimestamp(3, receivedTime);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu broadcast receiver: " + e.getMessage());
            return false;
        }
    }
    
    public int getUserIdByUsername(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1;
        }
    }
    
    // Kiểm tra xem user đã tồn tại chưa
    public boolean userExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
    
    // Kiểm tra login
    public boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Trả về true nếu tồn tại bản ghi
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}