import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.sql.*;

class Client extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private Socket socket;
    private BufferedReader br;
    private PrintWriter out;

    private Connection dbConnection;

    public Client() {
        setTitle("Client Chat");
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
        startClient();
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

    private void startClient() {
        try {
            chatArea.append("Connecting to server...\n");
            socket = new Socket("127.0.0.1", 8888);
            chatArea.append("Connected to server.\n");

            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            startReading();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startReading() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = br.readLine()) != null) {
                    if (msg.equalsIgnoreCase("exit")) {
                        chatArea.append("Server left the chat.\n");
                        socket.close();
                        break;
                    }
                    chatArea.append("Server: " + msg + "\n");
                    storeEncryptedMessage("Server", "Client", msg);
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
            storeEncryptedMessage("Client", "Server", msg);
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
        SwingUtilities.invokeLater(Client::new);
    }
}
