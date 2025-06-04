package server;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.PrintWriter;


public class ServerUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable usersTable, chatLogsTable, broadcastMessagesTable, broadcastReceiversTable;
    private Connection con;
    private JLabel statusLabel;
    private JTextArea logArea;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private Color primaryColor = new Color(0, 102, 204);
    private Color secondaryColor = new Color(240, 240, 240);
    private Color headerColor = new Color(70, 130, 180);

    public ServerUI() {
        setTitle("UDP Server Management");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Thiết lập giao diện chính
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(secondaryColor);

        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(secondaryColor);

        try {
            con = new ConnectDB().getConnection();
            statusLabel.setText("Status: Connected to database");
            statusLabel.setForeground(new Color(0, 150, 0));

            usersTable = createStyledTable("SELECT * FROM users");
            chatLogsTable = createStyledTable("SELECT * FROM chat_logs");
            broadcastMessagesTable = createStyledTable("SELECT * FROM broadcast_messages");
            broadcastReceiversTable = createStyledTable("SELECT * FROM broadcast_receivers");

            tabbedPane.addTab("👥 Users", createTablePanel(usersTable));
            tabbedPane.addTab("💬 Chat Logs", createTablePanel(chatLogsTable));
            tabbedPane.addTab("📢 Broadcasts", createTablePanel(broadcastMessagesTable));
            tabbedPane.addTab("📩 Receivers", createTablePanel(broadcastReceiversTable));

        } catch (Exception e) {
            statusLabel.setText("Status: Database connection failed");
            statusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "Database connection failed!\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Tạo log area với style đẹp hơn
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(250, 250, 250));
        logArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Server Log"),
            new EmptyBorder(5, 5, 5, 5)
        ));
        logScroll.setPreferredSize(new Dimension(0, 150));
        mainPanel.add(logScroll, BorderLayout.SOUTH);

        add(mainPanel);
        log("Server UI initialized");
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.setBackground(secondaryColor);

        JLabel titleLabel = new JLabel("UDP Server Management Console");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(primaryColor);
        panel.add(titleLabel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statusPanel.setBackground(secondaryColor);
        
        statusLabel = new JLabel("Status: Initializing...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusPanel.add(statusLabel);

        //nút refresh data
        JButton refreshBtn = new JButton("Refresh Data");
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setBorder(new CompoundBorder(
            new LineBorder(primaryColor, 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refreshTables());
        statusPanel.add(refreshBtn);

        //clear all data
        JButton clearDataBtn = new JButton("Clear All Data");
        clearDataBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearDataBtn.setBackground(Color.WHITE);
        clearDataBtn.setBorder(new CompoundBorder(
            new LineBorder(primaryColor, 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        clearDataBtn.setFocusPainted(false);
        clearDataBtn.addActionListener(e -> clearAllData());
        statusPanel.add(clearDataBtn);


        panel.add(statusPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createTablePanel(JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 0, 0, 0));
        panel.setBackground(secondaryColor);

        // Thiết lập style cho bảng
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 220, 220));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        // Thiết lập style cho header của bảng
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(headerColor); // Màu nền header
        header.setForeground(Color.BLACK); // Màu chữ header (trắng)
        header.setOpaque(true); // Đảm bảo header hiển thị đúng
        header.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); 

        // Đảm bảo chiều cao header đủ lớn
        header.setPreferredSize(new Dimension(header.getWidth(), 35));
    
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel cho nút Export
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(secondaryColor);
        
        JButton exportBtn = new JButton("Export to CSV");
        exportBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        exportBtn.setBackground(Color.WHITE);
        exportBtn.setBorder(new CompoundBorder(
            new LineBorder(primaryColor, 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        exportBtn.setFocusPainted(false);
        exportBtn.addActionListener(e -> exportToCSV(table));
        buttonPanel.add(exportBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JTable createStyledTable(String query) throws SQLException {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                model.addColumn(meta.getColumnName(i));
            }

            while (rs.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    rowData[i] = rs.getObject(i + 1);
                }
                model.addRow(rowData);
            }
        }

        JTable table = new JTable(model);
        
        // Custom renderer với màu sắc và căn chỉnh
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, 
                        isSelected, hasFocus, row, column);
                
                c.setForeground(Color.BLACK);
                setHorizontalAlignment(JLabel.CENTER);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                
                return c;
            }
        };
        
        // Áp dụng renderer cho tất cả các cột
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        return table;
    }

    private void refreshTables() {
        try {
            ((DefaultTableModel) usersTable.getModel()).setRowCount(0);
            ((DefaultTableModel) chatLogsTable.getModel()).setRowCount(0);
            ((DefaultTableModel) broadcastMessagesTable.getModel()).setRowCount(0);
            ((DefaultTableModel) broadcastReceiversTable.getModel()).setRowCount(0);

            fillTable(usersTable, "SELECT * FROM users");
            fillTable(chatLogsTable, "SELECT * FROM chat_logs");
            fillTable(broadcastMessagesTable, "SELECT * FROM broadcast_messages");
            fillTable(broadcastReceiversTable, "SELECT * FROM broadcast_receivers");

            log("Data refreshed successfully");
            statusLabel.setText("Status: Data refreshed successfully");
            statusLabel.setForeground(new Color(0, 150, 0));
        } catch (SQLException e) {
            log("Error refreshing data: " + e.getMessage());
            statusLabel.setText("Status: Error refreshing data");
            statusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "Error refreshing data!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void clearAllData() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to clear all data?", 
            "Confirm Clear All Data", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Tạo câu lệnh Statement
                Statement stmt = con.createStatement();

                // Xóa dữ liệu từ bảng broadcast_receivers trước
                stmt.executeUpdate("DELETE FROM broadcast_receivers");
                // Sau đó xóa dữ liệu từ bảng broadcast_messages
                stmt.executeUpdate("DELETE FROM broadcast_messages");
                // Tiếp theo là xóa dữ liệu từ bảng chat_logs
                stmt.executeUpdate("DELETE FROM chat_logs");
                // Cuối cùng xóa dữ liệu từ bảng users
                stmt.executeUpdate("DELETE FROM users");

                // Cập nhật lại giao diện và bảng sau khi xóa
                refreshTables();

                log("All data cleared successfully");
                statusLabel.setText("Status: All data cleared");
                statusLabel.setForeground(new Color(0, 150, 0));  // Màu xanh khi thành công
            } catch (SQLException e) {
                log("Error clearing data: " + e.getMessage());
                statusLabel.setText("Status: Error clearing data");
                statusLabel.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, "Error clearing data!\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void fillTable(JTable table, String query) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    rowData[i] = rs.getObject(i + 1);
                }
                model.addRow(rowData);
            }
        }
    }

    private void exportToCSV(JTable table) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export to CSV");
        fileChooser.setSelectedFile(new java.io.File("export.csv")); // Gợi ý tên file

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();

            try (PrintWriter writer = new PrintWriter(fileToSave, "UTF-8")) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();

                // Ghi tiêu đề cột
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.print(model.getColumnName(i));
                    if (i < model.getColumnCount() - 1) writer.print(",");
                }
                writer.println();

                // Ghi dữ liệu từng dòng
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object cell = model.getValueAt(row, col);
                        String text = (cell == null) ? "" : cell.toString().replaceAll("\"", "\"\"");
                        writer.print("\"" + text + "\"");
                        if (col < model.getColumnCount() - 1) writer.print(",");
                    }
                    writer.println();
                }

                log("Exported to CSV: " + fileToSave.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Exported to CSV:\n" + fileToSave.getAbsolutePath(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log("Error exporting to CSV: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Failed to export CSV:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void log(String message) {
        String timestamp = dateFormat.format(new Date());
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set look and feel to system default
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // Tạo và hiển thị UI
                new ServerUI().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}