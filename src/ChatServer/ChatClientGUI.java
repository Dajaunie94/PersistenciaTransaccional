package ChatServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent; // Esta línea puede ser eliminada si no se usa directamente
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.HashSet;


public class ChatClientGUI extends JFrame {
    private JTextField messageField, serverIPField, portField, usernameField;
    private JTextArea chatArea;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String username;
    private String chatPartner; // Nombre del usuario con el que estás conversando

    public ChatClientGUI() {
        // Configuración de la interfaz gráfica
        setTitle("Cliente de Chat");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Panel de conexión
        JPanel connectionPanel = new JPanel();
        serverIPField = new JTextField("localhost", 10);
        portField = new JTextField("12345", 5);
        usernameField = new JTextField(10);
        JButton connectButton = new JButton("Conectar");

        connectionPanel.add(new JLabel("IP Servidor:"));
        connectionPanel.add(serverIPField);
        connectionPanel.add(new JLabel("Puerto:"));
        connectionPanel.add(portField);
        connectionPanel.add(new JLabel("Usuario:"));
        connectionPanel.add(usernameField);
        connectionPanel.add(connectButton);

        // Panel de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        // Panel de usuarios conectados
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScrollPane = new JScrollPane(userList);

        // Panel de entrada de mensajes
        messageField = new JTextField(30);
        JButton sendButton = new JButton("Enviar");

        JPanel messagePanel = new JPanel();
        messagePanel.add(messageField);
        messagePanel.add(sendButton);

        // Layout principal
        setLayout(new BorderLayout());
        add(connectionPanel, BorderLayout.NORTH);
        add(chatScrollPane, BorderLayout.CENTER);
        add(userScrollPane, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        // Botón de conexión
        connectButton.addActionListener(e -> connectToServer());

        // Botón de enviar mensaje
        sendButton.addActionListener(e -> sendMessage());

        // Acción al presionar Enter en el campo de mensaje
        messageField.addActionListener(e -> sendMessage());

     // Listener para la selección de usuarios en la lista
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Asegura que la selección es definitiva
                chatPartner = userList.getSelectedValue(); // Asigna el usuario seleccionado
                chatArea.append("Has iniciado un chat con: " + chatPartner + "\n");
            }
        });
    }

    private void connectToServer() {
        String serverAddress = serverIPField.getText();
        int serverPort = Integer.parseInt(portField.getText());
        username = usernameField.getText();

        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Enviar nombre de usuario al servidor
            out.println(username);

            // Hilo para recibir mensajes del servidor
            new Thread(new IncomingMessageHandler()).start();

            JOptionPane.showMessageDialog(this, "Conectado como " + username);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

 // Método para enviar un mensaje
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && chatPartner != null) { // Verifica que haya un usuario seleccionado
            if (message.equalsIgnoreCase("chao")) {
                out.println("chao " + chatPartner); // Enviar mensaje de despedida al servidor con el nombre del usuario
                chatArea.append("Has abandonado el chat con " + chatPartner + "\n");
                messageField.setEditable(false);
            } else {
                out.println("MSG " + chatPartner + ": " + message); // Enviar mensaje específico al usuario seleccionado
                chatArea.append("Tú: " + message + "\n"); // Mostrar el mensaje en el área de chat derecha
            }
            messageField.setText("");
        } else if (chatPartner == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un usuario para chatear.");
        }
    }
 // Método para actualizar la lista de usuarios conectados
    private void updateUsersList(HashSet<String> users) {
        userListModel.clear();
        for (String user : users) {
            if (!user.equals(username)) { // No mostrar el propio usuario en la lista
                userListModel.addElement(user);
            }
        }
    }

    private class IncomingMessageHandler implements Runnable {
        public void run() {
            try {
                String incomingMessage;
                while ((incomingMessage = in.readLine()) != null) {
                    if (incomingMessage.startsWith("Usuarios conectados:")) {
                        String[] users = incomingMessage.substring(19).replace("[", "").replace("]", "").split(", ");
                        HashSet<String> userSet = new HashSet<>();
                        for (String user : users) {
                            userSet.add(user.trim());
                        }
                        updateUsersList(userSet);
                    } else {
                        chatArea.append(incomingMessage + "\n");
                    }
                }
            } catch (IOException e) {
                chatArea.append("Conexión perdida.\n");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI clientGUI = new ChatClientGUI();
            clientGUI.setVisible(true);
        });
    }
}
