package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Base64;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class ChatClient extends JFrame {
    // Authentication components
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    // Chat components
    private JTextArea chatArea;
    private JTextField messageField;
    private JSpinner keySpinner;
    private JButton sendButton;
    private JButton sendFileButton;
    private JLabel statusLabel;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private String selectedRecipient = null;

    // Network
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private static final int SERVER_PORT = 11567;
    private String username;
    private Timer userListTimer;

    public ChatClient() {
        initAuthUI();
        setTitle("UDP Chat - Đăng nhập/Đăng ký");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initAuthUI() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 165, 0), 2));

        // Left panel (branding/logo)
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(new Color(0, 128, 128));
        leftPanel.setLayout(new BorderLayout());
        JLabel logoLabel = new JLabel("ChatApp", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 24));
        logoLabel.setForeground(Color.WHITE);
        leftPanel.add(logoLabel, BorderLayout.CENTER);
        JLabel copyrightLabel = new JLabel("© COMPANY NAME. ALL RIGHTS RESERVED", SwingConstants.CENTER);
        copyrightLabel.setForeground(Color.WHITE);
        copyrightLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        leftPanel.add(copyrightLabel, BorderLayout.SOUTH);

        // Right panel (form)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tabbed pane for Sign Up and Login
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 16));

        // Sign Up tab
        JPanel signUpPanel = new JPanel(new GridBagLayout());
        signUpPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        signUpPanel.add(usernameLabel, gbc);

        gbc.gridy++;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        signUpPanel.add(usernameField, gbc);

        gbc.gridy++;
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        signUpPanel.add(passwordLabel, gbc);

        gbc.gridy++;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        signUpPanel.add(passwordField, gbc);

        gbc.gridy++;
        registerButton = new JButton("Sign Up");
        registerButton.setBackground(new Color(0, 128, 128));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));
        signUpPanel.add(registerButton, gbc);

        gbc.gridy++;
        JButton loginLink = new JButton("I've an account");
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setForeground(new Color(0, 128, 128));
        loginLink.setFont(new Font("Arial", Font.PLAIN, 12));
        loginLink.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        signUpPanel.add(loginLink, gbc);

        // Login tab
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(Color.WHITE);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel loginUsernameLabel = new JLabel("Username");
        loginUsernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(loginUsernameLabel, gbc);

        gbc.gridy++;
        JTextField loginUsernameField = new JTextField(20);
        loginUsernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(loginUsernameField, gbc);

        gbc.gridy++;
        JLabel loginPasswordLabel = new JLabel("Password");
        loginPasswordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(loginPasswordLabel, gbc);

        gbc.gridy++;
        JPasswordField loginPasswordField = new JPasswordField(20);
        loginPasswordField.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(loginPasswordField, gbc);

        gbc.gridy++;
        loginButton = new JButton("Login");
        loginButton.setBackground(new Color(0, 128, 128));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginPanel.add(loginButton, gbc);

        gbc.gridy++;
        JButton signUpLink = new JButton("I don't have an account");
        signUpLink.setBorderPainted(false);
        signUpLink.setContentAreaFilled(false);
        signUpLink.setForeground(new Color(0, 128, 128));
        signUpLink.setFont(new Font("Arial", Font.PLAIN, 12));
        signUpLink.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        loginPanel.add(signUpLink, gbc);

        tabbedPane.addTab("SIGN UP", signUpPanel);
        tabbedPane.addTab("LOGIN", loginPanel);

        rightPanel.add(tabbedPane, BorderLayout.CENTER);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        add(mainPanel);

        // Action listeners with updated field mapping
        loginButton.addActionListener(e -> {
            usernameField = loginUsernameField;
            passwordField = loginPasswordField;
            handleAuth("LOGIN");
        });
        registerButton.addActionListener(e -> handleAuth("REGISTER"));
        passwordField.addActionListener(e -> handleAuth("LOGIN"));
        loginPasswordField.addActionListener(e -> {
            usernameField = loginUsernameField;
            passwordField = loginPasswordField;
            handleAuth("LOGIN");
        });
    }

    private void initChatUI() {
        getContentPane().removeAll();
        setTitle("UDP Chat Client - " + username);
        setSize(800, 600);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // User list panel (left side)
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBackground(new Color(240, 240, 240));
        userPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search bar
        JLabel onlineLabel = new JLabel(" User online", SwingConstants.LEFT);
        onlineLabel.setFont(new Font("Arial", Font.BOLD, 14));
        onlineLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        userPanel.add(onlineLabel, BorderLayout.NORTH);

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserListCellRenderer());
        userList.setBackground(new Color(240, 240, 240));
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createEmptyBorder());
        userScrollPane.setPreferredSize(new Dimension(200, 0));
        userPanel.add(userScrollPane, BorderLayout.CENTER);

        // Thêm sự kiện chọn người dùng
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedRecipient = userList.getSelectedValue();
                if (selectedRecipient != null) {
                    statusLabel.setText(" Đang trò chuyện với: " + selectedRecipient);
                } else {
                    statusLabel.setText(" Connected as " + username);
                }
            }
        });

        // Chat area (right side)
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(245, 245, 245));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Message input panel
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        messagePanel.add(messageField, BorderLayout.CENTER);

        // Key spinner
        keySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 25, 1));
        keySpinner.setPreferredSize(new Dimension(60, 30));
        messagePanel.add(keySpinner, BorderLayout.WEST);

        // Send and Send File buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(0, 128, 128));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        buttonPanel.add(sendButton);

        sendFileButton = new JButton("Send File");
        sendFileButton.setBackground(new Color(0, 128, 128));
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setFont(new Font("Arial", Font.BOLD, 14));
        buttonPanel.add(sendFileButton);

        messagePanel.add(buttonPanel, BorderLayout.EAST);

        chatPanel.add(messagePanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userPanel, chatPanel);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(1);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Status label
        statusLabel = new JLabel(" Connected as " + username);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);

        // Keep original action listeners
        sendButton.addActionListener(e -> sendMessage());
        sendFileButton.addActionListener(e -> sendFile());
        messageField.addActionListener(e -> sendMessage());

        // Start receiving messages
        new Thread(this::receiveMessages).start();

        // Periodically request user list from server
        startUserListRefresh();

        revalidate();
        repaint();
    }

    private void startUserListRefresh() {
        userListTimer = new Timer();
        userListTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String msg = "GET_USERLIST";
                    byte[] sendData = msg.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
                    socket.send(sendPacket);
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(" Lỗi cập nhật danh sách người dùng");
                    });
                }
            }
        }, 0, 5000);
    }

    private void handleAuth(String type) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không được để trống tên đăng nhập hoặc mật khẩu!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (username.length() > 50) {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập phải dưới 50 ký tự!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length() > 100) {
            JOptionPane.showMessageDialog(this, "Mật khẩu phải dưới 100 ký tự!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");
            String msg = type + "|" + username + "|" + password;
            byte[] sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            socket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.setSoTimeout(5000);
            socket.receive(receivePacket);
            socket.setSoTimeout(0);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

            if (type.equals("REGISTER")) {
                if (response.startsWith("USERLIST|")) {
                    JOptionPane.showMessageDialog(this, "Đăng ký thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    this.username = username;
                    initChatUI();
                } else {
                    JOptionPane.showMessageDialog(this, "Đăng ký thất bại: Tên đăng nhập đã tồn tại!",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else { // LOGIN
                if (response.equals("LOGIN_SUCCESS")) {
                    JOptionPane.showMessageDialog(this, "Đăng nhập thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    this.username = username;
                    initChatUI();
                } else {
                    JOptionPane.showMessageDialog(this, "Đăng nhập thất bại: Sai tên đăng nhập hoặc mật khẩu!",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SocketTimeoutException e) {
            JOptionPane.showMessageDialog(this, "Không nhận được phản hồi từ server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối đến server: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        try {
            String message = messageField.getText().trim();
            if (message.isEmpty()) return;

            int key = (int) keySpinner.getValue();
            String encrypted = CaesarCipher.encrypt(message, key);

            String dataToSend;
            if (selectedRecipient != null) {
                dataToSend = "PRIVATE|" + selectedRecipient + "|" + key + "|" + encrypted;
            } else {
                dataToSend = key + "|" + encrypted;
            }

            byte[] sendData = dataToSend.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            socket.send(sendPacket);

            chatArea.append("You" + (selectedRecipient != null ? " (to " + selectedRecipient + ")" : "") + " (" + key + "): " + message + "\n");
            messageField.setText("");
        } catch (

Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi tin nhắn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            new Thread(() -> {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    if (fileBytes.length > 64000) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(ChatClient.this,
                                    "File phải nhỏ hơn 64KB!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        });
                        return;
                    }
                    String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                    String senderUsername = username;
                    String fileMsg;
                    if (selectedRecipient != null) {
                        fileMsg = "FILE|" + selectedRecipient + "|" + file.getName() + "|" + encodedFile + "|" + senderUsername;
                    } else {
                        fileMsg = "FILE|BROADCAST|" + file.getName() + "|" + encodedFile + "|" + senderUsername;
                    }

                    byte[] sendData = fileMsg.getBytes();
                    if (sendData.length > 60000) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(ChatClient.this,
                                    "File quá lớn để gửi!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        });
                        return;
                    }

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
                    socket.send(sendPacket);

                    SwingUtilities.invokeLater(() -> {
                        chatArea.append("You sent file: " + file.getName() +
                                (selectedRecipient != null ? " to " + selectedRecipient : "") + "\n");
                        statusLabel.setText(" File sent successfully");
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ChatClient.this,
                                "Lỗi gửi file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    });
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void receiveMessages() {
        byte[] receiveData = new byte[65507];

        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

                SwingUtilities.invokeLater(() -> {
                    if (receivedMessage.startsWith("COUNT_RESULT|")) {
                        String result = receivedMessage.substring("COUNT_RESULT|".length());
                        chatArea.append("Character count result:\n" + result + "\n");
                    } else if (receivedMessage.startsWith("USERLIST|")) {
                        updateUserList(receivedMessage.substring("USERLIST|".length()));
                    } else if (receivedMessage.startsWith("FILE|")) {
                        String[] parts = receivedMessage.split("\\|", 5);
                        if (parts.length >= 4) {
                            String sender = parts.length == 5 ? parts[4] : "Unknown";
                            String filename = parts[2];
                            String fileContent = parts[3];
                            chatArea.append("Received file: " + filename + " from " + sender + "\n");

                            int option = JOptionPane.showConfirmDialog(ChatClient.this,
                                    "Bạn có muốn lưu file " + filename + " từ " + sender + " không?",
                                    "Nhận file", JOptionPane.YES_NO_OPTION);

                            if (option == JOptionPane.YES_OPTION) {
                                JFileChooser fileChooser = new JFileChooser();
                                fileChooser.setSelectedFile(new File(filename));
                                if (fileChooser.showSaveDialog(ChatClient.this) == JFileChooser.APPROVE_OPTION) {
                                    try {
                                        byte[] fileBytes = Base64.getDecoder().decode(fileContent);
                                        Files.write(fileChooser.getSelectedFile().toPath(), fileBytes);
                                        chatArea.append("File đã được lưu: " + fileChooser.getSelectedFile() + "\n");
                                    } catch (IOException e) {
                                        JOptionPane.showMessageDialog(ChatClient.this,
                                                "Lỗi khi lưu file: " + e.getMessage(),
                                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                                    } catch (IllegalArgumentException e) {
                                        JOptionPane.showMessageDialog(ChatClient.this,
                                                "Dữ liệu file không hợp lệ!",
                                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            }
                        }
                    } else if (receivedMessage.startsWith("PRIVATE|")) {
                        String[] parts = receivedMessage.split("\\|", 3);
                        if (parts.length == 3) {
                            String sender = parts[1];
                            String message = parts[2];
                            chatArea.append("[From " + sender + "]: " + message + "\n");
                        }
                    } else {
                        chatArea.append(receivedMessage + "\n");
                    }
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(" Lỗi nhận tin nhắn");
                });
                e.printStackTrace();
                break;
            }
        }
    }

    private void updateUserList(String userListStr) {
        userListModel.clear();
        if (userListStr.isEmpty()) {
            return;
        }
        for (String user : userListStr.split(",")) {
            if (!user.isEmpty() && !user.equals(username)) {
                userListModel.addElement(user);
            }
        }
    }

    private static class UserListCellRenderer extends JPanel implements ListCellRenderer<String> {
        private JLabel userLabel;
        private JLabel statusLabel;
        private JLabel avatarLabel;

        public UserListCellRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBackground(new Color(240, 240, 240));
            avatarLabel = new JLabel(new ImageIcon("default-avatar.png"));
            userLabel = new JLabel();
            userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            statusLabel = new JLabel();
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            statusLabel.setForeground(Color.GRAY);
            add(avatarLabel, BorderLayout.WEST);
            add(userLabel, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            userLabel.setText(value);
            statusLabel.setText("Online");
            if (isSelected) {
                setBackground(new Color(220, 220, 220));
            } else {
                setBackground(new Color(240, 240, 240));
            }
            return this;
        }
    }

    static class CaesarCipher {
        public static String encrypt(String text, int shift) {
            StringBuilder result = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (Character.isLetter(c)) {
                    char base = Character.isLowerCase(c) ? 'a' : 'A';
                    c = (char) (((c - base + shift) % 26) + base);
                }
                result.append(c);
            }
            return result.toString();
        }

        public static String decrypt(String text, int shift) {
            return encrypt(text, 26 - (shift % 26));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient().setVisible(true));
    }
}

