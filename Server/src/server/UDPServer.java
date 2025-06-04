package server;

import java.net.*;
import java.sql.*;
import java.util.*;

public class UDPServer {
    private static final int PORT = 11567;
    private static final List<ClientInfo> clients = new ArrayList<>();
    private static DBAccess dbAccess = new DBAccess();

    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(PORT);
        byte[] receiveData = new byte[65507];
        System.out.println("Server is running on port " + PORT);

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            InetAddress ip = receivePacket.getAddress();
            int port = receivePacket.getPort();
            String receivedText = new String(receivePacket.getData(), 0, receivePacket.getLength(),"UTF-8");
            System.out.println("Received: " + receivedText);

            if (receivedText.startsWith("REGISTER|")) {
                String[] parts = receivedText.split("\\|", 3);
                if (parts.length == 3) {
                    String username = parts[1];
                    String password = parts[2];

                    ClientInfo client = new ClientInfo(username, password, ip, port);
                    if (!clientExists(client)) {
                        clients.add(client);
                        saveUserToDatabase(username, password, ip, port);
                    }

                    broadcastUserList(serverSocket);
                }
            } else if (receivedText.startsWith("LOGIN|")) {
                String[] parts = receivedText.split("\\|", 3);
                if (parts.length == 3) {
                    String username = parts[1];
                    String password = parts[2];

                    if (dbAccess.checkLogin(username, password)) {
                        ClientInfo client = new ClientInfo(username, password, ip, port);
                        if (!clientExists(client)) {
                            clients.add(client);
                        }

                        String successMsg = "LOGIN_SUCCESS";
                        byte[] sendData = successMsg.getBytes("UTF-8");
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
                        serverSocket.send(sendPacket);

                        broadcastUserList(serverSocket);
                    } else {
                        String failMsg = "LOGIN_FAIL";
                        byte[] sendData = failMsg.getBytes("UTF-8");
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
                        serverSocket.send(sendPacket);
                    }
                }
            } else if (receivedText.startsWith("PRIVATE|")) {
                String[] parts = receivedText.split("\\|", 4);
                if (parts.length == 4) {
                    String recipient = parts[1];
                    int key = Integer.parseInt(parts[2]);
                    String encrypted = parts[3];

                    String decrypted = decryptCaesar(encrypted, key);

                    Map<Character, Integer> freq = countLetters(decrypted);
                    String stat = generateStats(freq);

                    byte[] statBytes = ("COUNT_RESULT|" + stat).getBytes();
                    DatagramPacket statPacket = new DatagramPacket(statBytes, statBytes.length, ip, port);
                    serverSocket.send(statPacket);

                    String senderUsername = getUsernameByAddress(ip, port);
                    int messageId = dbAccess.saveMessage(encrypted, decrypted, key, ip.getHostAddress(), "PRIVATE");

                    for (ClientInfo client : clients) {
                        if (client.username.equals(recipient)) {
                            String privateMessage = "PRIVATE|" + senderUsername + "|" + decrypted;
                            byte[] msgBytes = privateMessage.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(msgBytes, msgBytes.length, client.ip, client.port);
                            serverSocket.send(sendPacket);
                            break;
                        }
                    }
                }
            } else if (receivedText.startsWith("FILE|")) {
                String[] parts = receivedText.split("\\|", 5);
                if (parts.length == 5) {
                    String recipient = parts[1];
                    String filename = parts[2];
                    String fileContent = parts[3];
                    String senderUsername = parts[4];

                    if (!recipient.equals("BROADCAST")) {
                        for (ClientInfo client : clients) {
                            if (client.username.equals(recipient)) {
                                String fileMsg = "FILE|" + recipient + "|" + filename + "|" + fileContent + "|" + senderUsername;
                                byte[] msgBytes = fileMsg.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(msgBytes, msgBytes.length, client.ip, client.port);
                                serverSocket.send(sendPacket);
                                break;
                            }
                        }
                    } else {
                        for (ClientInfo client : clients) {
                            if (!client.username.equals(senderUsername)) {
                                String fileMsg = "FILE|" + client.username + "|" + filename + "|" + fileContent + "|" + senderUsername;
                                byte[] msgBytes = fileMsg.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(msgBytes, msgBytes.length, client.ip, client.port);
                                serverSocket.send(sendPacket);
                            }
                        }
                    }
                }
            } else if (receivedText.equals("GET_USERLIST")) {
                broadcastUserList(serverSocket);
            } else {
                String[] parts = receivedText.split("\\|", 2);
                if (parts.length == 2) {
                    int key = Integer.parseInt(parts[0]);
                    String encrypted = parts[1];

                    String decrypted = decryptCaesar(encrypted, key);

                    Map<Character, Integer> freq = countLetters(decrypted);
                    String stat = generateStats(freq);

                    byte[] statBytes = ("COUNT_RESULT|" + stat).getBytes();
                    DatagramPacket statPacket = new DatagramPacket(statBytes, statBytes.length, ip, port);
                    serverSocket.send(statPacket);

                    String senderUsername = getUsernameByAddress(ip, port);
                    int messageId = dbAccess.saveMessage(encrypted, decrypted, key, ip.getHostAddress(), "DIRECT");

                    String broadcast = "[" + senderUsername + "]: " + decrypted;
                    broadcastMessage(serverSocket, ip, port, broadcast, messageId);
                }
            }
        }
    }

    private static boolean clientExists(ClientInfo newClient) {
        for (ClientInfo client : clients) {
            if (client.username.equals(newClient.username) &&
                    client.ip.equals(newClient.ip) &&
                    client.port == newClient.port) {
                return true;
            }
        }
        return false;
    }

    private static void broadcastMessage(DatagramSocket socket, InetAddress senderIp, int senderPort,
                                        String message, int baseMessageId) {
        int broadcastId = dbAccess.saveBroadcastMessage(baseMessageId);

        if (broadcastId > 0) {
            for (ClientInfo client : clients) {
                if (!(client.ip.equals(senderIp) && client.port == senderPort)) {
                    try {
                        byte[] msgBytes = message.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(msgBytes, msgBytes.length,
                                client.ip, client.port);
                        socket.send(sendPacket);

                        saveBroadcastReceiver(client.username, broadcastId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void broadcastUserList(DatagramSocket socket) {
        StringBuilder userList = new StringBuilder("USERLIST|");
        for (ClientInfo client : clients) {
            userList.append(client.username).append(",");
        }

        String listStr = userList.toString();
        byte[] listBytes = listStr.getBytes();

        for (ClientInfo client : clients) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(listBytes, listBytes.length,
                        client.ip, client.port);
                socket.send(sendPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getUsernameByAddress(InetAddress ip, int port) {
        for (ClientInfo client : clients) {
            if (client.ip.equals(ip) && client.port == port) {
                return client.username;
            }
        }
        return "Unknown";
    }

    private static String decryptCaesar(String text, int key) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isLowerCase(c) ? 'a' : 'A';
                sb.append((char) ((c - base - key + 26) % 26 + base));
            } else sb.append(c);
        }
        return sb.toString();
    }

    private static Map<Character, Integer> countLetters(String text) {
        Map<Character, Integer> map = new TreeMap<>();
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetter(c)) {
                map.put(c, map.getOrDefault(c, 0) + 1);
            }
        }
        return map;
    }

    private static String generateStats(Map<Character, Integer> freq) {
        StringBuilder sb = new StringBuilder("Character Frequency:\n");
        for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private static void saveUserToDatabase(String username, String password, InetAddress ip, int port) {
        try {
            if (!dbAccess.userExists(username)) {
                dbAccess.saveUser(username, password, ip.getHostAddress(), port);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void saveBroadcastReceiver(String receiverUsername, int broadcastId) {
        try {
            int receiverId = dbAccess.getUserIdByUsername(receiverUsername);
            if (receiverId > 0) {
                dbAccess.saveBroadcastReceiver(broadcastId, receiverId, new Timestamp(System.currentTimeMillis()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class ClientInfo {
        String username;
        String password;
        InetAddress ip;
        int port;

        public ClientInfo(String username, String password, InetAddress ip, int port) {
            this.username = username;
            this.password = password;
            this.ip = ip;
            this.port = port;
        }
    }
}