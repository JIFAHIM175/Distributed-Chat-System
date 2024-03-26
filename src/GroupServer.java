import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class GroupServer {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private String coordinatorID;

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT + ". Waiting for clients to connect...");

        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            } catch (IOException e) {
                System.err.println("Server exception: " + e.getMessage());
            }
        }
    }

    private void updateCoordinator() {
        if (clients.isEmpty()) {
            coordinatorID = null;
        } else if (coordinatorID == null) {
            coordinatorID = clients.keys().nextElement();
            broadcastMessage("New coordinator is " + coordinatorID);
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients.values()) {
            client.out.println(message);
        }
    }

    private void sendPrivateMessage(String senderId, String recipientId, String message) {
        ClientHandler recipient = clients.get(recipientId);
        if (recipient != null) {
            recipient.out.println("[Private from " + senderId + "]: " + message);
        } else {
            System.out.println("Recipient " + recipientId + " not found.");
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                out.println("Enter your unique ID:");
                clientId = in.readLine();
                out.println("Welcome, " + clientId + "! If you are the first client, you will be the coordinator.");

                synchronized (clients) {
                    if (clients.isEmpty()) {
                        coordinatorID = clientId;
                        out.println("You are the coordinator.");
                    } else {
                        out.println("Coordinator is: " + coordinatorID);
                    }
                    clients.put(clientId, this);
                    updateCoordinator();
                    broadcastMessage(clientId + " has joined the chat.");
                }

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("@")) {
                        String[] parts = inputLine.split(" ", 2);
                        String recipientId = parts[0].substring(1);
                        String privateMessage = parts.length > 1 ? parts[1] : "";
                        sendPrivateMessage(clientId, recipientId, privateMessage);
                    } else if (inputLine.startsWith("/")) {
                        handleCommand(inputLine);
                    } else {
                        broadcastMessage(clientId + ": " + inputLine);
                    }
                }

            } catch (IOException e) {
                System.err.println("Exception in client handler for " + clientId + ": " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void handleCommand(String command) {
            switch (command) {
                case "/list":
                    sendUserList();
                    break;
                case "/quit":
                    disconnectClient();
                    break;
                default:
                    out.println("Unknown command: " + command);
                    break;
            }
        }

        private void sendUserList() {
            StringBuilder userList = new StringBuilder("Connected Users:\n");
            clients.keySet().forEach(clientId -> userList.append("- ").append(clientId).append("\n"));
            out.println(userList.toString());
        }

        private void disconnectClient() {
            clients.remove(clientId);
            broadcastMessage(clientId + " has left the chat.");
            //close
            closeConnection();
            if (clientId.equals(coordinatorID) && !clients.isEmpty()) {
                updateCoordinator(); // Update the coordinator since the current one has left
            }
        }

        private void closeConnection() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println(clientId + " connection closed.");
            } catch (IOException e) {
                System.err.println("Error closing connection for " + clientId + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            new GroupServer().start();
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }
}

