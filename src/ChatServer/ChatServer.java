package ChatServer;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<String> usernames = new HashSet<>();
    private static Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Servidor iniciado en el puerto " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String username;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Solicitar y registrar el nombre de usuario
                out.println("Ingrese su nombre de usuario:");
                username = in.readLine();
                synchronized (usernames) {
                    usernames.add(username);
                }
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }
                notifyUsers(); // Enviar lista de usuarios conectados

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("chao")) {
                        out.println("Has salido del chat.");
                        break;
                    } else if (message.startsWith("MSG")) {
                        String targetUser = message.split(" ")[1].replace(":", "");
                        String userMessage = message.substring(message.indexOf(":") + 1).trim();
                        sendMessageToUser(targetUser, username + ": " + userMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error al manejar el cliente: " + e.getMessage());
            } finally {
                if (username != null) {
                    usernames.remove(username);
                    notifyUsers(); // Enviar lista actualizada
                }
                if (out != null) {
                    clientWriters.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("No se pudo cerrar el socket del cliente");
                }
            }
        }

        // Método para notificar a todos los clientes la lista actualizada de usuarios conectados
        private void notifyUsers() {
            String userList = "Usuarios conectados: " + usernames.toString();
            for (PrintWriter writer : clientWriters) {
                writer.println(userList);
            }
        }

        // Método para enviar un mensaje a un usuario específico
        private void sendMessageToUser(String targetUser, String message) {
            for (PrintWriter writer : clientWriters) {
                writer.println("MSG " + targetUser + ": " + message);
            }
        }
    }
}
