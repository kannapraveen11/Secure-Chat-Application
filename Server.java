import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.sql.*;

class Server extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private ServerSocket server;
    private Socket socket;
    private BufferedReader br;
    private PrintWriter out;

    private Connection dbConnection;

    public Server() {
        setTitle("Server Chat");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);

        connectDatabase();
        startServer();
    }

    private void connectDatabase() {
        try {
            dbConnection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/chat_db", "root", "Devansh#29"
            );
            System.out.println("✅ Connected to MySQL");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void startServer() {
        try {
            server = new ServerSocket(8888);
            chatArea.append("Server is ready to accept connection...\n");
            socket = server.accept();

            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            startReading();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startReading() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = br.readLine()) != null) {
                    if (msg.equalsIgnoreCase("exit")) {
                        chatArea.append("Client left the chat.\n");
                        socket.close();
                        break;
                    }
                    chatArea.append("Client: " + msg + "\n");
                    storeEncryptedMessage("Client", "Server", msg);
                }
            } catch (IOException e) {
                chatArea.append("Connection closed.\n");
            }
        }).start();
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            chatArea.append("You: " + msg + "\n");
            out.println(msg);
            storeEncryptedMessage("Server", "Client", msg);
            inputField.setText("");
        }
    }

    // One-Time Pad Encryption
    private byte[] xor(byte[] plaintext, byte[] key) {
        byte[] result = new byte[plaintext.length];
        for (int i = 0; i < plaintext.length; i++) {
            result[i] = (byte) (plaintext[i] ^ key[i]);
        }
        return result;
    }

    // Store Encrypted Message in DB
    private void storeEncryptedMessage(String sender, String receiver, String message) {
        try {
            byte[] plaintext = message.getBytes();
            byte[] key = new byte[plaintext.length];
            new SecureRandom().nextBytes(key);

            byte[] ciphertext = xor(plaintext, key);

            String sql = "INSERT INTO messages (sender, receiver, ciphertext, key_data) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = dbConnection.prepareStatement(sql);
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setBytes(3, ciphertext);
            ps.setBytes(4, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            chatArea.append("Error storing message in DB\n");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }
}
